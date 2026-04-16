package com.crims.effectiveinstruments.client.particle;

import com.crims.effectiveinstruments.config.EIClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AuraNoteParticle extends TextureSheetParticle {
    private static final float BASE_ALPHA = 0.7f;
    private static final int FADE_IN_TICKS = 8;
    private static final int FADE_OUT_TICKS = 10;

    protected AuraNoteParticle(ClientLevel level, double x, double y, double z,
                                float r, float g, float b, SpriteSet spriteSet) {
        super(level, x, y, z);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.lifetime = 40 + random.nextInt(20);
        this.gravity = -0.01f; // slow upward drift
        this.alpha = 0f;
        this.quadSize = 0.15f + random.nextFloat() * 0.1f;
        this.xd = 0;
        this.yd = 0.01;
        this.zd = 0;
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Fade in / hold / fade out
        if (age < FADE_IN_TICKS) {
            alpha = BASE_ALPHA * ((float) age / FADE_IN_TICKS);
        } else if (age > lifetime - FADE_OUT_TICKS) {
            alpha = BASE_ALPHA * ((float) (lifetime - age) / FADE_OUT_TICKS);
        } else {
            alpha = BASE_ALPHA;
        }

        // Gentle sinusoidal horizontal drift (halved when reduced motion is on)
        double driftScale = EIClientConfig.REDUCED_MOTION.get() ? 0.001 : 0.002;
        this.xd += Math.sin(age * 0.15) * driftScale;
        this.zd += Math.cos(age * 0.15 + 1.0) * driftScale;

        this.move(this.xd, this.yd, this.zd);

        // Dampen horizontal drift so it doesn't accumulate too much
        this.xd *= 0.92;
        this.zd *= 0.92;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        // Subtle pulse — halved when reduced motion is on
        float pulseAmplitude = EIClientConfig.REDUCED_MOTION.get() ? 0.05f : 0.1f;
        float pulse = 1.0f + pulseAmplitude * (float) Math.sin(age * 0.2 + partialTick * 0.2);
        return this.quadSize * pulse;
    }
}
