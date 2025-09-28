package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class Resume extends Command {
    public String getName() {
        return "resume";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Resumes the current song";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            SongHandler.getInstance().resume();
            return true;
        } else {
            return false;
        }
    }
}