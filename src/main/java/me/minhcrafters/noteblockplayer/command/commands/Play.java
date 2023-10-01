package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import net.minecraft.command.CommandSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Play extends Command {
    public String getName() {
        return "play";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "play <song or url>"};
    }

    public String getDescription() {
        return "Plays a song";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            SongManager.getInstance().loadSong(args);
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = args.lastIndexOf("/");
        String dirString = "";
        Path dir = NoteblockPlayer.SONGS_DIR;
        if (lastSlash >= 0) {
            dirString = args.substring(0, lastSlash + 1);
            dir = dir.resolve(dirString);
        }

        if (!Files.exists(dir)) return null;

        ArrayList<String> suggestions = new ArrayList<>();
        for (File file : Objects.requireNonNull(FileUtils.listFilesSilently(dir))
                .map(Path::toFile)
                .toList()) {
            if (file.isFile()) {
                suggestions.add(dirString + file.getName());
            } else if (file.isDirectory()) {
                suggestions.add(dirString + file.getName() + "/");
            }
        }
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }
}
