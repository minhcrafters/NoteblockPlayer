package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.song.SongManager;
import net.minecraft.text.Text;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.mc;

public class Stop extends Command {
    public String getName() {
        return "stop";
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "stop"};
    }

    public String getDescription() {
        return "Stops playing";
    }

    public boolean processCommand(String args) {
        if (SongManager.getInstance().currentSong == null && SongManager.getInstance().songQueue.isEmpty()) {
            NoteblockPlayer.addChatMessage(Text.of("§6No song is currently playing"));
            return true;
        }
        if (args.length() == 0) {
            if (SongManager.getInstance().stage != null) {
                SongManager.getInstance().stage.movePlayerToStagePosition();
            }
            SongManager.getInstance().restoreStateAndCleanUp();
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
            NoteblockPlayer.addChatMessage(Text.of("§6Stopped playing"));
            return true;
        } else {
            return false;
        }
    }
}
