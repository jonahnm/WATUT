package com.corosus.watut.particle;

import com.corosus.coroutil.util.CULog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

public class ParticleItem extends ParticleRotating {

    public static HashSet<String> itemBlacklist = new HashSet<>();

    public ItemStack itemStack;
    public float xFrom;
    public float yFrom;
    public float zFrom;
    public float xTo;
    public float yTo;
    public float zTo;

    private final SingleQuadParticle.Layer spriteLayer;

    public ParticleItem(ClientLevel pLevel, float brightness, ItemStack itemStack, float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo) {
        super(pLevel, xFrom, yFrom, zFrom);
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 1F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.xFrom = xFrom;
        this.yFrom = yFrom;
        this.zFrom = zFrom;
        this.xTo = xTo;
        this.yTo = yTo;
        this.zTo = zTo;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);

        this.itemStack = itemStack;

        ItemStackRenderState scratchItemStackRenderState = new ItemStackRenderState();
        TextureAtlasSprite sprite;
        try {
            Minecraft.getInstance()
                    .getItemModelResolver()
                    .updateForTopItem(scratchItemStackRenderState, itemStack, ItemDisplayContext.GROUND, pLevel, null, 0);
            Material.Baked material = scratchItemStackRenderState.pickParticleMaterial(this.random);
            sprite = material != null ? material.sprite() : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        } catch (Exception exception) {
            CULog.err("ERROR, exception resolving item particle sprite: " + itemStack.getItem().toString() + " - adding to ParticleItem render blacklist for this minecraft session");
            itemBlacklist.add(itemStack.getItem().toString());
            sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        }
        this.sprite = sprite;
        this.spriteLayer = SingleQuadParticle.Layer.bySprite(sprite);

        this.rotationYaw = pLevel.getRandom().nextFloat() * 360;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return this.spriteLayer;
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= 6) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override
    public void extract(net.minecraft.client.renderer.state.level.QuadParticleRenderState state, net.minecraft.client.Camera camera, float partialTick) {
        if (itemBlacklist.contains(this.itemStack.getItem().toString())) return;

        Vec3 vec3 = camera.position();
        float f = (float)(Mth.lerp(partialTick, this.xo, this.x));
        float f1 = (float)(Mth.lerp(partialTick, this.yo, this.y));
        float f2 = (float)(Mth.lerp(partialTick, this.zo, this.z));

        float lerp = ((float)this.age + partialTick) / 3.0F;
        double x = Mth.lerp(lerp, f, xTo);
        double y = Mth.lerp(lerp, f1, yTo);
        double z = Mth.lerp(lerp, f2, zTo);

        if (this.age >= 3) {
            x = xTo;
            y = yTo;
            z = zTo;
        }

        x = x - vec3.x();
        y = y - vec3.y() - 0.15;
        z = z - vec3.z();

        int light = this.getLightCoords(partialTick);
        int color = net.minecraft.util.ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);

        org.joml.Quaternionf quaternion = new org.joml.Quaternionf(0, 0, 0, 1);
        quaternion.mul(com.mojang.math.Axis.YP.rotationDegrees(this.rotationYaw));

        state.add(this.getLayer(), (float)x, (float)y, (float)z,
                quaternion.x, quaternion.y, quaternion.z, quaternion.w,
                this.quadSize, this.getU0(), this.getU1(), this.getV0(), this.getV1(), color, light);
    }

}
