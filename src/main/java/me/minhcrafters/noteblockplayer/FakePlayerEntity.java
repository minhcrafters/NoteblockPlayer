package me.minhcrafters.noteblockplayer;

import com.mojang.authlib.GameProfile;
import me.minhcrafters.noteblockplayer.song.SongManager;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    ClientPlayerEntity player = NoteblockPlayer.mc.player;
    ClientWorld world = NoteblockPlayer.mc.world;

    public FakePlayerEntity() {
        super(NoteblockPlayer.mc.world, new GameProfile(UUID.randomUUID(), NoteblockPlayer.mc.player.getGameProfile().getName()));

        copyStagePosAndPlayerLook();

        getInventory().clone(player.getInventory());

        Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);

        headYaw = player.headYaw;
        bodyYaw = player.bodyYaw;

        if (player.isSneaking()) {
            setSneaking(true);
            setPose(EntityPose.CROUCHING);
        }

        capeX = getX();
        capeY = getY();
        capeZ = getZ();

        world.addEntity(getId(), this);
    }

    public void resetPlayerPosition() {
        player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public void copyStagePosAndPlayerLook() {
        Stage stage = SongManager.getInstance().stage;
        if (stage != null) {
            refreshPositionAndAngles(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, getYaw(), getPitch());
            headYaw = player.headYaw;
            // bodyYaw = player.bodyYaw;
        } else {
            copyPositionAndRotation(player);
        }
    }
}
