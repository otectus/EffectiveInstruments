package com.crims.effectiveinstruments.command;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.AuraJsonLoader;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        );
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
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments help     - this message")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Debug logging: " + (EIServerConfig.DEBUG_MODE.get() ? "on" : "off"))
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Toggle debug via server config (effectiveinstruments-server.toml → debugMode)")
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
