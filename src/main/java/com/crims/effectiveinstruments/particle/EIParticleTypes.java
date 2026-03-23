package com.crims.effectiveinstruments.particle;

import com.crims.effectiveinstruments.EffectiveInstrumentsMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EIParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EffectiveInstrumentsMod.MODID);

    public static final RegistryObject<ParticleType<AuraNoteParticleOptions>> AURA_NOTE =
            PARTICLE_TYPES.register("aura_note", () -> new ParticleType<>(false, AuraNoteParticleOptions.DESERIALIZER) {
                @Override
                public Codec<AuraNoteParticleOptions> codec() {
                    return AuraNoteParticleOptions.CODEC;
                }
            });
}
