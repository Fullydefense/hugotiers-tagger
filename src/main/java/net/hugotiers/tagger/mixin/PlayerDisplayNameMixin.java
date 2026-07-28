package net.hugotiers.tagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.loader.api.FabricLoader;
import net.hugotiers.tagger.HugoTiersClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fallback for third-party clients that replace the vanilla nametag renderer.
 *
 * <p>Lunar Client draws player nametags with its own pipeline instead of Minecraft's, reading the
 * text from {@link PlayerEntity#getDisplayName()}. Our normal hook sits in the vanilla label render
 * path, so on Lunar it runs but nothing it produces is ever drawn — the mod loads, shows up in the
 * mod list, logs no error, and displays nothing at all.
 *
 * <p>Decorating {@code getDisplayName} instead puts the badge where those clients actually look.
 * It is deliberately gated on the Lunar module ("ichor") being present, because on vanilla Fabric
 * this method also feeds chat messages, the tab list and death messages — places the badge does not
 * belong. {@link HugoTiersClient#decorateLabel} additionally refuses to decorate twice, so if a
 * client uses both paths the badge still appears exactly once.
 *
 * <p>Known limitation: the animated rainbow outline is drawn by our own quads inside the vanilla
 * label renderer. On a client that bypasses that renderer, the badge returns but the outline does
 * not.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDisplayNameMixin {
    // Resolved once: getDisplayName is called very frequently.
    private static final boolean HUGOTIERS$THIRD_PARTY_NAMETAGS =
            FabricLoader.getInstance().isModLoaded("ichor");

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text hugotiers$decorateDisplayName(Text original) {
        if (!HUGOTIERS$THIRD_PARTY_NAMETAGS) {
            return original;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        return HugoTiersClient.decorateLabel(self.getUuid(), original);
    }
}
