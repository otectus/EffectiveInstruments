package com.crims.effectiveinstruments.client.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.client.widget.AuraSelectorWidget;
import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.network.EIPacketHandler;
import com.crims.effectiveinstruments.network.packet.InstrumentOpenC2SPacket;
import com.crims.effectiveinstruments.network.packet.SelectAuraC2SPacket;
import com.cstav.genshinstrument.client.gui.screen.instrument.partial.InstrumentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Injects the {@link AuraSelectorWidget} into instrument-screen open events.
 * Supports two categories of screen:
 * <ul>
 *   <li><b>Stationary instruments</b> — Genshin Instruments (and compat
 *       mods like Even More Instruments) via {@link InstrumentScreen}.
 *       Selections route through {@link InstrumentAuraMapping} and
 *       {@link com.crims.effectiveinstruments.aura.AuraManager}.</li>
 *   <li><b>Mobile instruments</b> — Immersive Melodies' two screens, detected
 *       by class-name string match so we don't hard-import an optional mod.
 *       Selections route through {@link MobileInstrumentAuraMapping} and a
 *       per-player selection store. The instrument ID is pulled from the
 *       player's held item; IM screens don't expose it on their own.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        modid = EffectiveInstrumentsMod.MODID,
        value = Dist.CLIENT
)
public class AuraOverlayInjector {

    /** Fully-qualified names of Immersive Melodies' screen classes (optional mod). */
    private static final Set<String> IM_SCREEN_CLASSES = Set.of(
            "immersive_melodies.client.gui.ImmersiveMelodiesScreen",
            "immersive_melodies.client.gui.ImmersiveMelodiesFreePlayingScreen"
    );

    @Nullable
    private static AuraSelectorWidget selectorWidget = null;
    @Nullable
    private static String currentSelectedAuraId = null;
    @Nullable
    private static ResourceLocation currentInstrumentId = null;
    /** True when the active overlay belongs to an IM screen — decides packet routing. */
    private static boolean currentIsMobileTier = false;

    // Per-instrument aura overrides remembered within the session
    private static final Map<ResourceLocation, String> instrumentAuraOverrides = new HashMap<>();

    // --- IM render-based overlay state (bypasses widget-list dependency) ---
    // Immersive Melodies screens aggressively rebuild their widget lists on
    // tab/scroll/refresh. Rather than fight that, we draw our icons directly
    // on ScreenEvent.Render.Post and hit-test clicks in MouseButtonPressed.Pre.
    // Populated by onScreenInit when an IM screen opens.

    @Nullable
    private static Screen imOverlayScreen = null;
    private static final List<AuraPreset> imOverlayAllowed = new ArrayList<>();
    private static final List<int[]> imOverlayRects = new ArrayList<>(); // [x,y,w,h] per preset

    // 16-px icon size = native 1:1 blit of the 16×16 source PNG. Larger values
    // work but introduce nearest-neighbor pixel stretching (what the user saw
    // as "slightly broken" in 1.4.5). Gap + margins bumped to compensate for
    // the smaller footprint.
    private static final int IM_ICON_SIZE = 16;
    private static final int IM_ICON_GAP = 3;
    private static final int IM_MARGIN_RIGHT = 6;
    private static final int IM_MARGIN_TOP = 6;

    @SubscribeEvent
    public static void onScreenInit(final ScreenEvent.Init.Post event) {
        if (!EIClientConfig.SHOW_OVERLAY.get()) return;
        if (!AuraRegistry.isLoaded()) return;

        Screen screen = event.getScreen();

        boolean isStationary = screen instanceof InstrumentScreen
                || EIClientConfig.SCREEN_CLASS_ALLOWLIST.get().contains(screen.getClass().getName());
        boolean isMobile = IM_SCREEN_CLASSES.contains(screen.getClass().getName());

        if (!isStationary && !isMobile) return;

        // Note: when Init.Post re-fires for the same Screen instance (window
        // resize, vanilla rebuildWidgets), Minecraft has already cleared
        // screen.children. We deliberately do NOT reuse the existing widget
        // here — width-derived layout is computed in the constructor from
        // parentScreen.width, so a resize needs a fresh construction to
        // reposition correctly. The old widget becomes GC-eligible.

        if (isMobile) {
            // Mobile path: IM screens rebuild widgets aggressively, so we don't
            // try to live as a Screen child. Instead we render directly via
            // ScreenEvent.Render.Post and hit-test clicks in MouseButtonPressed.Pre.
            handleImScreenOpen(screen);
            return;
        }

        List<AuraPreset> allowed;
        currentIsMobileTier = false;
        if (screen instanceof InstrumentScreen instrumentScreen) {
            currentInstrumentId = instrumentScreen.getInstrumentId();
            EIPacketHandler.sendToServer(
                    new InstrumentOpenC2SPacket(currentInstrumentId, /* mobileTier */ false)
            );
            String override = instrumentAuraOverrides.get(currentInstrumentId);
            if (override != null) {
                currentSelectedAuraId = override;
                EIPacketHandler.sendToServer(new SelectAuraC2SPacket(override));
            }
        }
        allowed = InstrumentAuraMapping.getAllowedAuras(currentInstrumentId);

        selectorWidget = new AuraSelectorWidget(
                screen,
                allowed,
                currentSelectedAuraId,
                AuraOverlayInjector::onAuraSelected
        );
        event.addListener(selectorWidget);
    }

    /**
     * Populate the IM render-overlay state and notify the server. Resolves the
     * held instrument id, looks up allowed mobile auras, and falls back to
     * every enabled mobile-tier preset if the user's mapping file doesn't
     * cover this instrument — users with missing/deleted mappings otherwise
     * saw no overlay at all.
     */
    private static void handleImScreenOpen(Screen screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            EffectiveInstrumentsMod.LOGGER.debug(
                    "[EI overlay] IM screen opened but Minecraft.player is null — skipping overlay");
            return;
        }

        ResourceLocation imId = findHeldImInstrument(player);
        if (imId == null) {
            // Fallback: inspect whatever's held in either hand and see if it
            // LOOKS like an IM instrument by namespace. If so, proceed using
            // that id even without a mapping entry. This catches users whose
            // mapping file is missing or out-of-date.
            imId = findHeldByNamespace(player, "immersive_melodies");
            if (imId == null) {
                EffectiveInstrumentsMod.LOGGER.info(
                        "[EI overlay] IM screen {} opened but no IM instrument is held — overlay skipped",
                        screen.getClass().getSimpleName());
                // User-visible hint: without this, the player sees nothing and
                // blames the mod. Action-bar is non-intrusive and auto-clears.
                player.displayClientMessage(
                        Component.translatable("message.effectiveinstruments.im_overlay_hold_required")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW),
                        true);
                return;
            }
            EffectiveInstrumentsMod.LOGGER.info(
                    "[EI overlay] held IM instrument '{}' has no mapping entry — showing all enabled mobile presets",
                    imId);
        }

        List<AuraPreset> allowed = MobileInstrumentAuraMapping.getAllowedAuras(imId);
        if (allowed.isEmpty()) {
            // Mapping might exist but have resolved to empty. Fall back to
            // every enabled mobile-tier preset so the UI is at least present.
            allowed = new ArrayList<>();
            for (AuraPreset preset : AuraRegistry.getEnabledPresets()) {
                if (preset.supports(com.crims.effectiveinstruments.aura.BuffTier.MOBILE)) {
                    allowed.add(preset);
                }
            }
            EffectiveInstrumentsMod.LOGGER.info(
                    "[EI overlay] mobile allow-list empty for '{}' — falling back to {} enabled mobile presets",
                    imId, allowed.size());
        }
        if (allowed.isEmpty()) {
            EffectiveInstrumentsMod.LOGGER.info(
                    "[EI overlay] no mobile presets enabled at all — overlay skipped");
            return;
        }

        currentInstrumentId = imId;
        currentIsMobileTier = true;
        imOverlayScreen = screen;
        imOverlayAllowed.clear();
        imOverlayAllowed.addAll(allowed);
        rebuildImRects(screen);

        EIPacketHandler.sendToServer(
                new InstrumentOpenC2SPacket(imId, /* mobileTier */ true)
        );

        String override = instrumentAuraOverrides.get(imId);
        if (override != null) {
            currentSelectedAuraId = override;
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(override, imId.toString()));
        } else {
            currentSelectedAuraId = null;
        }
        EffectiveInstrumentsMod.LOGGER.info(
                "[EI overlay] IM overlay active on {} for '{}' with {} presets (selected={})",
                screen.getClass().getSimpleName(), imId, allowed.size(),
                currentSelectedAuraId == null ? "none" : currentSelectedAuraId);
    }

    /**
     * Fallback instrument-id lookup when the mapping doesn't know about the
     * held item. Returns the first held stack whose item namespace matches
     * {@code namespace}.
     */
    @Nullable
    private static ResourceLocation findHeldByNamespace(LocalPlayer player, String namespace) {
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId != null && namespace.equals(itemId.getNamespace())) return itemId;
        }
        return null;
    }

    /**
     * Recompute the icon-click rectangles for the current IM screen. Lays out
     * icons top-right with margin/gap matching the stationary selector.
     */
    private static void rebuildImRects(Screen screen) {
        imOverlayRects.clear();
        int count = imOverlayAllowed.size();
        if (count == 0) return;

        int maxRowWidth = (int) (screen.width * 0.6);
        int buttonsPerRow = Math.max(1, Math.min(count,
                (maxRowWidth + IM_ICON_GAP) / (IM_ICON_SIZE + IM_ICON_GAP)));
        int rowWidth = buttonsPerRow * IM_ICON_SIZE + (buttonsPerRow - 1) * IM_ICON_GAP;

        int col = 0;
        int offsetX = 0;
        int offsetY = 0;
        int baseX = screen.width - rowWidth - IM_MARGIN_RIGHT;
        int baseY = IM_MARGIN_TOP;
        for (int i = 0; i < count; i++) {
            imOverlayRects.add(new int[]{
                    baseX + offsetX, baseY + offsetY, IM_ICON_SIZE, IM_ICON_SIZE
            });
            col++;
            if (col >= buttonsPerRow) {
                col = 0;
                offsetX = 0;
                offsetY += IM_ICON_SIZE + IM_ICON_GAP;
            } else {
                offsetX += IM_ICON_SIZE + IM_ICON_GAP;
            }
        }
    }

    /**
     * Draw the IM aura selector on top of the host screen every frame. This
     * path is invariant to IM's widget-list rebuilds — we never insert into
     * screen.children, we just draw.
     */
    @SubscribeEvent
    public static void onImScreenRenderPost(final ScreenEvent.Render.Post event) {
        if (imOverlayScreen == null) return;
        if (event.getScreen() != imOverlayScreen) return;
        if (imOverlayAllowed.isEmpty() || imOverlayRects.size() != imOverlayAllowed.size()) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        for (int i = 0; i < imOverlayAllowed.size(); i++) {
            AuraPreset preset = imOverlayAllowed.get(i);
            int[] r = imOverlayRects.get(i);
            int x = r[0], y = r[1], w = r[2], h = r[3];
            boolean selected = preset.id().equals(currentSelectedAuraId);
            drawImIcon(gfx, preset, x, y, w, h, selected, mouseX, mouseY);
        }
    }

    /**
     * Draw one aura icon. Tries the preset's texture first (matching the
     * stationary widget's behavior); on resource miss falls back to a
     * color-filled square with the first letter of the display name. The
     * polarity-colored border makes selection obvious without requiring a
     * texture.
     */
    private static void drawImIcon(
            GuiGraphics gfx, AuraPreset preset,
            int x, int y, int w, int h,
            boolean selected, int mouseX, int mouseY
    ) {
        // Try texture first. The shipped PNGs are pre-baked with border and
        // polarity styling, so if we get the texture we don't need to draw
        // any scaffolding underneath it.
        ResourceLocation tex = selected ? preset.selectedIconTexture() : preset.iconTexture();
        if (tex == null) tex = preset.iconTexture();
        boolean drewTexture = false;
        if (tex != null) {
            try {
                // 10-arg scaling blit — same overload the stationary widget
                // uses at AuraButtonWidget.java:92. Maps the full 16×16 source
                // onto the w×h destination with nearest-neighbor scaling.
                gfx.blit(tex, x, y, w, h, 0.0f, 0.0f, 16, 16, 16, 16);
                drewTexture = true;
            } catch (Exception ignored) {
                // Resource missing or blit failed — fall through to letter.
            }
        }
        if (!drewTexture) {
            // Letter fallback (matches AuraButtonWidget): fill colored bg,
            // draw polarity border, render first letter in the center.
            int border = preset.isOffensive()
                    ? (selected ? 0xFFFF5050 : 0xFF961E1E)
                    : (selected ? 0xFF78DC78 : 0xFF287828);
            int bg = 0xFF000000 | (preset.color() & 0xFFFFFF);
            gfx.fill(x, y, x + w, y + h, bg);
            gfx.fill(x, y, x + w, y + 1, border);
            gfx.fill(x, y + h - 1, x + w, y + h, border);
            gfx.fill(x, y, x + 1, y + h, border);
            gfx.fill(x + w - 1, y, x + w, y + h, border);

            String letter = "?";
            Component name = preset.displayName();
            if (name != null) {
                String s = name.getString();
                if (s != null && !s.isEmpty()) letter = s.substring(0, 1).toUpperCase();
            }
            var font = Minecraft.getInstance().font;
            int textW = font.width(letter);
            gfx.drawString(font, letter,
                    x + (w - textW) / 2, y + (h - 8) / 2,
                    0xFFFFFFFF, true);
        }

        // Selection highlight: 1px outer ring so textured icons still show
        // "this is selected" — the pre-baked PNG selected-state is subtle.
        if (selected) {
            int ring = preset.isOffensive() ? 0xFFFFAA55 : 0xFFAAFF88;
            gfx.fill(x - 1, y - 1, x + w + 1, y, ring);
            gfx.fill(x - 1, y + h, x + w + 1, y + h + 1, ring);
            gfx.fill(x - 1, y, x, y + h, ring);
            gfx.fill(x + w, y, x + w + 1, y + h, ring);
        }

        // Tooltip on hover — show the preset's display name + description.
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            List<Component> lines = new ArrayList<>();
            lines.add(preset.displayName() != null
                    ? preset.displayName()
                    : Component.literal(preset.id()));
            if (preset.description() != null) {
                String desc = preset.description().getString();
                if (desc != null && !desc.isEmpty()) {
                    lines.add(preset.description());
                }
            }
            gfx.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
        }
    }

    /**
     * Handle left-click on an IM icon. Fires before the screen's own
     * mouseClicked, and when we consume the click we cancel propagation so
     * the underlying IM widget (melody row etc.) doesn't also react.
     */
    @SubscribeEvent
    public static void onImMousePressed(final ScreenEvent.MouseButtonPressed.Pre event) {
        if (imOverlayScreen == null) return;
        if (event.getScreen() != imOverlayScreen) return;
        if (event.getButton() != 0) return; // left-click only
        if (imOverlayAllowed.isEmpty() || imOverlayRects.size() != imOverlayAllowed.size()) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (int i = 0; i < imOverlayAllowed.size(); i++) {
            int[] r = imOverlayRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                AuraPreset preset = imOverlayAllowed.get(i);
                boolean deselect = preset.id().equals(currentSelectedAuraId);
                onAuraSelected(deselect ? null : preset);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClose(final ScreenEvent.Closing event) {
        Screen closed = event.getScreen();
        boolean wasStationary = selectorWidget != null && closed == selectorWidget.parentScreen;
        boolean wasImOverlay = imOverlayScreen != null && closed == imOverlayScreen;
        if (!wasStationary && !wasImOverlay) return;

        if (currentInstrumentId != null && currentSelectedAuraId != null) {
            instrumentAuraOverrides.put(currentInstrumentId, currentSelectedAuraId);
        } else if (currentInstrumentId != null) {
            instrumentAuraOverrides.remove(currentInstrumentId);
        }
        // Notify server the screen closed so the mobile handler can enter
        // its linger / idle path.
        if (currentIsMobileTier && currentInstrumentId != null) {
            EIPacketHandler.sendToServer(
                    new InstrumentOpenC2SPacket(currentInstrumentId, /* mobileTier */ true, /* close */ true)
            );
        }
        selectorWidget = null;
        imOverlayScreen = null;
        imOverlayAllowed.clear();
        imOverlayRects.clear();
        currentSelectedAuraId = null;
        currentInstrumentId = null;
        currentIsMobileTier = false;
    }

    @SubscribeEvent
    public static void onPlayerLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        instrumentAuraOverrides.clear();
        currentSelectedAuraId = null;
        currentInstrumentId = null;
        currentIsMobileTier = false;
        selectorWidget = null;
        imOverlayScreen = null;
        imOverlayAllowed.clear();
        imOverlayRects.clear();
    }

    /**
     * Walk both hands looking for an item that has a mobile mapping entry.
     * The held item is how IM screens identify their instrument — the screen
     * classes don't publish it.
     */
    @Nullable
    private static ResourceLocation findHeldImInstrument(LocalPlayer player) {
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) continue;
            if (MobileInstrumentAuraMapping.hasMapping(itemId)) return itemId;
        }
        return null;
    }

    private static void onAuraSelected(@Nullable AuraPreset preset) {
        String newId = preset == null ? null : preset.id();
        currentSelectedAuraId = newId;
        if (currentInstrumentId != null) {
            if (newId == null) {
                instrumentAuraOverrides.remove(currentInstrumentId);
            } else {
                instrumentAuraOverrides.put(currentInstrumentId, newId);
            }
        }
        String auraArg = newId == null ? "" : newId;
        if (currentIsMobileTier && currentInstrumentId != null) {
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(auraArg, currentInstrumentId.toString()));
        } else {
            EIPacketHandler.sendToServer(new SelectAuraC2SPacket(auraArg));
        }
    }

    /**
     * Called by the server sync packet when a default aura is auto-selected.
     * Only applies if the client has no override for the current instrument.
     */
    public static void onServerSyncAura(String auraId) {
        if (currentInstrumentId != null && instrumentAuraOverrides.containsKey(currentInstrumentId)) {
            return;
        }
        currentSelectedAuraId = auraId.isEmpty() ? null : auraId;
        if (selectorWidget != null) {
            selectorWidget.setSelectedAuraId(currentSelectedAuraId);
        }
    }
}
