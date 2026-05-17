package com.crims.effectiveinstruments.performer;

import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Generic mobile-instrument stack lookup for any {@link LivingEntity} —
 * player or NPC. Phase 0 uses this from {@link PlayerPerformer#instrumentStack}
 * on the mobile path; future per-mod adapters call this directly when their
 * performer's mainhand is the canonical instrument slot.
 *
 * <p>Tries (in order):
 * <ol>
 *   <li>The IM-marked stack via {@link ImmersiveMelodiesCompat#findActivePlayingStack}
 *       (any stack with the {@code playing=true} NBT and a registered mobile mapping).</li>
 *   <li>Any held stack whose item id has a mobile mapping entry (screen-open path,
 *       free-play, NPC adapter goal-driven path).</li>
 * </ol>
 * <p>Returns {@link ItemStack#EMPTY} when no qualifying stack is found — never null.
 */
public final class MobileInstrumentLookup {

    public static ItemStack find(LivingEntity entity) {
        ImmersiveMelodiesCompat.HeldInstrument held = ImmersiveMelodiesCompat.findActivePlayingStack(entity);
        if (held != null) return held.stack();

        for (ItemStack stack : entity.getHandSlots()) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) continue;
            if (MobileInstrumentAuraMapping.hasMapping(itemId)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private MobileInstrumentLookup() {}
}
