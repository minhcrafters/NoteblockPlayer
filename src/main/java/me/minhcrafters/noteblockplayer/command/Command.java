package me.minhcrafters.noteblockplayer.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public abstract class Command {
    /**
     * Get the name of the command
     * @return The command name
     */
    public abstract String getName();

    /**
     * Get the expected syntax for the command
     * @return Array of syntax options
     */
    public abstract String[] getSyntax();

    /**
     * Get the command description
     * @return The description text
     */
    public abstract String getDescription();

    /**
     * Process the command with the given arguments
     * @param args Command arguments as a string
     * @return true if command executed successfully, false if syntax error
     */
    public abstract boolean processCommand(String args);

    /**
     * Get command aliases
     * @return Array of aliases for this command
     */
    public String[] getAliases() {
        return new String[0];
    }

    /**
     * Get suggestions for command arguments
     * @param args Partial argument string 
     * @param suggestionsBuilder Builder for tab completion suggestions
     * @return CompletableFuture containing suggestions
     */
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return suggestionsBuilder.buildFuture();
    }
    
    /**
     * Get required permission level to execute this command
     * @return The required permission level (default 0 for all players)
     */
    public int getRequiredPermissionLevel() {
        return 0; // Default permission level that all players have
    }
}
