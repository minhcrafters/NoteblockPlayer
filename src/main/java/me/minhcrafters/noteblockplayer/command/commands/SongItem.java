package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.item.SongItemCreatorThread;
import me.minhcrafters.noteblockplayer.song.item.SongItemUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.mc;

public class SongItem extends Command {
    public String getName() {
        return "songItem";
    }

    public String[] getAliases() {
        return new String[] { "item" };
    }

    public String[] getSyntax() {
        return new String[] {
                "create <song or url>",
                "setSongName <name>",
        };
    }

    public String getDescription() {
        return "Assigns/edits song data for the item in your hand";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            return false;
        }

        if (mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            NoteblockPlayer.addChatMessage("§cYou must be in creative mode to use this command");
            return true;
        }

        ItemStack stack = mc.player.getMainHandStack();
        NbtCompound songPlayerNBT = SongItemUtils.getSongItemTag(stack);

        String[] split = args.split(" ");
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (split.length < 2)
                    return false;
                String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                try {
                    (new SongItemCreatorThread(location)).start();
                } catch (IOException e) {
                    NoteblockPlayer.addChatMessage("§cError creating song item: §4" + e.getMessage());
                }
                return true;
            case "setsongname":
                if (split.length < 2)
                    return false;
                if (songPlayerNBT == null) {
                    NoteblockPlayer.addChatMessage("§cYou must be holding a song item");
                    return true;
                }
                String name = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt
                        .getCompound(SongItemUtils.SONG_ITEM_KEY).putString(SongItemUtils.DISPLAY_NAME_KEY, name));
                SongItemUtils.addSongItemDisplay(stack);
                mc.player.setStackInHand(Hand.MAIN_HAND, stack);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND),
                        36 + mc.player.getInventory().selectedSlot);
                NoteblockPlayer.addChatMessage("§6Set song's display name to §3" + name);
                return true;
            default:
                return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);
        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[] {
                    "create",
                    "setSongName",
            }, suggestionsBuilder);
        }
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (split.length >= 2) {
                    String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                    return Utils.giveSongSuggestions(location, suggestionsBuilder);
                }
            case "setsongname":
            default:
                return null;
        }
    }
}
