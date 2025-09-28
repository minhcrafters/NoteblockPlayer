package me.minhcrafters.noteblockplayer.utils;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class Utils {
    static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Helper method to recursively walk through all files in a directory and its
     * subdirectories
     * 
     * @param rootDir      The root directory to start walking from
     * @param relativePath The current relative path from the root directory
     * @param resultList   The list to add song paths to
     */
    private static void findAllSongPaths(Path rootDir, String relativePath, List<String> resultList) {
        try {
            try (Stream<Path> entries = Files.list(rootDir)) {
                for (Path entry : entries.toList()) {
                    String relPath = relativePath.isEmpty() ? entry.getFileName().toString()
                            : relativePath + entry.getFileName().toString();

                    if (Files.isRegularFile(entry)) {
                        resultList.add(relPath);
                    } else if (Files.isDirectory(entry)) {
                        // Add the directory with a trailing slash
                        resultList.add(relPath + "/");
                        // Recursively search subdirectories
                        findAllSongPaths(entry, relPath + "/", resultList);
                    }
                }
            }
        } catch (IOException e) {
            // Silently handle any IO exceptions
        }
    }

    public static CompletableFuture<Suggestions> giveSongSuggestions(String arg,
            SuggestionsBuilder suggestionsBuilder) {
        // Handle empty input case
        if (arg == null || arg.isEmpty()) {
            try {
                ArrayList<String> suggestionsList = new ArrayList<>();
                // Find all song paths in the songs directory and its subdirectories
                findAllSongPaths(NoteblockPlayer.SONGS_DIR, "", suggestionsList);

                return CommandSource.suggestMatching(suggestionsList, suggestionsBuilder);
            } catch (Exception e) {
                return suggestionsBuilder.buildFuture();
            }
        }

        // The current input might contain a partial file/directory name
        int lastSlash = arg.lastIndexOf("/");
        String dirPart = "";
        String filePart = arg;

        // Split into directory and file parts
        if (lastSlash >= 0) {
            dirPart = arg.substring(0, lastSlash + 1); // Include trailing slash
            filePart = lastSlash + 1 < arg.length() ? arg.substring(lastSlash + 1) : "";
        }

        // Resolve the directory path
        Path dir = NoteblockPlayer.SONGS_DIR;
        if (!dirPart.isEmpty()) {
            try {
                dir = FileUtils.resolveWithIOException(dir, dirPart);
            } catch (IOException e) {
                return suggestionsBuilder.buildFuture();
            }
        }

        // Build list of suggestions
        ArrayList<String> suggestionsList = new ArrayList<>();

        if (filePart.isEmpty()) {
            // If we're just at a directory with no partial filename yet,
            // list everything in this directory and its subdirectories
            findAllSongPaths(dir, dirPart, suggestionsList);
        } else {
            // If we have a partial filename, show matches from current directory
            try {
                try (Stream<Path> songFiles = Files.list(dir)) {
                    for (Path path : songFiles.toList()) {
                        String fileName = path.getFileName().toString();

                        // Check if this file/directory matches our partial input
                        if (fileName.toLowerCase().startsWith(filePart.toLowerCase())) {
                            if (Files.isRegularFile(path)) {
                                suggestionsList.add(dirPart + fileName);
                            } else if (Files.isDirectory(path)) {
                                String newDirPath = dirPart + fileName + "/";
                                suggestionsList.add(newDirPath);

                                // Also add files from matching subdirectories
                                findAllSongPaths(path, newDirPath, suggestionsList);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Just continue with any suggestions we may have found
            }
        }

        // Replace the current partial argument with the suggestions
        return CommandSource.suggestMatching(suggestionsList, suggestionsBuilder);
    }

    public static CompletableFuture<Suggestions> givePlaylistSuggestions(SuggestionsBuilder suggestionsBuilder) {
        if (!Files.exists(NoteblockPlayer.PLAYLISTS_DIR))
            return null;
        try {
            return CommandSource.suggestMatching(
                    Files.list(NoteblockPlayer.PLAYLISTS_DIR)
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString),
                    suggestionsBuilder);
        } catch (IOException e) {
            return null;
        }
    }

    public static CompletableFuture<Suggestions> giveSongDirectorySuggestions(String arg,
            SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString;
        Path dir = NoteblockPlayer.SONGS_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash + 1);
            try {
                dir = FileUtils.resolveWithIOException(dir, dirString);
            } catch (IOException e) {
                return null;
            }
        } else {
            dirString = "";
        }

        Stream<Path> songFiles;
        try {
            songFiles = Files.list(dir);
        } catch (IOException e) {
            return null;
        }

        int clipStart;
        if (arg.contains(" ")) {
            clipStart = arg.lastIndexOf(" ") + 1;
        } else {
            clipStart = 0;
        }

        Stream<String> suggestions = songFiles
                .filter(Files::isDirectory)
                .map(path -> dirString + path.getFileName().toString() + "/")
                .filter(str -> str.startsWith(arg))
                .map(str -> str.substring(clipStart));
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static MutableText getStyledText(String str, Style style) {
        MutableText text = MutableText.of(PlainTextContent.of(str));
        text.setStyle(style);
        return text;
    }

    public static void setItemName(ItemStack stack, Text text) {
        stack.set(DataComponentTypes.CUSTOM_NAME, text);
    }

    public static void setItemLore(ItemStack stack, Text... loreLines) {
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(loreLines)));
    }

    public static MutableText joinTexts(MutableText base, Text... children) {
        if (base == null) {
            base = Text.empty();
        }
        for (Text child : children) {
            base.append(child);
        }
        return base;
    }

    public static String getWorldName() {
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static String getServerIdentifier() {
        if (mc.isInSingleplayer())
            return "local;" + mc.getServer().getSavePath(WorldSavePath.ROOT).getParent().getFileName().toString();
        else
            return "remote;" + mc.getCurrentServerEntry().address;
    }
}
