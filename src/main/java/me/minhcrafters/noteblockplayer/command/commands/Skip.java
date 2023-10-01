package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

public class Skip extends Command {
    public String getName() {
        return "skip";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "skip"};
    }

    public String getDescription() {
        return "Skips current song";
    }

    public boolean processCommand(String args) {
        if (SongManager.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
            return true;
        }
        if (args.isEmpty()) {
            SongManager.getInstance().currentSong = null;
            return true;
        } else {
            return false;
        }
    }
}
