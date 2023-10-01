package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import me.minhcrafters.noteblockplayer.utils.Paginator;
import net.minecraft.text.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

public class Songs extends Command {
    public String getName() {
        return "songs";
    }

    public String[] getAliases() {
        return new String[]{"list"};
    }

    public String[] getSyntax() {
        return new String[]{CommandManager.getCommandPrefix() + "songs [subdirectory]"};
    }

    public String getDescription() {
        return "Lists available songs";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            ArrayList<Text> songs = new ArrayList<>();
            for (File songFile : Objects.requireNonNull(FileUtils.listFilesSilently(NoteblockPlayer.SONGS_DIR))
                    .map(Path::toFile)
                    .toList()) {
                if (songFile.isDirectory()) {
                    String dirName = songFile.getName();
                    for (File file : Objects.requireNonNull(FileUtils.listFilesSilently(songFile.toPath().toAbsolutePath()))
                            .map(Path::toFile)
                            .toList()) {
                        String fileName = file.getName();
                        MutableText text = Text.literal("§3" + dirName + File.separator + fileName)
                                .setStyle(Style.EMPTY
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§6Click to play §3" + fileName)))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CommandManager.getCommandPrefix() + "play " + "." + File.separator + dirName + File.separator + fileName))
                                );
                        songs.add(text);
                    }
                } else {
                    String fileName = songFile.getName();
                    MutableText text = Text.literal("§3" + fileName)
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§6Click to play §3" + fileName)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CommandManager.getCommandPrefix() + "play " + fileName))
                            );
                    songs.add(text);
                }
            }
            new Paginator(songs, "§6Available songs:", "songs", 1, 7).display();
        } else {
            int pageArgs = Integer.parseInt(args);

            ArrayList<Text> songs = new ArrayList<>();

            for (File songFile : Objects.requireNonNull(FileUtils.listFilesSilently(NoteblockPlayer.SONGS_DIR))
                    .map(Path::toFile)
                    .toList()) {
                String fileName = songFile.getName();
                MutableText text = Text.literal("§3" + fileName)
                        .setStyle(Style.EMPTY
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§6Click to play §3" + fileName)))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, CommandManager.getCommandPrefix() + "play " + fileName))
                        );
                songs.add(text);
            }

            new Paginator(songs, "§6Available songs:", "songs", pageArgs, 7).display();
        }
        return true;
    }
}
