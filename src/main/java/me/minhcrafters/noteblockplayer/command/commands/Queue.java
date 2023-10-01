package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.Song;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

public class Queue extends Command {
    public String getName() {
        return "queue";
    }

    public String[] getAliases() {
        return new String[]{"showQueue"};
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "queue"};
    }

    public String getDescription() {
        return "Shows the current song queue";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            if (SongManager.getInstance().currentSong == null && SongManager.getInstance().songQueue.isEmpty()) {
                NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
                return true;
            }

            NoteblockPlayer.addChatMessage(Text.of("§6------------------------------"));
            if (SongManager.getInstance().currentSong != null) {
                NoteblockPlayer.addChatMessage(Text.of("§6Current song: §3" + SongManager.getInstance().currentSong.name));
            }
            int index = 0;
            for (Song song : SongManager.getInstance().songQueue) {
                index++;
                NoteblockPlayer.addChatMessage(Text.of(String.format("§6%d. §3%s", index, song.name)));
            }
            NoteblockPlayer.addChatMessage(Text.of("§6------------------------------"));
            return true;
        } else {
            return false;
        }
    }
}
