package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;

public class ToggleFlightNoClip extends Command {
    public String getName() {
        return "toggleFlightNoclip";
    }

    public String[] getAliases() {
        return new String[]{"flightNoclip"};
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Toggles flight noclip. When enabled, your local player can clip through blocks when flying while playing a song.";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            NoteblockPlayer.getConfig().flightNoclip = !NoteblockPlayer.getConfig().flightNoclip;
            if (NoteblockPlayer.getConfig().flightNoclip) {
                NoteblockPlayer.addChatMessage("§6Enabled flight noclip");
            } else {
                NoteblockPlayer.addChatMessage("§6Disabled flight noclip");
            }
            
            return true;
        } else {
            return false;
        }
    }
}
