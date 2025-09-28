package me.minhcrafters.noteblockplayer.mixin;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Used for debugging purposes

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(at = @At("HEAD"), method = "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V")
    public void onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        Stage stage = SongHandler.getInstance().stage;
        if (stage != null && !SongHandler.getInstance().building) {
            for (BlockPos nbp : stage.noteblockPositions.values()) {
                if (nbp.equals(pos)) {
                    BlockState oldState = NoteblockPlayer.mc.world.getBlockState(pos);
                    if (oldState.equals(state))
                        return;
                    NoteblockPlayer.addChatMessage(String.format("§7Block in stage changed from §2%s §7to §2%s",
                            oldState.toString(), state.toString()));
                    break;
                }
            }
        }
    }
}
