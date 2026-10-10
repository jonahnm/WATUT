package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Captures the active GUI so it can be shown to other players as a billboard particle.
 *
 * <p>MC 26.2 rewrote rendering into an extract/submit pipeline: the screen is no longer drawn
 * immediately to a bound framebuffer, so the old approach (re-render the screen into a custom
 * MainTarget, then blur-downscale with immediate-mode draws) is no longer possible. Instead,
 * after the GuiRenderer has drawn the GUI to the main render target, we sample the main
 * target in a custom render pass into a scaled-down TextureTarget (applying the blur shader),
 * then copy the result back to the CPU via copyTextureToBuffer for compression and network
 * upload.
 */
public class ScreenParticleRenderer {

    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    //used to render a sized down and cropped version of the gui captured from the main render target
    private TextureTarget mainRenderTargetScaledDown;

    public int width;
    public int height;
    public static int defaultWidthScaledDown = 256;
    public static int defaultHeightScaledDown = 256;
    public static int bytesPerPixel = 4;
    public int widthScaledDown = defaultWidthScaledDown;
    public int heightScaledDown = defaultHeightScaledDown;
    public boolean needsInit = true;

    private GpuBuffer quadVertexBuffer;
    private GpuBuffer quadIndexBuffer;
    private int quadIndexCount;
    private final net.minecraft.client.renderer.ProjectionMatrixBuffer projectionMatrixBuffer = new net.minecraft.client.renderer.ProjectionMatrixBuffer("watut_capture");

    private static ScreenParticleRenderer instance;

    public static ScreenParticleRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenParticleRenderer();
        }
        return instance;
    }

    public void checkSetup() {
        if (needsInit) {
            needsInit = false;
            setup();
        }
    }

    public void setup() {
        Minecraft mc = Minecraft.getInstance();
        width = mc.getWindow().getWidth();
        height = mc.getWindow().getHeight();

        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthScaledDown = width;
            heightScaledDown = height;
        } else {
            widthScaledDown = defaultWidthScaledDown;
            heightScaledDown = defaultHeightScaledDown;
        }

        recreateScaledDownTarget();
    }

    private void recreateScaledDownTarget() {
        if (mainRenderTargetScaledDown != null) {
            mainRenderTargetScaledDown.destroyBuffers();
        }
        mainRenderTargetScaledDown = new TextureTarget("watut_gui_capture", widthScaledDown, heightScaledDown, false, GpuFormat.RGBA8_UNORM);
        setupQuadGeometry();
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        recreateScaledDownTarget();
    }

    public void resizeScaledDown(int width, int height) {
        checkSetup();
        int widthToUse = defaultWidthScaledDown;
        int heightToUse = defaultHeightScaledDown;
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthToUse = width;
            heightToUse = height;
        }

        widthScaledDown = widthToUse;
        heightScaledDown = heightToUse;

        CULog.dbg("resizeScaledDown to " + widthToUse + " " + heightToUse);

        if (mainRenderTargetScaledDown == null || mainRenderTargetScaledDown.width != widthToUse || mainRenderTargetScaledDown.height != heightToUse) {
            recreateScaledDownTarget();
        }
    }

    public TextureTarget getMainRenderTargetScaledDown() {
        return mainRenderTargetScaledDown;
    }

    private void setupQuadGeometry() {
        if (widthScaledDown <= 0 || heightScaledDown <= 0) {
            return;
        }
        GpuBuffer oldVertex = quadVertexBuffer;
        GpuBuffer oldIndex = quadIndexBuffer;
        quadVertexBuffer = null;
        quadIndexBuffer = null;
        if (oldVertex != null) oldVertex.close();
        if (oldIndex != null) oldIndex.close();

        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
            addQuadVertices(bufferBuilder, 0, 0, widthScaledDown, heightScaledDown, 0, 1, 0, 1);
            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                quadVertexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Watut capture quad vertices",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST,
                        meshData.vertexBuffer().remaining());
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(quadVertexBuffer.slice(), meshData.vertexBuffer());
                ByteBuffer indexBytes = meshData.indexBuffer();
                quadIndexCount = meshData.drawState().indexCount();
                if (indexBytes != null) {
                    quadIndexBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "Watut capture quad indices",
                            GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST,
                            indexBytes.remaining());
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(quadIndexBuffer.slice(), indexBytes);
                }
            }
        }
    }

    private static void addQuadVertices(BufferBuilder bufferBuilder, int x0, int y0, int x1, int y1, float minU, float maxU, float minV, float maxV) {
        // Bottom-left vertex
        bufferBuilder.addVertex((float)x0, (float)y0, 0F).setUv(minU, maxV);
        // Top-left vertex
        bufferBuilder.addVertex((float)x0, (float)y1, 0F).setUv(minU, minV);
        // Top-right vertex
        bufferBuilder.addVertex((float)x1, (float)y1, 0F).setUv(maxU, minV);
        // Bottom-right vertex
        bufferBuilder.addVertex((float)x1, (float)y0, 0F).setUv(maxU, maxV);
    }

    /**
     * Samples the current gui from the main render target into the scaled down target,
     * applying the mod's blur shader, then reads the result back to the cpu.
     * Must be called on the render thread after GuiRenderer has drawn the gui.
     */
    public void captureAndReadback() {
        checkSetup();
        if (mainRenderTargetScaledDown == null || widthScaledDown <= 0 || heightScaledDown <= 0) {
            return;
        }
        if (quadVertexBuffer == null || quadIndexBuffer == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        if (mainTarget == null || mainTarget.getColorTextureView() == null) {
            return;
        }

        float radius;
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            radius = 0;
        } else {
            radius = ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel;
        }

        PlayerStatusManagerClient.positionTexBlur.setResolution(widthScaledDown, heightScaledDown);
        PlayerStatusManagerClient.positionTexBlur.setRadius(radius);
        PlayerStatusManagerClient.positionTexBlur.setBlurLevel(ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel);
        GpuBufferSlice blurInfo = PlayerStatusManagerClient.positionTexBlur.getBlurInfoBufferSlice();

        //ortho projection in scaled down gui pixel space
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, widthScaledDown, heightScaledDown, 0.0F, 1000.0F, 21000.0F);
        RenderSystem.setProjectionMatrix(projectionMatrixBuffer.getBuffer(ortho), ProjectionType.ORTHOGRAPHIC);

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Watut gui capture",
                mainRenderTargetScaledDown.getColorTextureView(),
                Optional.of(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F))
        )) {
            renderPass.setPipeline(PlayerStatusManagerClient.positionTexBlur.getPipeline());
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F)));
            renderPass.setUniform("WatutBlurInfo", blurInfo);
            renderPass.bindTexture("Sampler0", mainTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setVertexBuffer(0, quadVertexBuffer.slice());
            renderPass.setIndexBuffer(quadIndexBuffer, IndexType.SHORT);
            renderPass.drawIndexed(quadIndexCount, 1, 0, 0, 0);
        }

        //readback for compression + network upload
        GpuBuffer readBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Watut capture readback",
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
                (long) widthScaledDown * heightScaledDown * bytesPerPixel);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(
                mainRenderTargetScaledDown.getColorTexture(),
                readBuffer,
                0L,
                () -> {
                    try (GpuBufferSlice.MappedView read = readBuffer.map(true, false)) {
                        ByteBuffer data = read.data();
                        data.position(0);
                        data.limit(widthScaledDown * heightScaledDown * bytesPerPixel);
                        RenderHelper.processor.submitForProcessing(data);
                    } catch (Exception e) {
                        CULog.dbg("watut capture readback failed: " + e);
                    } finally {
                        readBuffer.close();
                    }
                },
                0);
    }
}
