package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

public class Pause extends Command {
    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "pause"};
    }

    @Override
    public String getDescription() {
        return "Pauses the current playing song.";
    }

    @Override
    public boolean processCommand(String args) {
        if (SongManager.getInstance().currentSong == null && SongManager.getInstance().songQueue.isEmpty()) {
            NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
            return true;
        }

        if (args.isEmpty()) {
            SongManager.getInstance().currentSong.pause();
            NoteblockPlayer.addChatMessage(Text.of("§6Paused the current song."));
            return true;
        }

        return false;
    }
}
