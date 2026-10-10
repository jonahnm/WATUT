package com.corosus.watut.particle;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class ParticleDynamic extends ParticleRotating {

    public SingleQuadParticle.Layer dynamicLayer;

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return dynamicLayer;
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, SingleQuadParticle.Layer layer) {
        this(pLevel, pX, pY, pZ, layer, 1F);
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, SingleQuadParticle.Layer layer, float brightness) {
        super(pLevel, pX, pY, pZ);
        this.dynamicLayer = layer;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTick) {
        Vec3 vec3 = camera.position();
        float f = (float)(Mth.lerp(partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp(partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp(partialTick, this.zo, this.z) - vec3.z());
        Quaternionf quaternion;
        if (useCustomRotation) {
            quaternion = new Quaternionf(0, 0, 0, 1);
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTick, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTick, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, this.prevRotationRoll, rotationRoll)));
        } else {
            if (this.roll == 0.0F) {
                quaternion = camera.rotation();
            } else {
                quaternion = new Quaternionf(camera.rotation());
                quaternion.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
            }
        }

        float f3 = this.getQuadSize(partialTick);
        int light = this.getLightCoords(partialTick);
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);

        //note: aspect ratio handling of the full screen quad is done via the layer texture itself
        particleTypeRenderState.add(this.getLayer(), f, f1, f2,
                quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                f3, 0.0F, 1.0F, 0.0F, 1.0F, color, light);
    }
}
