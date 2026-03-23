package com.crims.effectiveinstruments.client.particle;

import com.crims.effectiveinstruments.config.EIClientConfig;
import com.crims.effectiveinstruments.particle.AuraNoteParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class AuraNoteParticleProvider implements ParticleProvider<AuraNoteParticleOptions> {
    private final SpriteSet spriteSet;

    public AuraNoteParticleProvider(SpriteSet spriteSet) {
        this.spriteSet = spriteSet;
    }

    @Nullable
    @Override
    public Particle createParticle(AuraNoteParticleOptions options, ClientLevel level,
                                    double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed) {
        EIClientConfig.ParticlesMode mode = EIClientConfig.PARTICLES_MODE.get();
        if (mode == EIClientConfig.ParticlesMode.NONE) {
            return null;
        }

        AuraNoteParticle particle = new AuraNoteParticle(
                level, x, y, z, options.r(), options.g(), options.b(), spriteSet
        );

        if (mode == EIClientConfig.ParticlesMode.MINIMAL) {
            particle.setLifetime(particle.getLifetime() / 2);
        }

        return particle;
    }
}
