package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.stage.Stage;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class StageType extends Command {
    @Override
    public String getName() {
        return "setstagetype";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"setstage", "stagetype"};
    }

    @Override
    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "setstagetype <default | wide | spherical>"};
    }

    @Override
    public String getDescription() {
        return "Sets the noteblock stage design.";
    }

    @Override
    public boolean processCommand(String args) {
        if (!args.isEmpty()) {
            try {
                Stage.StageType stageType = Stage.StageType.valueOf(args.toUpperCase(Locale.ROOT));
                NoteblockPlayer.getConfig().stageType = stageType;
                NoteblockPlayer.addChatMessage(Text.of("§6Set stage type to §3" + stageType.name()));
            } catch (IllegalArgumentException e) {
                NoteblockPlayer.addChatMessage(Text.of("§cInvalid stage type"));
            }
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(Arrays.stream(Stage.StageType.values()).map(Stage.StageType::name), suggestionsBuilder);
        } else {
            return null;
        }
    }
}
