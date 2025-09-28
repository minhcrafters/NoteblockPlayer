package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;

public class ToggleFakePlayer extends Command {
    public String getName() {
        return "toggleFakePlayer";
    }

    public String[] getAliases() {
        return new String[] { "fakePlayer", "fp" };
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Shows a fake player representing your true position when playing songs";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            NoteblockPlayer.getConfig().showFakePlayer = !NoteblockPlayer.getConfig().showFakePlayer;
            if (NoteblockPlayer.getConfig().showFakePlayer) {
                NoteblockPlayer.addChatMessage("§6Enabled fake player");
            } else {
                NoteblockPlayer.addChatMessage("§6Disabled fake player");
            }

            return true;
        } else {
            return false;
        }
    }
}
