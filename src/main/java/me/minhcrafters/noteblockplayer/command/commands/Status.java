package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.Song;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

public class Status extends Command {
    public String getName() {
        return "status";
    }

    public String[] getAliases() {
        return new String[]{"current", "nowplaying", "np"};
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "status"};
    }

    public String getDescription() {
        return "Gets the status of the song that is currently playing";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            if (SongManager.getInstance().currentSong == null) {
                NoteblockPlayer.addChatMessage(Text.of("§6No songs are currently playing."));
                return true;
            }

            Song currentSong = SongManager.getInstance().currentSong;

            NoteblockPlayer.addChatMessage(Text.of(String.format("""
                    §6--------- §3Now playing §6---------
                    §6Name: §3%s
                    §6Author: §3%s
                    §6Original author: §3%s
                    §6Description: §3%s
                    """, currentSong.name, currentSong.author, currentSong.originalAuthor, currentSong.description)));
            return true;
        } else {
            return false;
        }
    }
}
