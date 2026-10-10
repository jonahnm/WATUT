package com.corosus.watut.particle;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class ParticleRotating extends SingleQuadParticle {

    public boolean useCustomRotation = true;
    public float prevRotationYaw;
    public float rotationYaw;
    public float prevRotationPitch;
    public float rotationPitch;
    public float prevRotationRoll;
    public float rotationRoll;
    public float brightness = 1F;

    //removes particle once hits 0, other things should reset this to keep it spawned
    public int despawnCountdown = 40;

    public static SingleQuadParticle.Layer layerForTexture(Identifier texture) {
        return new SingleQuadParticle.Layer(true, texture, RenderPipelines.TRANSLUCENT_PARTICLE);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        if (this.sprite == null) {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
        return SingleQuadParticle.Layer.bySprite(this.sprite);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    public void tick() {
        despawnCountdown--;
        if (despawnCountdown <= 0) {
            remove();
        }
    }

    public float getColorRed() {
        return rCol;
    }

    public float getColorGreen() {
        return gCol;
    }

    public float getColorBlue() {
        return bCol;
    }

    public void keepAlive() {
        despawnCountdown = 40;
    }

    public ParticleRotating(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ, null);
    }

    public void setQuadSize(float size) {
        this.quadSize = size;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
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
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightCoords(partialTick);
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);

        particleTypeRenderState.add(this.getLayer(), f, f1, f2,
                quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                f3, u0, u1, v0, v1, color, light);
    }

    public void setPosPrev(double pX, double pY, double pZ) {
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }
}
