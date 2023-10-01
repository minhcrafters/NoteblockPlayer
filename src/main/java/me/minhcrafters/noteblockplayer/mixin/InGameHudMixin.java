package me.minhcrafters.noteblockplayer.mixin;

import me.minhcrafters.noteblockplayer.utils.ProgressDisplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    private int scaledWidth;

    @Shadow
    private int scaledHeight;

    @Shadow
    private int heldItemTooltipFade;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V", ordinal = 3))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        ProgressDisplay.getInstance().onRenderHUD(context, scaledWidth, scaledHeight, heldItemTooltipFade);
    }
}
