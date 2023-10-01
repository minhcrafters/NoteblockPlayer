package me.minhcrafters.noteblockplayer.utils;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FileUtils {
    public static void createDirectoriesSilently(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ignored) {
        }
    }

    public static Path resolveWithIOException(Path path, String other) throws IOException {
        try {
            return path.resolve(other);
        } catch (InvalidPathException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static CompletableFuture<Suggestions> giveSongSuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString = "";
        Path dir = NoteblockPlayer.SONGS_DIR;

        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash + 1);
            try {
                dir = resolveWithIOException(dir, dirString);
            } catch (IOException e) {
                return null;
            }
        }

        Stream<Path> songFiles = listFilesSilently(dir);

        if (songFiles == null) return null;
        ArrayList<String> suggestions = new ArrayList<>();

        for (Path path : songFiles.toList()) {
            if (Files.isRegularFile(path)) {
                suggestions.add(dirString + path.getFileName().toString());
            } else if (Files.isDirectory(path)) {
                suggestions.add(dirString + path.getFileName().toString() + "/");
            }
        }

        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static Stream<Path> listFilesSilently(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            return null;
        }
    }

    public static class LimitedSizeInputStream extends InputStream {
        private final InputStream original;
        private final long maxSize;
        private long total;

        public LimitedSizeInputStream(InputStream original, long maxSize) {
            this.original = original;
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int i = original.read();
            if (i >= 0) incrementCounter(1);
            return i;
        }

        @Override
        public int read(byte @NotNull [] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException {
            int i = original.read(b, off, len);
            if (i >= 0) incrementCounter(i);
            return i;
        }

        private void incrementCounter(int size) throws IOException {
            total += size;
            if (total > maxSize) throw new IOException("Input stream exceeded maximum size of " + maxSize + " bytes");
        }
    }
}
