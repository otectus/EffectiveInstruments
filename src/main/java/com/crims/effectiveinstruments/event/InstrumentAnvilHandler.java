package com.crims.effectiveinstruments.event;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.config.InstrumentDurabilityConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
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
 * <p>1.4.9 (RECS §2.4): both paths now follow vanilla's anvil-cost growth.
 * Each repair reads the stack's {@code RepairCost} NBT, computes the next
 * value via {@link AnvilMenu#calculateIncreasedRepairCost} ({@code prev*2+1}),
 * and writes it back on the output. Repeated repairs eventually become
 * "Too Expensive!" — same as vanilla tools — so instruments aren't
 * indefinitely cheap to maintain.
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

        // 1.4.9 (RECS §2.4): mirror vanilla AnvilMenu cost growth. Each
        // repair reads RepairCost on the input and bumps it on the output via
        // calculateIncreasedRepairCost (= prev*2 + 1). Without this an
        // instrument is indefinitely cheap to keep repaired — a 2-level combine
        // every time, no escalation.
        int prevRepairCost = left.getBaseRepairCost();
        int nextRepairCost = AnvilMenu.calculateIncreasedRepairCost(prevRepairCost);

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
            output.setRepairCost(nextRepairCost);

            event.setOutput(output);
            event.setMaterialCost(1);
            // Vanilla combine adds a small flat constant on top of the
            // escalating prev-cost — keep the same shape so a fresh-from-craft
            // instrument still costs ~2 levels on its first combine.
            event.setCost(nextRepairCost + 2);
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
        output.setRepairCost(nextRepairCost);

        event.setOutput(output);
        event.setMaterialCost(materialsNeeded);
        // Cost = escalating prev + per-material flat. Vanilla shape, so big
        // damage-from-broken jobs sting more than a touch-up.
        event.setCost(nextRepairCost + materialsNeeded * 2);
    }

    private static void setDurability(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(InstrumentDurability.TAG_KEY, value);
    }

    private InstrumentAnvilHandler() {}
}
