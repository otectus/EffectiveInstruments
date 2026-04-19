package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.config.InstrumentDurabilityConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles anvil repair for tracked instruments. Two paths:
 * <ul>
 *   <li><b>Combine two damaged copies</b> (left + right are the same instrument):
 *       output durability = min(max, leftCur + rightCur + bonus), where bonus is
 *       {@link EIServerConfig#DURABILITY_ANVIL_REPAIR_BONUS_PERCENT}% of max —
 *       mirrors vanilla tool behavior at 12%.</li>
 *   <li><b>Material repair</b> (right slot matches the instrument's configured
 *       {@code repairMaterial}): restore {@code repairPerUnit} durability per
 *       item consumed, up to what's needed to reach max.</li>
 * </ul>
 *
 * <p>Registered manually from {@link com.crims.effectiveinstruments.EffectiveInstrumentsMod}
 * rather than via {@code @EventBusSubscriber} so the listener is bound to the
 * Forge event bus with a clear lifecycle. Same concurrency contract as other
 * Forge events — main thread only.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = EffectiveInstrumentsMod.MODID)
public final class InstrumentAnvilHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!EIServerConfig.DURABILITY_ENABLED.get()) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) return;

        if (!InstrumentDurability.isTracked(left)) return;

        int current = InstrumentDurability.getCurrent(left);
        int max = InstrumentDurability.getMax(left);
        if (current >= max) return; // nothing to repair

        ResourceLocation leftId = ForgeRegistries.ITEMS.getKey(left.getItem());
        if (leftId == null) return;

        // Combine two damaged copies of the same instrument.
        if (right.getItem() == left.getItem()) {
            int rightCurrent = InstrumentDurability.getCurrent(right);
            int bonus = max * EIServerConfig.DURABILITY_ANVIL_REPAIR_BONUS_PERCENT.get() / 100;
            int newDurability = Math.min(max, current + rightCurrent + bonus);

            ItemStack output = left.copy();
            // Preserve the anvil's rename semantics.
            if (event.getName() != null && !event.getName().isEmpty()) {
                output.setHoverName(net.minecraft.network.chat.Component.literal(event.getName()));
            }
            setDurability(output, newDurability);

            event.setOutput(output);
            event.setMaterialCost(1);
            event.setCost(2);
            return;
        }

        // Material-based repair.
        InstrumentDurabilityConfig.Entry entry = InstrumentDurabilityConfig.get(leftId);
        if (entry == null || entry.repairMaterial() == null) return;

        ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
        if (rightId == null || !rightId.equals(entry.repairMaterial())) return;

        int needed = max - current;
        int perUnit = Math.max(1, entry.repairPerUnit());
        // How many material items are needed (rounded up).
        int materialsNeeded = Math.min(right.getCount(), (needed + perUnit - 1) / perUnit);
        if (materialsNeeded <= 0) return;

        int restoredBy = Math.min(needed, materialsNeeded * perUnit);
        int newDurability = Math.min(max, current + restoredBy);

        ItemStack output = left.copy();
        if (event.getName() != null && !event.getName().isEmpty()) {
            output.setHoverName(net.minecraft.network.chat.Component.literal(event.getName()));
        }
        setDurability(output, newDurability);

        event.setOutput(output);
        event.setMaterialCost(materialsNeeded);
        // Keep xp cost low but non-zero so players feel the interaction. Scales with
        // materials consumed so repairing from broken stings more than a touch-up.
        event.setCost(Math.max(1, materialsNeeded * 2));
    }

    private static void setDurability(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(InstrumentDurability.TAG_KEY, value);
    }

    private InstrumentAnvilHandler() {}
}
