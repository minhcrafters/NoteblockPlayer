package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.song.Song;

public class Queue extends Command {
    public String getName() {
        return "queue";
    }

    public String[] getAliases() {
        return new String[]{"showQueue"};
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Shows the current song queue";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
                NoteblockPlayer.addChatMessage("§6No song is currently playing");
                return true;
            }

            NoteblockPlayer.addChatMessage("§6------------------------------");
            if (SongHandler.getInstance().currentSong != null) {
                NoteblockPlayer.addChatMessage("§6Current song: §3" + SongHandler.getInstance().currentSong.name);
            }
            int index = 0;
            for (Song song : SongHandler.getInstance().songQueue) {
                index++;
                NoteblockPlayer.addChatMessage(String.format("§6%d. §3%s", index, song.name));
            }
            NoteblockPlayer.addChatMessage("§6------------------------------");
            return true;
        } else {
            return false;
        }
    }
}
