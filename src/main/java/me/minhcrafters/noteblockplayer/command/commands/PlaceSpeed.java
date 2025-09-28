package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PlaceSpeed extends Command {
    public String getName() {
        return "placeSpeed";
    }

    public String[] getSyntax() {
        return new String[] {
                "set <speed>",
                "reset"
        };
    }

    public String getDescription() {
        return "Sets the block placement speed in blocks/sec";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            return false;
        }

        String[] split = args.split(" ");
        switch (split[0].toLowerCase(Locale.ROOT)) {
            case "set":
                if (split.length != 2)
                    return false;
                double speed;
                try {
                    speed = Double.parseDouble(split[1]);
                } catch (NumberFormatException e) {
                    NoteblockPlayer.addChatMessage("§cSpeed must be a number");
                    return true;
                }
                if (speed <= 0) {
                    NoteblockPlayer.addChatMessage("§cSpeed must be greater than 0");
                    return true;
                }
                NoteblockPlayer.getConfig().placeSpeed = speed;

                NoteblockPlayer.addChatMessage(
                        "§6Set block placement speed to §3" + NoteblockPlayer.getConfig().placeSpeed + " §6blocks/sec");
                return true;
            case "reset":
                if (split.length != 1)
                    return false;
                NoteblockPlayer.getConfig().placeSpeed = 20;

                NoteblockPlayer.addChatMessage("§6Reset block placement speed to §3"
                        + NoteblockPlayer.getConfig().placeSpeed + " §6blocks/sec");
                return true;
            default:
                return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);
        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[] {
                    "set",
                    "reset",
            }, suggestionsBuilder);
        } else {
            return null;
        }
    }
}
