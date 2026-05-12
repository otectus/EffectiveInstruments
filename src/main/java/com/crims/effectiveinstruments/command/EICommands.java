package com.crims.effectiveinstruments.command;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.AuraJsonLoader;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.compat.genshin.GenshinInstrumentsCompat;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class EICommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("effectiveinstruments")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    AuraRegistry.reload();
                                    int total = AuraRegistry.getAllPresets().size();
                                    int enabled = AuraRegistry.getEnabledPresets().size();
                                    int mappings = InstrumentAuraMapping.getMappingCount();
                                    int mobileMappings = MobileInstrumentAuraMapping.getMappingCount();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.success",
                                                    total, enabled, mappings),
                                            true);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.mobile_mappings",
                                                    mobileMappings,
                                                    ImmersiveMelodiesCompat.isAvailable() ? "active" : "inactive"),
                                            true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("status")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        return showStatus(ctx.getSource(), player);
                                    }
                                    ctx.getSource().sendFailure(Component.literal("Must specify a player or run as a player"));
                                    return 0;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            return showStatus(ctx.getSource(), player);
                                        })
                                )
                        )
                        .then(Commands.literal("help")
                                .executes(ctx -> {
                                    showHelp(ctx.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("durability")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("get")
                                        .executes(ctx -> runDurabilityGet(ctx.getSource())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> runDurabilitySet(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("repair")
                                        .executes(ctx -> runDurabilityRepair(ctx.getSource())))
                        )
                        .then(Commands.literal("diagnose")
                                .requires(source -> source.hasPermission(0))
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        return runDiagnose(ctx.getSource(), player);
                                    }
                                    ctx.getSource().sendFailure(Component.literal("Run as a player."));
                                    return 0;
                                })
                        )
                        .then(Commands.literal("reset-mappings")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> runResetMappings(ctx.getSource()))
                        )
        );
    }

    /**
     * Nuke-and-rebuild for the instrument-aura mapping file + every marker.
     * Added in 1.4.8 as a recovery path for users whose mapping got stuck in a
     * duplicate-offensive state. Preset JSONs are untouched — only the
     * instrument-to-aura routing table is regenerated.
     */
    private static int runResetMappings(CommandSourceStack source) {
        java.util.List<String> deleted = InstrumentAuraMapping.resetMappings();
        AuraRegistry.reload();
        source.sendSuccess(() -> Component.literal(
                "Instrument-aura mappings reset. Deleted: " + deleted + ". Mappings regenerated: "
                        + InstrumentAuraMapping.getMappingCount()
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * Dump every piece of state that gates the aura pipeline so the user can
     * see exactly why nothing is firing. Added in 1.4.7 after the
     * "Wither did nothing to a zombie" complaint where the failure mode was
     * silent across multiple gate layers.
     */
    private static int runDiagnose(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("=== EI Diagnose ===").withStyle(ChatFormatting.GOLD), false);

        // 1.5.0: backend availability is the first thing to surface so users
        // diagnosing "nothing fires" can rule out "no backend installed" in
        // a single command instead of poking at logs.
        source.sendSuccess(() -> Component.literal(
                "Backends: genshin=" + (GenshinInstrumentsCompat.isAvailable() ? "active" : "absent")
                        + " immersive_melodies=" + (ImmersiveMelodiesCompat.isAvailable() ? "active" : "absent")
        ).withStyle(ChatFormatting.GRAY), false);
        if (!GenshinInstrumentsCompat.isAvailable() && !ImmersiveMelodiesCompat.isAvailable()) {
            source.sendSuccess(() -> Component.literal(
                    "WARNING: no instrument backend installed — gameplay features inactive."
            ).withStyle(ChatFormatting.YELLOW), false);
        }

        ItemStack heldMain = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack heldOff = player.getItemInHand(InteractionHand.OFF_HAND);
        source.sendSuccess(() -> Component.literal(
                "Main-hand: " + heldMain.getItem() + " tracked=" + InstrumentDurability.isTracked(heldMain)
                        + " dur=" + InstrumentDurability.getCurrent(heldMain) + "/" + InstrumentDurability.getMax(heldMain)
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Off-hand:  " + heldOff.getItem() + " tracked=" + InstrumentDurability.isTracked(heldOff)
                        + " dur=" + InstrumentDurability.getCurrent(heldOff) + "/" + InstrumentDurability.getMax(heldOff)
        ).withStyle(ChatFormatting.GRAY), false);

        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        long now = player.level().getGameTime();
        if (state == null) {
            source.sendSuccess(() -> Component.literal("No PlayerAuraState — never opened an instrument this session.")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            String selAura = state.getSelectedAura() != null ? state.getSelectedAura().id() : "(none)";
            boolean offensive = state.getSelectedAura() != null && state.getSelectedAura().isOffensive();
            boolean active = AuraManager.isActiveTest(player.getUUID(), now);
            source.sendSuccess(() -> Component.literal(
                    "Selected aura: " + selAura + " offensive=" + offensive
                            + " instrument=" + (state.getCurrentInstrumentId() != null
                                    ? state.getCurrentInstrumentId() : "(none)")
            ).withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal(
                    "Screen-open flag: " + state.isInstrumentOpen()
                            + " | isActive=" + active + " | affected targets=" + state.getAffectedTargetCount()
            ).withStyle(ChatFormatting.GRAY), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Durability: enabled=" + EIServerConfig.DURABILITY_ENABLED.get()
                        + " creativeImmunity=" + EIServerConfig.DURABILITY_CREATIVE_IMMUNITY.get()
                        + " playerCreative=" + player.getAbilities().instabuild
        ).withStyle(ChatFormatting.GRAY), false);

        // 1.4.9 (RECS §3.10): symmetric positive-targeting block. Without
        // this, "my regen aura doesn't hit my ally" had no diagnostic surface.
        source.sendSuccess(() -> Component.literal(
                "Positive targeting: otherPlayers=" + EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYERS.get()
                        + " otherPlayerPets=" + EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYER_PETS.get()
                        + " villagers=" + EIServerConfig.POSITIVE_INCLUDE_VILLAGERS.get()
                        + " ironGolems=" + EIServerConfig.POSITIVE_INCLUDE_IRON_GOLEMS.get()
                        + " passive=" + EIServerConfig.POSITIVE_INCLUDE_PASSIVE_MOBS.get()
                        + " hostile=" + EIServerConfig.POSITIVE_INCLUDE_HOSTILE_MOBS.get()
        ).withStyle(ChatFormatting.GRAY), false);

        source.sendSuccess(() -> Component.literal(
                "Offensive targeting: enabled=" + EIServerConfig.OFFENSIVE_AURAS_ENABLED.get()
                        + " allNonPets=" + EIServerConfig.OFFENSIVE_INCLUDE_ALL_NON_PETS.get()
                        + " (fine-grained if false: hostile=" + EIServerConfig.OFFENSIVE_INCLUDE_HOSTILE_MOBS.get()
                        + " passive=" + EIServerConfig.OFFENSIVE_INCLUDE_PASSIVE_MOBS.get()
                        + " villagers=" + EIServerConfig.OFFENSIVE_INCLUDE_VILLAGERS.get()
                        + " golems=" + EIServerConfig.OFFENSIVE_INCLUDE_IRON_GOLEMS.get()
                        + " players=" + EIServerConfig.OFFENSIVE_INCLUDE_OTHER_PLAYERS.get() + ")"
        ).withStyle(ChatFormatting.GRAY), false);

        source.sendSuccess(() -> Component.literal(
                "Activation: noteThresholdMin=" + EIServerConfig.NOTE_THRESHOLD_MIN.get()
                        + " windowTicks=" + EIServerConfig.NOTE_THRESHOLD_WINDOW_TICKS.get()
                        + " noteWindowTicks=" + EIServerConfig.NOTE_WINDOW_TICKS.get()
                        + " auraTickInterval=" + EIServerConfig.AURA_TICK_INTERVAL.get()
        ).withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int runDurabilityGet(CommandSourceStack source) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int cur = InstrumentDurability.getCurrent(stack);
        int max = InstrumentDurability.getMax(stack);
        source.sendSuccess(
                () -> Component.literal(String.format("Durability: %d/%d%s",
                        cur, max, cur <= 0 ? " (broken)" : "")),
                false);
        return 1;
    }

    private static int runDurabilitySet(CommandSourceStack source, int value) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int newValue = InstrumentDurability.set(stack, value);
        int max = InstrumentDurability.getMax(stack);
        source.sendSuccess(
                () -> Component.literal("Durability set to " + newValue + "/" + max),
                true);
        return 1;
    }

    private static int runDurabilityRepair(CommandSourceStack source) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int max = InstrumentDurability.getMax(stack);
        InstrumentDurability.set(stack, max);
        source.sendSuccess(
                () -> Component.literal("Instrument fully repaired (" + max + "/" + max + ")"),
                true);
        return 1;
    }

    private static ItemStack resolveHeldInstrument(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (InstrumentDurability.isTracked(main)) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (InstrumentDurability.isTracked(off)) return off;
        return null;
    }

    private static void showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Effective Instruments ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Config dir: " + AuraJsonLoader.getAurasDir().getParent())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Commands:")
                .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments reload   - reload aura presets and mappings")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments status   - inspect a player's aura state")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments durability {get|set <n>|repair} - inspect or edit held instrument durability (OP)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments diagnose - dump pipeline state (why isn't my aura firing?)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments reset-mappings - delete + regenerate the instrument-aura mapping (OP)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments help     - this message")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Debug logging: " + (EIServerConfig.DEBUG_MODE.get() ? "on" : "off"))
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Toggle debug via server config (config/effective_instruments/server.toml → debugMode)")
                .withStyle(ChatFormatting.DARK_GRAY), false);
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer player) {
        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        ImmersiveMelodiesAuraHandler.MobileStateView mobile =
                ImmersiveMelodiesAuraHandler.getView(player.getUUID());

        if (state == null && mobile == null) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.none"),
                    false);
            return 1;
        }

        source.sendSuccess(
                () -> Component.translatable("command.effectiveinstruments.status.header"),
                false);

        // Stationary block
        if (state != null) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.aura",
                            state.getSelectedAura() != null ? state.getSelectedAura().id() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.instrument",
                            state.getCurrentInstrumentId() != null ? state.getCurrentInstrumentId().toString() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.active",
                            state.isInstrumentOpen() ? "yes" : "no"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.targets",
                            state.getAffectedTargetCount()),
                    false);
        }

        // Mobile block — only when compat is available (otherwise it's just noise for every server).
        if (ImmersiveMelodiesCompat.isAvailable()) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_aura",
                            mobile != null ? mobile.auraId() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_instrument",
                            mobile != null && mobile.instrumentId() != null
                                    ? mobile.instrumentId().toString()
                                    : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_active",
                            mobile != null ? "yes" : "no"),
                    false);
            if (mobile != null) {
                source.sendSuccess(
                        () -> Component.translatable("command.effectiveinstruments.status.mobile_targets",
                                mobile.affectedTargetCount()),
                        false);
            }
        }

        return 1;
    }

    private EICommands() {}
}
