package com.crims.effectiveinstruments.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public record AuraNoteParticleOptions(float r, float g, float b) implements ParticleOptions {

    public static final Codec<AuraNoteParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("r").forGetter(AuraNoteParticleOptions::r),
                    Codec.FLOAT.fieldOf("g").forGetter(AuraNoteParticleOptions::g),
                    Codec.FLOAT.fieldOf("b").forGetter(AuraNoteParticleOptions::b)
            ).apply(instance, AuraNoteParticleOptions::new)
    );

    @SuppressWarnings("deprecation")
    public static final Deserializer<AuraNoteParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public AuraNoteParticleOptions fromCommand(ParticleType<AuraNoteParticleOptions> type, StringReader reader)
                throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            return new AuraNoteParticleOptions(r, g, b);
        }

        @Override
        public AuraNoteParticleOptions fromNetwork(ParticleType<AuraNoteParticleOptions> type, FriendlyByteBuf buf) {
            return new AuraNoteParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return EIParticleTypes.AURA_NOTE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    @Override
    public String writeToString() {
        return String.format("%s %.2f %.2f %.2f",
                ForgeRegistries.PARTICLE_TYPES.getKey(getType()), r, g, b);
    }
}
