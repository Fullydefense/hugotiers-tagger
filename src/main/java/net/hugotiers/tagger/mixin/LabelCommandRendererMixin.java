package net.hugotiers.tagger.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.hugotiers.tagger.HugoTiersClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * After each nametag label is drawn we add a thin animated rainbow rectangle outline around just the
 * tier badge (icon + tier), for labels that carry our badge. We reuse the exact render layer MC uses
 * for nametag backgrounds (RenderLayers.textBackgroundSeeThrough) and the label's own matrix, so the
 * frame sits perfectly on the tag and is only drawn when the label itself is drawn.
 */
@Mixin(LabelCommandRenderer.class)
public class LabelCommandRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void hugotiers$drawBadgeFrame(BatchingRenderCommandQueue queue,
                                          VertexConsumerProvider.Immediate immediate,
                                          TextRenderer textRenderer,
                                          CallbackInfo ci,
                                          @Local OrderedRenderCommandQueueImpl.LabelCommand command) {
        HugoTiersClient.renderBadgeFrame(command, immediate, textRenderer);
    }
}
