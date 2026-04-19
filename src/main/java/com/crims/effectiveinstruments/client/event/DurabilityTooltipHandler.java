package com.crims.effectiveinstruments.client.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Appends an {@code "Instrument Durability: <cur>/<max>"} line to the tooltip
 * of tracked instruments. Client-side only, Forge event bus.
 *
 * <p>No bar widget — Forge's {@code IClientItemExtensions.getDurabilityForDisplay}
 * can't be registered for foreign items, so a text line is the portable option.
 */
@Mod.EventBusSubscriber(modid = EffectiveInstrumentsMod.MODID, value = Dist.CLIENT)
public final class DurabilityTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // Fail closed if config not loaded yet (rare, e.g. world-menu tooltip).
        try {
            if (!EIServerConfig.DURABILITY_ENABLED.get()) return;
        } catch (IllegalStateException ignored) {
            return;
        }

        if (!InstrumentDurability.isTracked(event.getItemStack())) return;

        int current = InstrumentDurability.getCurrent(event.getItemStack());
        int max = InstrumentDurability.getMax(event.getItemStack());

        ChatFormatting valueColour = current <= 0
                ? ChatFormatting.RED
                : current <= max / 4
                        ? ChatFormatting.GOLD
                        : ChatFormatting.WHITE;

        MutableComponent line = Component.literal("Instrument Durability: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(current + "/" + max).withStyle(valueColour));

        event.getToolTip().add(line);

        if (current <= 0) {
            event.getToolTip().add(Component.literal("  Broken — repair on an anvil")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        // Aura-system hint. A lot of new players hold an instrument and never
        // discover the top-right aura selector; this one line closes the gap.
        event.getToolTip().add(Component.translatable("tooltip.effectiveinstruments.aura_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private DurabilityTooltipHandler() {}
}
