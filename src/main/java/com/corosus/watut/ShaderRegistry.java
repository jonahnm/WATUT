package com.corosus.watut;

import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public class ShaderRegistry {

    public static ShaderRegistry instance;

    public static void init() {

        PlayerStatusManagerClient.particle = new ShaderProgramBlur("core/particle");
        PlayerStatusManagerClient.positionTexBlur = new ShaderProgramBlur("core/position_tex_blur");
        PlayerStatusManagerClient.positionTexBlurHorizontal = new ShaderProgramBlur("core/position_tex_blur_horizontal");
        PlayerStatusManagerClient.positionTexBlurVertical = new ShaderProgramBlur("core/position_tex_blur_vertical");

    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PlayerStatusManagerClient.positionTexBlur.getPipeline());
        event.registerPipeline(PlayerStatusManagerClient.positionTexBlurHorizontal.getPipeline());
        event.registerPipeline(PlayerStatusManagerClient.positionTexBlurVertical.getPipeline());
    }

    public static void reloadShaders() {
    }

}
