package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class Stop extends Command {
    public String getName() {
        return "stop";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Stops playing";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().isIdle()) {
            NoteblockPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }
        if (args.length() == 0) {
            if (SongHandler.getInstance().cleaningUp) {
                SongHandler.getInstance().restoreStateAndReset();
                NoteblockPlayer.addChatMessage("§6Stopped cleanup");
            } else if (NoteblockPlayer.getConfig().autoCleanup && SongHandler.getInstance().originalBlocks.size() != 0) {
                SongHandler.getInstance().partialResetAndCleanup();
                NoteblockPlayer.addChatMessage("§6Stopped playing and switched to cleanup");
            } else {
                SongHandler.getInstance().restoreStateAndReset();
                NoteblockPlayer.addChatMessage("§6Stopped playing");
            }
            return true;
        } else {
            return false;
        }
    }
}
