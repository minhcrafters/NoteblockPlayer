package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import net.minecraft.text.Text;

public class SetPrefix extends Command {
    @Override
    public String getName() {
        return "setprefix";
    }

    @Override
    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "setprefix <new_prefix>"};
    }

    @Override
    public String getDescription() {
        return "Changes the prefix of this mod.";
    }

    @Override
    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            NoteblockPlayer.getConfig().commandPrefix = args.trim();
            NoteblockPlayer.addChatMessage(Text.of(String.format("§6Successfully set prefix to §3%s§6.", args.trim())));
            return true;
        }
        return false;
    }
}
