package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class MaxRadius extends Command {
    @Override
    public String getName() {
        return "maxRadius";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{"set <radius>", "get"};
    }

    @Override
    public String getDescription() {
        return "Sets the maximum radius of the Stereo stage (not needed for other stages). This must be a number from 0 to 100.";
    }

    @Override
    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            return false;
        }

        String[] split = args.split(" ");
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "set":
                if (split.length == 2) {
                    try {
                        int radius = Integer.parseInt(split[1]);
                        if (radius < 0 || radius > 100) {
                            return false;
                        }
                        NoteblockPlayer.getConfig().maxRadius = radius;
                        NoteblockPlayer.addChatMessage("Radius value set to " + NoteblockPlayer.getConfig().maxRadius);
                        
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            case "get":
                NoteblockPlayer.addChatMessage("Radius value: " + NoteblockPlayer.getConfig().maxRadius);
                return true;
            default:
                return false;
        }
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);
        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[]{
                    "set",
                    "get",
            }, suggestionsBuilder);
        } else {
            return null;
        }
    }
}
