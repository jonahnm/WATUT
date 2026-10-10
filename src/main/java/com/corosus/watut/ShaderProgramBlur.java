package com.corosus.watut;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * MC 26.2 replaced the old ShaderProgram/CompiledShaderProgram/AbstractUniform system with
 * RenderPipeline + bind groups. This wraps a RenderPipeline built from the mod's
 * .vsh/.fsh pair, plus a std140 uniform buffer for the blur uniforms
 * (resolution, radius, blurLevel).
 */
public class ShaderProgramBlur {

    public static final BindGroupLayout BLUR_INFO_LAYOUT =
            BindGroupLayout.builder()
                    .withUniform("WatutBlurInfo", UniformType.UNIFORM_BUFFER)
                    .build();

    public static final int BLUR_INFO_SIZE = new Std140SizeCalculator().putVec2().putFloat().putFloat().get();

    private final RenderPipeline pipeline;

    //cached uniform values written to BLUR_INFO UBO
    public float resolutionX = 1;
    public float resolutionY = 1;
    public float radius = 0;
    public float blurLevel = 0;

    private GpuBuffer blurInfoBuffer;

    public ShaderProgramBlur(String resourcePath) {
        this.pipeline = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(WatutMod.MODID, "pipeline/" + resourcePath.replace('/', '_')))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BLUR_INFO_LAYOUT)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexShader(Identifier.fromNamespaceAndPath(WatutMod.MODID, resourcePath))
                .withFragmentShader(Identifier.fromNamespaceAndPath(WatutMod.MODID, resourcePath))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(java.util.Optional.empty())
                .build();
    }

    public RenderPipeline getPipeline() {
        return pipeline;
    }

    public void markUniformsNeedUpdate() {
    }

    public GpuBufferSlice getBlurInfoBufferSlice() {
        if (blurInfoBuffer == null) {
            blurInfoBuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "Watut blur info", GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM, BLUR_INFO_SIZE);
        }
        try (GpuBufferSlice.MappedView mapped = blurInfoBuffer.map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
            builder.putVec2(resolutionX, resolutionY);
            builder.putFloat(radius);
            builder.putFloat(blurLevel);
        }
        return blurInfoBuffer.slice();
    }

    public void setResolution(float x, float y) {
        this.resolutionX = x;
        this.resolutionY = y;
    }

    public void setRadius(float radius) {
        this.radius = Mth.clamp(radius, 0.0F, 128.0F);
    }

    public void setBlurLevel(float blurLevel) {
        this.blurLevel = blurLevel;
    }
}
