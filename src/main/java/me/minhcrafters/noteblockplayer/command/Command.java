package me.minhcrafters.noteblockplayer.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public abstract class Command {
    public abstract String getName();

    public abstract String[] getSyntax();

    public abstract String getDescription();

    public abstract boolean processCommand(String args);

    public String[] getAliases() {
        return new String[]{};
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return null;
    }
}
