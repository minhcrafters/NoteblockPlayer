package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;

public class ToggleSurvivalOnly extends Command {
    public String getName() {
        return "toggleSurvivalOnly";
    }

    public String[] getAliases() {
        return new String[] { "survivalOnly" };
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Enables or disables survival-only mode, in which automatic noteblock placement is disabled and automatic tuning is done by right-clicking.";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            if (!SongHandler.getInstance().isIdle()) {
                NoteblockPlayer.addChatMessage("§cYou cannot change this setting while playing or building");
                return true;
            }

            NoteblockPlayer.getConfig().survivalOnly = !NoteblockPlayer.getConfig().survivalOnly;
            if (NoteblockPlayer.getConfig().survivalOnly) {
                NoteblockPlayer.addChatMessage("§6Enabled survival only mode");
            } else {
                NoteblockPlayer.addChatMessage("§6Disabled survival only mode");
            }

            return true;
        } else {
            return false;
        }
    }
}
