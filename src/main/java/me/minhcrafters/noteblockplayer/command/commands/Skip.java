package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class Skip extends Command {
    public String getName() {
        return "skip";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Skips current song";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }
        if (args.length() == 0) {
            SongHandler.getInstance().currentSong = null;
            return true;
        } else {
            return false;
        }
    }
}
