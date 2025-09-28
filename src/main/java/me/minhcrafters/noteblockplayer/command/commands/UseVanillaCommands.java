package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class UseVanillaCommands extends Command {
    public String getName() {
        return "useVanillaCommands";
    }

    public String[] getAliases() {
        return new String[] { "vanilla", "useVanilla", "vanillaCommands" };
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Switches to using vanilla gamemode commands";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            NoteblockPlayer.getConfig().creativeCommand = "gamemode creative";
            NoteblockPlayer.getConfig().survivalCommand = "gamemode survival";
            NoteblockPlayer.addChatMessage("§6Now using vanilla gamemode commands");

            return true;
        } else {
            return false;
        }
    }
}
