package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.TimeUtils;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.song.Song;

public class Status extends Command {
    public String getName() {
        return "status";
    }

    public String[] getAliases() {
        return new String[] { "current", "songinfo" };
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Gets the status of the song that is currently playing";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            if (SongHandler.getInstance().currentSong == null) {
                NoteblockPlayer.addChatMessage("§6No song is currently playing");
                return true;
            }
            Song currentSong = SongHandler.getInstance().currentSong;
            long currentTime = Math.min(currentSong.time, currentSong.length);
            long totalTime = currentSong.length;

            // Build pretty-printed song information
            String songInfo = "§6----- Song Information -----\n" +
                    String.format("§6Title        : §3%s\n", currentSong.name) +
                    String.format("§6Author       : §3%s\n", currentSong.author) +
                    String.format("§6Description  : §3%s\n",
                            currentSong.description.isEmpty() ? "N/A" : currentSong.description)
                    +
                    String.format("§6Time         : §3%s / %s\n", TimeUtils.formatTime(currentTime),
                            TimeUtils.formatTime(totalTime))
                    +
                    String.format("§6Layers Count : §3%s\n", currentSong.getLayers().size()) +
                    String.format("§6Notes count  : §3%d\n", currentSong.getTotalNotes().size()) +
                    String.format("§6Current Note : §3%d\n", currentSong.position) +
                    String.format("§6Looping      : §3%s\n", currentSong.looping ? "Yes" : "No") +
                    String.format("§6Paused       : §3%s", currentSong.paused ? "Yes" : "No");
            NoteblockPlayer.addChatMessage(songInfo);
            return true;
        } else {
            return false;
        }
    }
}