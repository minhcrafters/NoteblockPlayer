package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class SetSurvivalCommand extends Command {
    public String getName() {
        return "setSurvivalCommand";
    }

    public String[] getAliases() {
        return new String[]{"ss"};
    }

    public String[] getSyntax() {
        return new String[]{"<command>"};
    }

    public String getDescription() {
        return "Sets the command used to go into survival mode";
    }

    public boolean processCommand(String args) {
        if (args.length() > 0) {
            if (args.startsWith("/")) {
                NoteblockPlayer.getConfig().survivalCommand = args.substring(1);
            } else {
                NoteblockPlayer.getConfig().survivalCommand = args;
            }
            NoteblockPlayer.addChatMessage("§6Set survival command to §3/" + NoteblockPlayer.getConfig().survivalCommand);
            
            return true;
        } else {
            return false;
        }
    }
}
