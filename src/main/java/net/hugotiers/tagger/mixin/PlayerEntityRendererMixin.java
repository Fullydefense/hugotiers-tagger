package net.hugotiers.tagger.mixin;

import net.hugotiers.tagger.HugoTiersClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Decorating the exact vanilla label path means the tier appears only when vanilla draws the
 * nametag. renderLabelIfPresent runs solely when the label is visible (state.displayName != null),
 * so invisibility, sneaking, F1 and team rules are all inherited — the tier can never be seen
 * where the nametag itself is hidden.
 *
 * Since Minecraft 1.21.2 the nametag text is the render-state field displayName rather than a
 * method parameter; we resolve the player's UUID from the render state's entity id and rewrite
 * that field in place before it is rendered.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void hugotiers$decorateLabel(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        Text original = state.displayName;
        if (original == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Entity entity = client.world.getEntityById(state.id);
        if (entity == null) {
            return;
        }
        Text decorated = HugoTiersClient.decorateLabel(entity.getUuid(), original);
        if (decorated != original) {
            state.displayName = decorated;
        }
    }
}
