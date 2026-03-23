package com.crims.effectiveinstruments.command;

import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.success",
                                                    total, enabled),
                                            true);
                                    return 1;
                                })
                        )
        );
    }

    private EICommands() {}
}
