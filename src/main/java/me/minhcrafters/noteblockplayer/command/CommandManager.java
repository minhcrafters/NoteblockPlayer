package me.minhcrafters.noteblockplayer.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.commands.Queue;
import me.minhcrafters.noteblockplayer.command.commands.*;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.minhcrafters.noteblockplayer.NoteblockPlayer.FORCE_PREFIX;

public class CommandManager {
    public static ArrayList<Command> commands = new ArrayList<>();
    public static HashMap<String, Command> commandMap = new HashMap<>();
    public static ArrayList<String> commandCompletions = new ArrayList<>();

    public static void initCommands() {
        commands.add(new Help());
        commands.add(new Play());
        commands.add(new Stop());
        commands.add(new Skip());
        commands.add(new Peek());
        commands.add(new Pause());
        commands.add(new Loop());
        commands.add(new Status());
        commands.add(new Queue());
        commands.add(new Songs());
        commands.add(new SongItem());
        commands.add(new StageType());
        commands.add(new SetPrefix());
        commands.add(new ToggleFakePlayer());
        commands.add(new DebugTest());

        commands.sort(Comparator.comparing(Command::getName));

        for (Command command : commands) {
            commandMap.put(command.getName().toLowerCase(Locale.ROOT), command);
            commandCompletions.add(command.getName());
            for (String alias : command.getAliases()) {
                commandMap.put(alias.toLowerCase(Locale.ROOT), command);
                commandCompletions.add(alias);
            }
        }
    }

    // returns true if it is a command and should be cancelled
    public static boolean processChatMessage(String message) {
        if (message.startsWith(NoteblockPlayer.getConfig().commandPrefix) || message.startsWith(FORCE_PREFIX)) {
            String[] parts = message.substring(1).split(" ", 2);
            String name = parts.length > 0 ? parts[0] : "";
            String args = parts.length > 1 ? parts[1] : "";
            Command c = commandMap.get(name.toLowerCase(Locale.ROOT));
            if (c == null) {
                NoteblockPlayer.addChatMessage(Text.of("§cInvalid command. Use " + NoteblockPlayer.getConfig().commandPrefix + "help to get help in using this mod."));
            } else {
                try {
                    boolean success = c.processCommand(args);
                    if (!success) {
                        NoteblockPlayer.addChatMessage(Text.of("§cInvalid syntax.\nCorrect usage: " + String.join("\n", c.getSyntax())));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    NoteblockPlayer.addChatMessage(Text.of("§cAn error occurred while running this command: §4" + e.getMessage()));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    // prefix included in command string
    public static CompletableFuture<Suggestions> handleSuggestions(String text, SuggestionsBuilder suggestionsBuilder) {
        if (!text.contains(" ")) {
            List<String> names = commandCompletions
                    .stream()
                    .map((commandName) -> NoteblockPlayer.getConfig().commandPrefix + commandName)
                    .collect(Collectors.toList());
            return CommandSource.suggestMatching(names, suggestionsBuilder);
        } else {
            String[] split = text.split(" ");
            if (split[0].startsWith(NoteblockPlayer.getConfig().commandPrefix)) {
                String commandName = split[0].substring(1).toLowerCase(Locale.ROOT);
                if (commandMap.containsKey(commandName)) {
                    return commandMap.get(commandName).getSuggestions(split.length == 1 ? "" : split[1], suggestionsBuilder);
                }
            }
            return null;
        }
    }

    public static String getCommandPrefix() {
        return NoteblockPlayer.getConfig().commandPrefix;
    }
}
