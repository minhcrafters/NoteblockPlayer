package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class Loop extends Command {
    public String getName() {
        return "loop";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Toggles song looping";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        SongHandler.getInstance().currentSong.looping = !SongHandler.getInstance().currentSong.looping;
        SongHandler.getInstance().currentSong.loopCount = 0;
        if (SongHandler.getInstance().currentSong.looping) {
            NoteblockPlayer.addChatMessage("§6Enabled looping");
        } else {
            NoteblockPlayer.addChatMessage("§6Disabled looping");
        }
        return true;
    }
}
