package com.crims.effectiveinstruments.client.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Draws a vanilla-style durability bar across the bottom of instrument item
 * slots. Instrument items are owned by foreign mods (Genshin Instruments, Even
 * More Instruments, Immersive Melodies) so we can't set {@code maxDamage} on
 * their {@link Item} classes — without that, vanilla's own bar code
 * ({@link Item#isBarVisible}, {@link Item#getBarWidth}, {@link Item#getBarColor})
 * never fires. Instead we attach an {@link IItemDecorator} to every item whose
 * mod id is in {@link #INSTRUMENT_MOD_IDS}; the decorator runs on top of the
 * slot render and draws the bar when the stack has NBT durability tracked.
 *
 * <p>The decorator is purely visual — it reads {@link InstrumentDurability}
 * state, it doesn't mutate anything.
 */
@Mod.EventBusSubscriber(
        bus = Mod.EventBusSubscriber.Bus.MOD,
        modid = EffectiveInstrumentsMod.MODID,
        value = Dist.CLIENT
)
public final class InstrumentDurabilityBarDecorator {

    private static final Set<String> INSTRUMENT_MOD_IDS = Set.of(
            "genshinstrument",
            "evenmoreinstruments",
            "immersive_melodies"
    );

    @SubscribeEvent
    public static void onRegisterItemDecorations(final RegisterItemDecorationsEvent event) {
        int registered = 0;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            if (!INSTRUMENT_MOD_IDS.contains(id.getNamespace())) continue;
            event.register(item, InstrumentDurabilityBarDecorator::renderBar);
            registered++;
        }
        EffectiveInstrumentsMod.LOGGER.info(
                "Registered durability-bar decorator for {} potential instrument item(s) (mod ids: {})",
                registered, INSTRUMENT_MOD_IDS);
    }

    /**
     * Draws the bar in the bottom two rows of the 16×16 slot — matches vanilla's
     * own placement (y = slot.y + 13, length 13 px). Never draws on untracked
     * stacks or when durability is at max (matches vanilla's "hide at full"
     * behavior). Returns false so stack-count overlay still renders.
     */
    private static boolean renderBar(
            GuiGraphics graphics, Font font, ItemStack stack, int xOffset, int yOffset
    ) {
        if (stack.isEmpty()) return false;
        if (!EIServerConfig.DURABILITY_ENABLED.get()) return false;
        if (!InstrumentDurability.isTracked(stack)) return false;

        int max = InstrumentDurability.getMax(stack);
        int current = InstrumentDurability.getCurrent(stack);
        if (max <= 0) return false;
        // Hide bar when at full — vanilla convention.
        if (current >= max) return false;

        float ratio = Math.max(0f, Math.min(1f, (float) current / (float) max));
        int width = Math.round(13.0f * ratio);
        int color = barColor(ratio);

        // Place bar exactly where vanilla's is: 2px tall, starting 13px down,
        // 13px wide maximum, 2px from the left of the slot. Dark backing row,
        // then the color row.
        int x = xOffset + 2;
        int y = yOffset + 13;

        graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),
                x, y, x + 13, y + 2, 0xFF000000);
        graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),
                x, y, x + width, y + 1, 0xFF000000 | color);
        return false;
    }

    /**
     * Green → yellow → red gradient, same intent as vanilla's HSV-based bar
     * color. Kept simple: three bands so the bar is readable at a glance.
     */
    private static int barColor(float ratio) {
        if (ratio > 0.66f) {
            return (0x55 << 16) | (0xFF << 8) | 0x55;
        } else if (ratio > 0.33f) {
            return (0xFF << 16) | (0xDD << 8) | 0x33;
        } else {
            return (0xFF << 16) | (0x55 << 8) | 0x44;
        }
    }

    private InstrumentDurabilityBarDecorator() {}
}
