package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class ToggleAutoCleanup extends Command {
    public String getName() {
        return "toggleAutoCleanup";
    }

    public String[] getAliases() {
        return new String[]{"autoCleanup"};
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Toggles whether you automatically clean up your stage and restore the original blocks after playing";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            NoteblockPlayer.getConfig().autoCleanup = !NoteblockPlayer.getConfig().autoCleanup;
            if (NoteblockPlayer.getConfig().autoCleanup) {
                NoteblockPlayer.addChatMessage("§6Enabled automatic cleanup");
            } else {
                NoteblockPlayer.addChatMessage("§6Disabled automatic cleanup");
            }
            
            return true;
        } else {
            return false;
        }
    }
}
