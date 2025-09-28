package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class Pause extends Command {
    public String getName() {
        return "pause";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Pauses the current song";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            SongHandler.getInstance().pause();
            return true;
        } else {
            return false;
        }
    }
}