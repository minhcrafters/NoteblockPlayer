package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class SetCreativeCommand extends Command {
    public String getName() {
        return "setCreativeCommand";
    }

    public String[] getAliases() {
        return new String[] { "sc" };
    }

    public String[] getSyntax() {
        return new String[] { "<command>" };
    }

    public String getDescription() {
        return "Sets the command used to go into creative mode";
    }

    public boolean processCommand(String args) {
        if (args.length() > 0) {
            if (args.startsWith("/")) {
                NoteblockPlayer.getConfig().creativeCommand = args.substring(1);
            } else {
                NoteblockPlayer.getConfig().creativeCommand = args;
            }
            NoteblockPlayer
                    .addChatMessage("§6Set creative command to §3/" + NoteblockPlayer.getConfig().creativeCommand);

            return true;
        } else {
            return false;
        }
    }
}
