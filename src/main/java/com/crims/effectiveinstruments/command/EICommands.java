package com.crims.effectiveinstruments.command;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.mojang.brigadier.CommandDispatcher;
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
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.success",
                                                    total, enabled, mappings),
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
        );
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer player) {
        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        if (state == null) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.none"),
                    false);
            return 1;
        }

        source.sendSuccess(
                () -> Component.translatable("command.effectiveinstruments.status.header"),
                false);
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

        return 1;
    }

    private EICommands() {}
}
