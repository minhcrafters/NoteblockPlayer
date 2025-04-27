package me.minhcrafters.noteblockplayer;

import me.minhcrafters.noteblockplayer.mixin.accessor.ClientPlayNetworkHandlerAccessor;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.stage.Stage;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    public static final UUID FAKE_PLAYER_UUID = UUID.randomUUID();

    ClientPlayerEntity player = NoteblockPlayer.mc.player;
    ClientWorld world = NoteblockPlayer.mc.world;

    public FakePlayerEntity() {
        super(NoteblockPlayer.mc.world, getProfile());

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

        world.addEntity(this);
    }

    public void resetPlayerPosition() {
        player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public void copyStagePosAndPlayerLook() {
        Stage lastStage = SongHandler.getInstance().lastStage;
        if (lastStage != null) {
            refreshPositionAndAngles(lastStage.position.getX() + 0.5, lastStage.position.getY(), lastStage.position.getZ() + 0.5, player.getYaw(), player.getPitch());
            headYaw = player.headYaw;
        } else {
            copyPositionAndRotation(player);
        }
    }

    private static GameProfile getProfile() {
        GameProfile profile = new GameProfile(FAKE_PLAYER_UUID, NoteblockPlayer.mc.player.getGameProfile().getName());
        profile.getProperties().putAll(NoteblockPlayer.mc.player.getGameProfile().getProperties());
        PlayerListEntry playerListEntry = new PlayerListEntry(NoteblockPlayer.mc.player.getGameProfile(), false);
        ((ClientPlayNetworkHandlerAccessor) NoteblockPlayer.mc.getNetworkHandler()).getPlayerListEntries().put(FAKE_PLAYER_UUID, playerListEntry);
        return profile;
    }
}
