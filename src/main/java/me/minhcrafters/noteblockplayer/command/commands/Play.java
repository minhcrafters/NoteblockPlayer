package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import net.minecraft.world.GameMode;

import java.util.concurrent.CompletableFuture;

public class Play extends Command {
    public String getName() {
        return "play";
    }

    public String[] getSyntax() {
        return new String[]{"<song or url>"};
    }

    public String getDescription() {
        return "Plays a song";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            if (NoteblockPlayer.getConfig().survivalOnly && NoteblockPlayer.mc.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
                NoteblockPlayer.addChatMessage("§cTo play in survival only mode, you must be in survival mode to start with.");
                return true;
            }

            NoteblockPlayer.addChatMessage(args);

            SongHandler.getInstance().loadSong(args);
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return Utils.giveSongSuggestions(args, suggestionsBuilder);
    }
}
