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

public class MusicSheet extends Command {
    public String getName() {
        return "musicsheet";
    }

    public String[] getAliases() {
        return new String[]{"sheet"};
    }

    public String[] getSyntax() {
        return new String[]{
                CommandManager.getCommandPrefix() + "create <file or url>",
                CommandManager.getCommandPrefix() + "setname <name>"
        };
    }

    public String getDescription() {
        return "Creates a new music sheet containing a song or modifies an existing one";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
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
                if (split.length < 2) return false;
                String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                try {
                    (new SongItemCreatorThread(location)).start();
                } catch (IOException e) {
                    NoteblockPlayer.addChatMessage("§cError: §4" + e.getMessage());
                }
                return true;
            }
            case "setname" -> {
                if (split.length < 2) return false;
                if (NoteblockPlayerNBT == null) {
                    NoteblockPlayer.addChatMessage("§cYou must hold a music sheet in your main hand to modify it.");
                    return true;
                }
                String name = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                NoteblockPlayerNBT.putString(SongItemUtils.DISPLAY_NAME_KEY, name);
                NoteblockPlayer.mc.player.setStackInHand(Hand.MAIN_HAND, stack);
                SongItemUtils.addSongItemDisplay(stack);
                NoteblockPlayer.mc.interactionManager.clickCreativeStack(NoteblockPlayer.mc.player.getStackInHand(Hand.MAIN_HAND), 36 + NoteblockPlayer.mc.player.getInventory().selectedSlot);
                NoteblockPlayer.addChatMessage("§6Successfully set the sheet's display name to §3" + name);
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
                    "setname",
            }, suggestionsBuilder);
        }
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "create":
                if (split.length >= 2) {
                    String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                    return FileUtils.giveSongSuggestions(location, suggestionsBuilder);
                }
            case "setname":
            default:
                return null;
        }
    }
}
