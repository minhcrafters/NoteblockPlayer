package me.minhcrafters.noteblockplayer.mixin;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private final ClientConnection connection;

    public ClientPlayNetworkHandlerMixin() {
        connection = null;
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage(Ljava/lang/String;)V", cancellable = true)
    private void onSendChatMessage(String content, CallbackInfo ci) {
        boolean isCommand = CommandManager.processChatMessage(content);
        if (isCommand) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        Stage stage = SongManager.getInstance().stage;
        if (stage != null && packet instanceof PlayerMoveC2SPacket) {
            if (!NoteblockPlayer.getConfig().bodyRotate) {
                connection.send(new PlayerMoveC2SPacket.PositionAndOnGround(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, true));
            } else {
                connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, NoteblockPlayer.mc.player.getYaw(), NoteblockPlayer.mc.player.getPitch(), true));
                if (NoteblockPlayer.fakePlayer != null) {
                    NoteblockPlayer.fakePlayer.copyStagePosAndPlayerLook();
                }
            }
            ci.cancel();
        } else if (packet instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket.Mode mode = ((ClientCommandC2SPacket) packet).getMode();
            if (NoteblockPlayer.fakePlayer != null) {
                if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
                    NoteblockPlayer.fakePlayer.setSneaking(true);
                    NoteblockPlayer.fakePlayer.setPose(EntityPose.CROUCHING);
                } else if (mode == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
                    NoteblockPlayer.fakePlayer.setSneaking(false);
                    NoteblockPlayer.fakePlayer.setPose(EntityPose.STANDING);
                }
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
    public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        SongManager.getInstance().cleanup();
    }

    @Inject(at = @At("TAIL"), method = "onPlayerRespawn(Lnet/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket;)V")
    public void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        SongManager.getInstance().cleanup();
    }

    @Inject(at = @At("TAIL"), method = "onPlayerAbilities(Lnet/minecraft/network/packet/s2c/play/PlayerAbilitiesS2CPacket;)V")
    public void onOnPlayerAbilities(PlayerAbilitiesS2CPacket packet, CallbackInfo ci) {
        SongManager songManager = SongManager.getInstance();
        if (songManager.currentSong != null || !songManager.songQueue.isEmpty()) {
            NoteblockPlayer.mc.player.getAbilities().flying = songManager.wasFlying;
        }
    }
}
