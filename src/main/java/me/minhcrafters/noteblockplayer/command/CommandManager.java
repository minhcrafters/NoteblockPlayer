package me.minhcrafters.noteblockplayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.command.commands.*;
import me.minhcrafters.noteblockplayer.command.commands.Queue;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandManager {
    public static ArrayList<Command> commands = new ArrayList<>();
    public static HashMap<String, Command> commandMap = new HashMap<>();
    public static ArrayList<String> commandCompletions = new ArrayList<>();
    public static final String COMMAND_ROOT = "nbp";

    public static void initCommands() {
        registerCommand(new Help());
        registerCommand(new Play());
        registerCommand(new Stop());
        registerCommand(new Skip());
        registerCommand(new Goto());
        registerCommand(new Loop());
        registerCommand(new Status());
        registerCommand(new Queue());
        registerCommand(new Songs());
        registerCommand(new MaxRadius());
        registerCommand(new PlaylistCommand());
        registerCommand(new SetCreativeCommand());
        registerCommand(new SetSurvivalCommand());
        registerCommand(new UseEssentialsCommands());
        registerCommand(new UseVanillaCommands());
        registerCommand(new ToggleFakePlayer());
        registerCommand(new SetStageType());
        registerCommand(new BreakSpeed());
        registerCommand(new PlaceSpeed());
        registerCommand(new ToggleMovement());
        registerCommand(new SetVelocityThreshold());
        registerCommand(new ToggleAutoCleanup());
        registerCommand(new CleanupLastStage());
        registerCommand(new Announcement());
        registerCommand(new ToggleSurvivalOnly());
        registerCommand(new ToggleFlightNoClip());
        registerCommand(new SongItem());
        registerCommand(new TestSong());

        CommandRegistrationCallback.EVENT.register(CommandManager::registerBrigadierCommands);
    }

    private static void registerCommand(Command command) {
        commands.add(command);
        commandMap.put(command.getName().toLowerCase(Locale.ROOT), command);
        commandCompletions.add(command.getName());
        for (String alias : command.getAliases()) {
            commandMap.put(alias.toLowerCase(Locale.ROOT), command);
            commandCompletions.add(alias);
        }
    }

    private static void registerBrigadierCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                                  CommandRegistryAccess registryAccess,
                                                  net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> rootCommand = net.minecraft.server.command.CommandManager.literal(COMMAND_ROOT);

        for (Command command : commands) {
            LiteralArgumentBuilder<ServerCommandSource> subCommand = net.minecraft.server.command.CommandManager.literal(command.getName())
                    .requires(source -> source.hasPermissionLevel(command.getRequiredPermissionLevel()));

            subCommand.executes(context -> executeCommand(context, command, ""));

            subCommand.then(net.minecraft.server.command.CommandManager.argument("args", StringArgumentType.greedyString())
                    .suggests((context, builder) ->
                            suggestArguments(command, "", builder))
                    .executes(context ->
                            executeCommand(context, command, StringArgumentType.getString(context, "args"))));

            rootCommand.then(subCommand);

            for (String alias : command.getAliases()) {
                LiteralArgumentBuilder<ServerCommandSource> aliasCommand = net.minecraft.server.command.CommandManager.literal(alias)
                        .requires(source -> source.hasPermissionLevel(command.getRequiredPermissionLevel()));

                aliasCommand.executes(context ->
                        executeCommand(context, command, ""));

                aliasCommand.then(net.minecraft.server.command.CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) ->
                                suggestArguments(command, "", builder))
                        .executes(context ->
                                executeCommand(context, command, StringArgumentType.getString(context, "args"))));

                rootCommand.then(aliasCommand);
            }
        }

        // Register the complete command
        dispatcher.register(rootCommand);
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context, Command command, String args) {
        try {
            boolean success = command.processCommand(args);
            if (!success) {
                if (command.getSyntax().length == 0) {
                    context.getSource().sendFeedback(() -> Text.literal("§cSyntax: /" + COMMAND_ROOT + " " + command.getName()), false);
                } else if (command.getSyntax().length == 1) {
                    context.getSource().sendFeedback(() -> Text.literal("§cSyntax: /" + COMMAND_ROOT + " " + command.getName() + " " + command.getSyntax()[0]), false);
                } else {
                    context.getSource().sendFeedback(() -> Text.literal("§cSyntax:"), false);
                    for (String syntax : command.getSyntax()) {
                        context.getSource().sendFeedback(() -> Text.literal("§c    /" + COMMAND_ROOT + " " + command.getName() + " " + syntax), false);
                    }
                }
                return 0;
            }
            return 1;
        } catch (Throwable e) {
            e.printStackTrace();
            context.getSource().sendFeedback(() -> Text.literal("§cAn error occurred while running this command: §4" + e.getMessage()), false);
            return 0;
        }
    }


    private static CompletableFuture<Suggestions> suggestArguments(Command command, String args, SuggestionsBuilder builder) {
        return command.getSuggestions(args, builder);
    }

//    /**
//     * @deprecated This method is preserved for backward compatibility. Use brigadier commands instead.
//     */
//    @Deprecated
//    public static boolean processChatMessage(String message) {
//        if (message.startsWith(NoteblockPlayer.getConfig().prefix)) {
//            String[] parts = message.substring(NoteblockPlayer.getConfig().prefix.length()).split(" ", 2);
//            String name = parts.length > 0 ? parts[0] : "";
//            String args = parts.length > 1 ? parts[1] : "";
//            Command c = commandMap.get(name.toLowerCase(Locale.ROOT));
//            if (c == null) {
//                NoteblockPlayer.addChatMessage("§cUnrecognized command");
//            } else {
//                try {
//                    boolean success = c.processCommand(args);
//                    if (!success) {
//                        if (c.getSyntax().length == 0) {
//                            NoteblockPlayer.addChatMessage("§cSyntax: " + NoteblockPlayer.getConfig().prefix + c.getName());
//                        } else if (c.getSyntax().length == 1) {
//                            NoteblockPlayer.addChatMessage("§cSyntax: " + NoteblockPlayer.getConfig().prefix + c.getName() + " " + c.getSyntax()[0]);
//                        } else {
//                            NoteblockPlayer.addChatMessage("§cSyntax:");
//                            for (String syntax : c.getSyntax()) {
//                                NoteblockPlayer.addChatMessage("§c    " + NoteblockPlayer.getConfig().prefix + c.getName() + " " + syntax);
//                            }
//                        }
//                    }
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                    NoteblockPlayer.addChatMessage("§cAn error occurred while running this command: §4" + e.getMessage());
//                }
//            }
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    /**
//     * @deprecated This method is preserved for backward compatibility. Brigadier handles suggestions internally.
//     */
//    @Deprecated
//    public static CompletableFuture<Suggestions> handleSuggestions(String text, SuggestionsBuilder suggestionsBuilder) {
//        if (!text.contains(" ")) {
//            List<String> names = commandCompletions
//                    .stream()
//                    .map((commandName) -> NoteblockPlayer.getConfig().prefix + commandName)
//                    .collect(Collectors.toList());
//            return CommandSource.suggestMatching(names, suggestionsBuilder);
//        } else {
//            String[] split = text.split(" ", 2);
//            if (split[0].startsWith(NoteblockPlayer.getConfig().prefix)) {
//                String commandName = split[0].substring(1).toLowerCase(Locale.ROOT);
//                if (commandMap.containsKey(commandName)) {
//                    return commandMap.get(commandName).getSuggestions(split.length == 1 ? "" : split[1], suggestionsBuilder);
//                }
//            }
//            return null;
//        }
//    }
}
