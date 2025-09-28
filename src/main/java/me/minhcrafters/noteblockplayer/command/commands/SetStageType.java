package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SetStageType extends Command {
    public String getName() {
        return "setStageType";
    }

    public String[] getAliases() {
        return new String[] { "setStage", "stageType" };
    }

    public String[] getSyntax() {
        return new String[] { "<DEFAULT | WIDE | SPHERICAL>" };
    }

    public String getDescription() {
        return "Sets the type of noteblock stage to build";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            try {
                Stage.StageType stageType = Stage.StageType.valueOf(args.toUpperCase(Locale.ROOT));
                NoteblockPlayer.getConfig().stageType = stageType;
                NoteblockPlayer.addChatMessage("§6Set stage type to §3" + stageType.name());

            } catch (IllegalArgumentException e) {
                NoteblockPlayer.addChatMessage("§cInvalid stage type");
            }
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(Arrays.stream(Stage.StageType.values()).map(Stage.StageType::name),
                    suggestionsBuilder);
        } else {
            return null;
        }
    }
}
