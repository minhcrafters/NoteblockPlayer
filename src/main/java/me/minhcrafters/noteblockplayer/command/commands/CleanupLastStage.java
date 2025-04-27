package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.mc;

public class CleanupLastStage extends Command {
    public String getName() {
        return "cleanupLastStage";
    }

    public String[] getAliases() {
        return new String[]{};
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Cleans up your most recent stage and restores the original blocks";
    }

    public boolean processCommand(String args) {
        if (args.length() == 0) {
            Stage lastStage = SongHandler.getInstance().lastStage;
            if (!SongHandler.getInstance().isIdle()) {
                NoteblockPlayer.addChatMessage("§cYou cannot start cleanup if you are in the middle of another action");
                return true;
            }
            if (lastStage == null || SongHandler.getInstance().originalBlocks.size() == 0 || !lastStage.serverIdentifier.equals(Utils.getServerIdentifier())) {
                NoteblockPlayer.addChatMessage("§6There is nothing to clean up");
                return true;
            }
            if (mc.player.getPos().squaredDistanceTo(lastStage.getOriginBottomCenter()) > 3 * 3 || !lastStage.worldName.equals(Utils.getWorldName())) {
                String coordStr = String.format(
                        "%d %d %d",
                        lastStage.position.getX(), lastStage.position.getY(), lastStage.position.getZ()
                );
                NoteblockPlayer.addChatMessage("§6You must be within §33 §6blocks of the center of your stage to start cleanup.");
                MutableText coordText = Utils.joinTexts(null,
                        Text.literal("This is at ").setStyle(Style.EMPTY.withColor(Formatting.GOLD)),
                        Text.literal(coordStr).setStyle(
                                Style.EMPTY
                                        .withColor(Formatting.DARK_AQUA)
                                        .withUnderline(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordStr))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy \"" + coordStr + "\"")))
                        ),
                        Text.literal(" in world ").setStyle(Style.EMPTY.withColor(Formatting.GOLD)),
                        Text.literal(lastStage.worldName).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA))
                );
                NoteblockPlayer.addChatMessage(coordText);
                return true;
            }

            SongHandler.getInstance().startCleanup();
            return true;
        } else {
            return false;
        }
    }
}
