package me.minhcrafters.noteblockplayer.mixin;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongManager;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(at = @At("HEAD"), method = "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V", cancellable = true)
    public void onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        if (NoteblockPlayer.getConfig().blockStateDebug) {
            Stage stage = SongManager.getInstance().stage;
            if (stage != null && !SongManager.getInstance().building) {
                for (BlockPos nbp : stage.noteblockPositions.values()) {
                    if (nbp.equals(pos)) {
                        BlockState oldState = NoteblockPlayer.mc.world.getBlockState(pos);
                        if (oldState.equals(state))
                            return;
                        NoteblockPlayer.addChatMessage(Text.of(String.format("§7Block in stage changed from §2%s §7to §2%s", oldState, state.toString())));
                        break;
                    }
                }
            }
        }
    }
}
