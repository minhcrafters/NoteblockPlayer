package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class SetVelocityThreshold extends Command {
    public String getName() {
        return "setVelocityThreshold";
    }

    public String[] getAliases() {
        return new String[] { "velocityThreshold", "threshold" };
    }

    public String[] getSyntax() {
        return new String[] { "<threshold>" };
    }

    public String getDescription() {
        return "Sets the minimum velocity below which notes won't be played (applies to midi and nbs). This must be a number from 0 to 100. For song items, the threshold is baked in upon item creation.";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            try {
                int threshold = Integer.parseInt(args);
                if (threshold < 0 || threshold > 100) {
                    NoteblockPlayer.addChatMessage("§cVelocity threshold must be a value between 0 and 100");
                    return true;
                }
                NoteblockPlayer.getConfig().velocityThreshold = threshold;
                NoteblockPlayer.addChatMessage("§6Set velocity threshold to " + threshold);

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
