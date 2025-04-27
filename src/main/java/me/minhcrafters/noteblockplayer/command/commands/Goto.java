package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.TimeUtils;
import me.minhcrafters.noteblockplayer.song.SongHandler;

import java.io.IOException;

public class Goto extends Command {
    public String getName() {
        return "goto";
    }

    public String[] getSyntax() {
        return new String[]{"<mm:ss>"};
    }

    public String getDescription() {
        return "Goes to a specific time in the song";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            NoteblockPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        if (args.length() > 0) {
            try {
                long time = TimeUtils.parseTime(args);
                SongHandler.getInstance().currentSong.setTime(time);
                NoteblockPlayer.addChatMessage("§6Set song time to §3" + TimeUtils.formatTime(time));
                return true;
            } catch (IOException e) {
                NoteblockPlayer.addChatMessage("§cNot a valid time stamp");
                return false;
            }
        } else {
            return false;
        }
    }
}
