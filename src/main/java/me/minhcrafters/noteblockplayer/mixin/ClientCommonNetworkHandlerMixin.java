package me.minhcrafters.noteblockplayer.mixin;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Mutable
    @Final
    @Shadow
    protected final ClientConnection connection;

    public ClientCommonNetworkHandlerMixin() {
        connection = null;
    }

    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        Stage lastStage = SongHandler.getInstance().lastStage;

        if (!SongHandler.getInstance().isIdle() && packet instanceof PlayerMoveC2SPacket) {
            if (lastStage != null) {
                if (!NoteblockPlayer.getConfig().rotate) {
                    connection.send(new PlayerMoveC2SPacket.Full(
                            lastStage.position.getX() + 0.5, lastStage.position.getY(), lastStage.position.getZ() + 0.5,
                            NoteblockPlayer.mc.player.getYaw(), NoteblockPlayer.mc.player.getPitch(),
                            true, false));
                    if (NoteblockPlayer.fakePlayer != null) {
                        NoteblockPlayer.fakePlayer.copyStagePosAndPlayerLook();
                    }
                }
            }
            ci.cancel();
        }
        else if (packet instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket.Mode mode = ((ClientCommandC2SPacket) packet).getMode();
            if (NoteblockPlayer.fakePlayer != null) {
                if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
                    NoteblockPlayer.fakePlayer.setSneaking(true);
                    NoteblockPlayer.fakePlayer.setPose(EntityPose.CROUCHING);
                }
                else if (mode == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
                    NoteblockPlayer.fakePlayer.setSneaking(false);
                    NoteblockPlayer.fakePlayer.setPose(EntityPose.STANDING);
                }
            }
        }
    }
}
