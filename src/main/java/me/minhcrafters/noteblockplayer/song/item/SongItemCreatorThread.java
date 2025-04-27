package me.minhcrafters.noteblockplayer.song.item;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.conversion.SPConverter;
import me.minhcrafters.noteblockplayer.song.SongLoaderThread;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.io.IOException;
import java.util.List;

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
            songData = SPConverter.getBytesFromSong(song);
        } catch (IOException e) {
            NoteblockPlayer.addChatMessage("§cError creating song item: §4" + e.getMessage());
            return;
        }
        NoteblockPlayer.mc.execute(() -> {
            if (NoteblockPlayer.mc.world == null) {
                return;
            }
            if (!NoteblockPlayer.mc.player.getInventory().getStack(slotId).equals(stack)) {
                NoteblockPlayer.addChatMessage("§cCould not create song item because item has moved");
            }
            ItemStack newStack;
            if (stack.isEmpty()) {
                newStack = Items.PAPER.getDefaultStack();
                // When going from 1.21.3 -> 1.21.4, datafixer changes the custom model data to a float array with one element
                newStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(751642938f), List.of(), List.of(), List.of()));
            }
            else {
                newStack = stack.copy();
            }
            newStack = SongItemUtils.createSongItem(newStack, songData, filename, song.name);
            NoteblockPlayer.mc.player.getInventory().setStack(slotId, newStack);
            NoteblockPlayer.mc.interactionManager.clickCreativeStack(NoteblockPlayer.mc.player.getStackInHand(Hand.MAIN_HAND), 36 + slotId);
            NoteblockPlayer.addChatMessage(Text.literal("§6Successfully assigned song data to §3").append(newStack.getItem().getName()));
        });
    }
}
