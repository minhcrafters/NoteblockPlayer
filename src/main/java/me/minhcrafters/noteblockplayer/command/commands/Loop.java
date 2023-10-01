package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

public class Loop extends Command {
    public String getName() {
        return "loop";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "loop"};
    }

    public String getDescription() {
        return "Toggles song looping";
    }

    public boolean processCommand(String args) {
        if (SongManager.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
            return true;
        }

        SongManager.getInstance().currentSong.looping = !SongManager.getInstance().currentSong.looping;
        SongManager.getInstance().currentSong.loopCount = 0;
        if (SongManager.getInstance().currentSong.looping) {
            NoteblockPlayer.addChatMessage(Text.of("§6Enabled looping"));
        } else {
            NoteblockPlayer.addChatMessage(Text.of("§6Disabled looping"));
        }
        return true;
    }
}
