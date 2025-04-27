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
    @Inject(at = @At("TAIL"), method = "renderHeldItemTooltip(Lnet/minecraft/client/gui/DrawContext;)V")
    private void onRenderHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        ProgressDisplay.getInstance().onRenderHUD(context);
    }
}
