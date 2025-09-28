package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.command.CommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Help extends Command {

    private static final int PAGE_SIZE = 10;

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String[] getSyntax() {
        return new String[] { "[command|page]" };
    }

    @Override
    public String getDescription() {
        return "Lists commands or explains command";
    }

    @Override
    public boolean processCommand(String args) {
        // if no args or the argument is numeric (indicating page number), show
        // paginated commands
        if (args.isEmpty() || isNumeric(args.trim())) {
            int pageNumber = 1;
            if (!args.trim().isEmpty()) {
                try {
                    pageNumber = Integer.parseInt(args.trim());
                } catch (NumberFormatException e) {
                    // Should not happen because of our isNumeric check
                }
            }
            // Calculate total pages using the commands list from CommandManager
            int totalCommands = CommandManager.commands.size();
            int totalPages = (totalCommands + PAGE_SIZE - 1) / PAGE_SIZE;
            if (pageNumber < 1 || pageNumber > totalPages) {
                NoteblockPlayer
                        .addChatMessage("§cPage " + pageNumber + " does not exist. Valid pages: 1 - " + totalPages);
                return true;
            }
            // Build header and commands for this page

            if (pageNumber > 1) {
                for (int i = 0; i < PAGE_SIZE + 2; i++) {
                    NoteblockPlayer.addChatMessage("");
                }
            }

            NoteblockPlayer
                    .addChatMessage("§6---------- Commands (Page " + pageNumber + "/" + totalPages + ") ----------");
            int startIndex = (pageNumber - 1) * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, totalCommands);
            for (int i = startIndex; i < endIndex; i++) {
                Command c = CommandManager.commands.get(i);

                NoteblockPlayer.addChatMessage(Text.literal("§3/" + CommandManager.COMMAND_ROOT + " " + c.getName())
                        .setStyle(Style.EMPTY
                                .withColor(Formatting.AQUA)
                                .withBold(true)
                                .withHoverEvent(
                                        new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Text.literal(String.format("%s\n\nClick to run command",
                                                        c.getDescription()))))
                                .withClickEvent(
                                        new ClickEvent(
                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                "/" + CommandManager.COMMAND_ROOT + " " + c.getName()))));
            }
            // Build navigation with clickable components if available
            MutableText navText = Text.empty();

            if (pageNumber > 1) {
                String prevCommand = "/" + CommandManager.COMMAND_ROOT + " help " + (pageNumber - 1);
                navText.append(createClickableComponent("[Prev]", prevCommand)).append("  ");
            } else {
                navText.append(Text.literal("§7[Prev]")).append("  ");
            }

            if (pageNumber < totalPages) {
                String nextCommand = "/" + CommandManager.COMMAND_ROOT + " help " + (pageNumber + 1);
                navText.append(createClickableComponent("[Next]", nextCommand));
            } else {
                navText.append(Text.literal("§7[Next]"));
            }

            NoteblockPlayer.addChatMessage(navText);
        } else {
            // If the argument is not numeric, treat it as a command name for detailed help
            String commandName = args.toLowerCase(Locale.ROOT);
            if (CommandManager.commandMap.containsKey(commandName)) {
                Command c = CommandManager.commandMap.get(commandName);
                NoteblockPlayer.addChatMessage("§6----------------പരമായ--------------");
                NoteblockPlayer.addChatMessage("§6Help: §3" + c.getName());
                NoteblockPlayer.addChatMessage("§6Description: §3" + c.getDescription());
                if (c.getSyntax().length == 0) {
                    NoteblockPlayer
                            .addChatMessage("§6Usage: §3" + "/" + CommandManager.COMMAND_ROOT + " " + c.getName());
                } else if (c.getSyntax().length == 1) {
                    NoteblockPlayer.addChatMessage("§6Usage: §3" + "/" + CommandManager.COMMAND_ROOT + " " + c.getName()
                            + " " + c.getSyntax()[0]);
                } else {
                    NoteblockPlayer.addChatMessage("§6Usage:");
                    for (String syntax : c.getSyntax()) {
                        NoteblockPlayer.addChatMessage(
                                "    §3" + "/" + CommandManager.COMMAND_ROOT + " " + c.getName() + " " + syntax);
                    }
                }
                if (c.getAliases().length > 0) {
                    NoteblockPlayer.addChatMessage("§6Aliases: §3" + String.join(", ", c.getAliases()));
                }
                NoteblockPlayer.addChatMessage("§6------------------------------");
            } else {
                NoteblockPlayer.addChatMessage("§cCommand not recognized: " + args);
            }
        }
        return true;
    }

    /**
     * Creates a clickable JSON text component.
     * When clicked, the command specified in "command" will be run.
     */
    private MutableText createClickableComponent(String display, String command) {
        return createClickableComponent(Text.literal(display), Text.empty(), command);
    }

    /**
     * Creates a clickable JSON text component.
     * When clicked, the command specified in "command" will be run.
     */
    private MutableText createClickableComponent(MutableText display, Text description, String command) {
        Style clickableStyle = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, description))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

        return display.setStyle(clickableStyle);
    }

    /**
     * Utility method to check if a given string is numeric.
     */
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(CommandManager.commandCompletions, suggestionsBuilder);
    }
}