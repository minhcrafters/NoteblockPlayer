package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import net.minecraft.text.Text;

public class ToggleFakePlayer extends Command {
    public String getName() {
        return "togglefakeplayer";
    }

    public String[] getAliases() {
        return new String[]{"fakeplayer", "fp"};
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "togglefakeplayer"};
    }

    public String getDescription() {
        return "Shows a fake player representing your true position when playing songs";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            NoteblockPlayer.showFakePlayer = !NoteblockPlayer.showFakePlayer;
            if (NoteblockPlayer.showFakePlayer) {
                NoteblockPlayer.addChatMessage(Text.of("§6Enabled fake player"));
            } else {
                NoteblockPlayer.addChatMessage(Text.of("§6Disabled fake player"));
            }
            return true;
        } else {
            return false;
        }
    }
}
