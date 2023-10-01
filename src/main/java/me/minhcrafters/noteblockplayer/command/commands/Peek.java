package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import me.minhcrafters.noteblockplayer.utils.TimeUtils;
import net.minecraft.text.Text;

import java.io.IOException;

public class Peek extends Command {
    public String getName() {
        return "peek";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "peek <mm:ss>"};
    }

    public String getDescription() {
        return "Goes to a specific time in the song";
    }

    public boolean processCommand(String args) {
        if (SongManager.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
            return true;
        }

        if (args.length() > 0) {
            try {
                long time = TimeUtils.parseTime(args);
                SongManager.getInstance().currentSong.setTime(time);
                NoteblockPlayer.addChatMessage(Text.of("§6Peeked to §3" + TimeUtils.formatTime(time)));
                return true;
            } catch (IOException e) {
                NoteblockPlayer.addChatMessage(Text.of("§cNot a valid timestamp"));
                return false;
            }
        } else {
            return false;
        }
    }
}
