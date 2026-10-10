package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2: the gui is no longer drawn immediately during Screen#render; it is extracted into
 * a GuiRenderState and later submitted to the main render target by GuiRenderer#render().
 * We hook the tail of that to capture the finished gui for the "what are they up to" feature.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererCapture {

    @Inject(method = "render", at = @At("TAIL"))
    private void render(CallbackInfo ci) {
        RenderHelper.captureScreen();
    }
}
