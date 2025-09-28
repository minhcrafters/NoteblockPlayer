package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class UseEssentialsCommands extends Command {
    public String getName() {
        return "useEssentialsCommands";
    }

    public String[] getAliases() {
        return new String[] { "essentials", "useEssentials", "essentialsCommands" };
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Switches to using essentials gamemode commands";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            NoteblockPlayer.getConfig().creativeCommand = "gmc";
            NoteblockPlayer.getConfig().survivalCommand = "gms";
            NoteblockPlayer.addChatMessage("§6Now using essentials gamemode commands");

            return true;
        } else {
            return false;
        }
    }
}
