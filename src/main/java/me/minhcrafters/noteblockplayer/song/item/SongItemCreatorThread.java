package me.minhcrafters.noteblockplayer.song.item;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.converter.MusicDiscConverter;
import me.minhcrafters.noteblockplayer.song.SongLoaderThread;
import me.minhcrafters.noteblockplayer.utils.SongItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.io.IOException;

public class SongItemCreatorThread extends SongLoaderThread {
    public final int slotId;
    public final ItemStack stack;

    public SongItemCreatorThread(String location) throws IOException {
        super(location);
        this.slotId = NoteblockPlayer.mc.player.getInventory().selectedSlot;
        this.stack = NoteblockPlayer.mc.player.getInventory().getStack(slotId);
    }

    @Override
    public void run() {
        super.run();
        byte[] songData;
        try {
            songData = MusicDiscConverter.getBytesFromSong(song);
        } catch (IOException e) {
            NoteblockPlayer.addChatMessage("§cError creating music sheet: §4" + e.getMessage());
            return;
        }
        NoteblockPlayer.mc.execute(() -> {
            if (NoteblockPlayer.mc.world == null) {
                return;
            }
            if (!NoteblockPlayer.mc.player.getInventory().getStack(slotId).equals(stack)) {
                NoteblockPlayer.addChatMessage("§cCould not create music sheet because item has moved");
            }
            ItemStack newStack;
            if (stack.isEmpty()) {
                newStack = Items.PAPER.getDefaultStack();
                newStack.getOrCreateNbt().putInt("CustomModelData", 751642938);
            } else {
                newStack = stack.copy();
            }
            newStack = SongItemUtils.createSongItem(newStack, songData, filename, song.name);
            NoteblockPlayer.mc.player.getInventory().setStack(slotId, newStack);
            NoteblockPlayer.mc.interactionManager.clickCreativeStack(NoteblockPlayer.mc.player.getStackInHand(Hand.MAIN_HAND), 36 + slotId);
            NoteblockPlayer.addChatMessage(Text.literal("§6Successfully created a new music sheet."));
        });
    }
}
