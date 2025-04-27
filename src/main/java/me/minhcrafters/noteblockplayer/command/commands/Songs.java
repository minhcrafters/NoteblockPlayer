package me.minhcrafters.noteblockplayer.command.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.command.CommandManager;
import me.minhcrafters.noteblockplayer.utils.Utils;
import me.minhcrafters.noteblockplayer.command.Command;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Songs extends Command {
    public String getName() {
        return "songs";
    }

    public String[] getAliases() {
        return new String[]{"list"};
    }

    public String[] getSyntax() {
        return new String[]{
                "",
                "<subdirectory>"};
    }

    public String getDescription() {
        return "Lists available songs. If an argument is provided, lists all songs in the subdirectory.";
    }

    public boolean processCommand(String args) {
        if (!args.contains(" ")) {
            Path dir;
            if (args.length() == 0) {
                dir = NoteblockPlayer.SONGS_DIR;
            } else {
                dir = NoteblockPlayer.SONGS_DIR.resolve(args);
                if (!Files.isDirectory(dir)) {
                    NoteblockPlayer.addChatMessage("§cDirectory not found");
                    return true;
                }
            }

            List<Path> allSongs = new ArrayList<>();
            try {
                // Find all songs recursively
                findSongsRecursively(dir, allSongs);
            } catch (IOException e) {
                NoteblockPlayer.addChatMessage("§cError reading folder: §4" + e.getMessage());
                return true;
            }

            if (allSongs.isEmpty()) {
                NoteblockPlayer.addChatMessage("§bNo songs found. You can put midi or nbs files in the §3songs §6folder.");
            } else {
                NoteblockPlayer.addChatMessage("§6----------------------------------------");
                NoteblockPlayer.addChatMessage("§eAll songs found in .minecraft/NoteblockPlayer/songs/" + args);
                NoteblockPlayer.addChatMessage("§6(Click on a song to play it)");

                // List all songs with clickable commands
                for (Path songPath : allSongs) {
                    // Get relative path from songs directory
                    String relativePath = NoteblockPlayer.SONGS_DIR.relativize(songPath).toString();
                    String songName = songPath.getFileName().toString();

                    // Create clickable text component for the song
                    Text songText = createClickableText(songName, "/" + CommandManager.COMMAND_ROOT + " play " + relativePath);

                    // Combine with the path information
                    Text fullText = Text.literal("")
                            .append(songText);
                            // .append(Text.literal(" §7(" + relativePath + ")"));

                    // Send the combined message
                    NoteblockPlayer.addChatMessage(fullText);
                }

                NoteblockPlayer.addChatMessage("§6----------------------------------------");
                NoteblockPlayer.addChatMessage("§eTotal songs found: §b" + allSongs.size());
                NoteblockPlayer.addChatMessage("§6----------------------------------------");
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Recursively finds all song files in a directory and its subdirectories
     *
     * @param directory The directory to search in
     * @param songsList The list to populate with found song files
     * @throws IOException If there's an error reading the directory
     */
    private void findSongsRecursively(Path directory, List<Path> songsList) throws IOException {
        Files.list(directory).forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    // Recursively search in subdirectories
                    findSongsRecursively(path, songsList);
                } else if (Files.isRegularFile(path)) {
                    // Check if it's a song file (.mid, .midi or .nbs)
                    String fileName = path.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".mid") || fileName.endsWith(".midi") || fileName.endsWith(".nbs")) {
                        songsList.add(path);
                    }
                }
            } catch (IOException e) {
                // Log the error but continue searching
                System.err.println("Error accessing path: " + path + " - " + e.getMessage());
            }
        });
    }

    /**
     * Creates a clickable text component using the direct Style and ClickEvent approach.
     * When clicked, the command specified in "command" will be executed.
     *
     * @param display The text to display
     * @param command The command to execute when clicked
     * @return A Text object with click event attached
     */
    private Text createClickableText(String display, String command) {
        Style clickableStyle = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

        return Text.literal(display).setStyle(clickableStyle);
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return Utils.giveSongDirectorySuggestions(args, suggestionsBuilder);
    }
}
