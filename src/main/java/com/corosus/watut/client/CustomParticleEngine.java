package com.corosus.watut.client;

import com.corosus.watut.particle.ParticleRotating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Thin wrapper over the vanilla {@link net.minecraft.client.particle.ParticleEngine}.
 *
 * <p>MC 26.2 completely rewrote particle rendering into an extract/submit pipeline
 * (particles now describe their billboard data via {@code extract} and are drawn by
 * {@code QuadParticleFeatureRenderer}). Because of that, the previous custom engine
 * (which existed to work around Oculus performance and per-player render types) no
 * longer maps to the engine architecture, so WATUT particles are now routed through
 * the vanilla engine.
 */
public class CustomParticleEngine {

    public CustomParticleEngine(ClientLevel level, Object textureManager) {
    }

    public void add(ParticleRotating particle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.particleEngine != null) {
            mc.particleEngine.add(particle);
        }
    }

    public void tick() {
        // vanilla ParticleEngine ticks its own particles, nothing to do here
    }

    public void setLevel(ClientLevel level) {
        // vanilla ParticleEngine manages its own level
    }

    public String countParticles() {
        return "0";
    }
}
