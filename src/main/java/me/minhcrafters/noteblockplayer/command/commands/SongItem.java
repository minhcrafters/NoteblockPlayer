package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.item.SongItemCreatorThread;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import me.minhcrafters.noteblockplayer.utils.SongItemUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SongItem extends Command {
    public String getName() {
        return "songitem";
    }

    public String[] getAliases() {
        return new String[]{"item"};
    }

    public String[] getSyntax() {
        return new String[]{
                CommandManager.getCommandPrefix() + "create <song or url>",
                CommandManager.getCommandPrefix() + "setsongname <name>"
        };
    }

    public String getDescription() {
        return "Assigns/edits song data for the item in your hand";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            return false;
        }

        if (NoteblockPlayer.mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            NoteblockPlayer.addChatMessage(Text.of("§cYou must be in creative mode to use this command."));
            return true;
        }

        ItemStack stack = NoteblockPlayer.mc.player.getMainHandStack();
        NbtCompound NoteblockPlayerNBT = SongItemUtils.getSongItemTag(stack);

        String[] split = args.split(" ");

        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (split.length != 2) return false;
                try {
                    (new SongItemCreatorThread(split[1])).start();
                } catch (IOException e) {
                    NoteblockPlayer.addChatMessage("§cError: §4" + e.getMessage());
                }
                return true;
            }
            case "setsongname" -> {
                if (split.length < 2) return false;
                if (NoteblockPlayerNBT == null) {
                    NoteblockPlayer.addChatMessage("§cYou must be holding a song item");
                    return true;
                }
                String name = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                NoteblockPlayerNBT.putString(SongItemUtils.DISPLAY_NAME_KEY, name);
                NoteblockPlayer.mc.player.setStackInHand(Hand.MAIN_HAND, stack);
                SongItemUtils.addSongItemDisplay(stack);
                NoteblockPlayer.mc.interactionManager.clickCreativeStack(NoteblockPlayer.mc.player.getStackInHand(Hand.MAIN_HAND), 36 + NoteblockPlayer.mc.player.getInventory().selectedSlot);
                NoteblockPlayer.addChatMessage("§6Successfully set item's display name to §3" + name);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);
        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[]{
                    "create",
                    "setsongnme",
            }, suggestionsBuilder);
        }
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (split.length == 2) {
                    return FileUtils.giveSongSuggestions(split[1], suggestionsBuilder);
                }
            case "setsongname":
            default:
                return null;
        }
    }
}
