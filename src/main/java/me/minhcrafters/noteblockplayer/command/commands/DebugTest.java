package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.Note;
import me.minhcrafters.noteblockplayer.song.Song;
import me.minhcrafters.noteblockplayer.song.SongManager;

public class DebugTest extends Command {
    public String getName() {
        return "debugtest";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "debugtest"};
    }

    public String getDescription() {
        return "Creates a song for debugging purposes";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            Song song = new Song("debug_test_song", "minhcrafters", "minhcrafters", "a song for debugging purposes");
            for (int i = 0; i < 400; i++) {
                song.add(new Note(i, i * 50));
            }
            song.length = 400 * 50;
            SongManager.getInstance().setSong(song);
            return true;
        } else {
            return false;
        }
    }
}
