package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.utils.Paginator;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static me.minhcrafters.noteblockplayer.command.CommandManager.*;

public class Help extends Command {
    public String getName() {
        return "help";
    }

    public String[] getSyntax() {
        return new String[]{getCommandPrefix() + "help [command]"};
    }

    public String getDescription() {
        return "Prints this page";
    }

    public boolean processCommand(String args) {
        String firstLine = "§3" + NoteblockPlayer.getModName() + " v" + NoteblockPlayer.getModVersion() + " §6by §3" + String.join(", ", NoteblockPlayer.getModAuthors()) + "\n§6Available commands:";

        if (args.isEmpty()) {
            ArrayList<Text> commandsList = new ArrayList<>();
            ArrayList<String> syntaxList = new ArrayList<>();

            for (Command c : commands) {
                syntaxList.addAll(Arrays.asList(c.getSyntax()));
                commandsList.add(Text.literal("§3" + CommandManager.getCommandPrefix() + c.getName())
                        .setStyle(Style.EMPTY
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(
                                        "§6Description: §3" + c.getDescription() + "\n" +
                                                "§6Usage: §3" + String.join("\n", syntaxList) + "\n" +
                                                "§6Aliases: §3" + (c.getAliases().length > 0 ? String.join(", ", c.getAliases()) : "None")))
                                )
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CommandManager.getCommandPrefix() + c.getName() + " "))
                        )
                );
            }

            new Paginator(commandsList, firstLine, getName(), 1, 6).display();
        } else {
            try {
                int pageArgs = Integer.parseInt(args);

                ArrayList<Text> commandsList = new ArrayList<>();

                for (Command c : commands) {
                    commandsList.add(Text.literal("§3" + CommandManager.getCommandPrefix() + c.getName())
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(
                                            "§6Description: §3" + c.getDescription() + "\n" +
                                                    "§6Usage: §3" + c.getSyntax() + "\n" +
                                                    "§6Aliases: §3" + (c.getAliases().length > 0 ? String.join(", ", c.getAliases()) : "None")))
                                    )
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CommandManager.getCommandPrefix() + c.getName() + " "))
                            )
                    );
                }

                new Paginator(commandsList, firstLine, getName(), pageArgs, 6).display();
                return true;
            } catch (NumberFormatException e) {
                if (commandMap.containsKey(args.toLowerCase(Locale.ROOT))) {
                    Command c = commandMap.get(args.toLowerCase(Locale.ROOT));
                    NoteblockPlayer.addChatMessage(Text.of("§6------------------------------"));
                    NoteblockPlayer.addChatMessage(Text.of("§6Help: §3" + c.getName()));
                    NoteblockPlayer.addChatMessage(Text.of("§6Description: §3" + c.getDescription()));
                    NoteblockPlayer.addChatMessage(Text.of("§6Usage: §3" + c.getSyntax()));
                    if (c.getAliases().length > 0) {
                        NoteblockPlayer.addChatMessage(Text.of("§6Aliases: §3" + String.join(", ", c.getAliases())));
                    }
                    NoteblockPlayer.addChatMessage(Text.of("§6------------------------------"));
                } else {
                    NoteblockPlayer.addChatMessage(Text.of("§cUnrecognized command: " + args));
                }
                return true;
            }
        }
        return true;
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(commandCompletions, suggestionsBuilder);
    }
}