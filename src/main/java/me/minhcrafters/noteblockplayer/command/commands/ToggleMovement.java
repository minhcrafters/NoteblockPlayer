package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ToggleMovement extends Command {
    public String getName() {
        return "toggleMovement";
    }

    public String[] getAliases() {
        return new String[] { "movement" };
    }

    public String[] getSyntax() {
        return new String[] { "<swing | rotate>" };
    }

    public String getDescription() {
        return "Toggles different types of movements";
    }

    public boolean processCommand(String args) {
        switch (args.toLowerCase(Locale.ROOT)) {
            case "swing":
                NoteblockPlayer.getConfig().swing = !NoteblockPlayer.getConfig().swing;
                if (NoteblockPlayer.getConfig().swing) {
                    NoteblockPlayer.addChatMessage("§6Enabled arm swinging");
                } else {
                    NoteblockPlayer.addChatMessage("§6Disabled arm swinging");
                }

                return true;
            case "rotate":
                NoteblockPlayer.getConfig().rotate = !NoteblockPlayer.getConfig().rotate;
                if (NoteblockPlayer.getConfig().rotate) {
                    NoteblockPlayer.addChatMessage("§6Enabled player rotation");
                } else {
                    NoteblockPlayer.addChatMessage("§6Disabled player rotation");
                }

                return true;
            default:
                return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(new String[] { "swing", "rotate" }, suggestionsBuilder);
        } else {
            return null;
        }
    }
}
