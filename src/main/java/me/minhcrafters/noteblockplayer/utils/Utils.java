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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    static final MinecraftClient mc = MinecraftClient.getInstance();

    public static CompletableFuture<Suggestions> giveSongSuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString = "";
        Path dir = NoteblockPlayer.SONGS_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash + 1);
            try {
                dir = FileUtils.resolveWithIOException(dir, dirString);
            } catch (IOException e) {
                return null;
            }
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

        ArrayList<String> suggestionsList = new ArrayList<>();
        for (Path path : songFiles.collect(Collectors.toList())) {
            if (Files.isRegularFile(path)) {
                suggestionsList.add(dirString + path.getFileName().toString());
            } else if (Files.isDirectory(path)) {
                suggestionsList.add(dirString + path.getFileName().toString() + "/");
            }
        }
        Stream<String> suggestions = suggestionsList.stream()
                .filter(str -> str.startsWith(arg))
                .map(str -> str.substring(clipStart));
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static CompletableFuture<Suggestions> givePlaylistSuggestions(SuggestionsBuilder suggestionsBuilder) {
        if (!Files.exists(NoteblockPlayer.PLAYLISTS_DIR)) return null;
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

    public static CompletableFuture<Suggestions> giveSongDirectorySuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
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
        else return "remote;" + mc.getCurrentServerEntry().address;
    }
}
