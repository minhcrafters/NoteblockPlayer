package me.minhcrafters.noteblockplayer.song;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.converter.MIDIConverter;
import me.minhcrafters.noteblockplayer.converter.NBSConverter;
import me.minhcrafters.noteblockplayer.utils.DownloadUtils;
import me.minhcrafters.noteblockplayer.utils.FileUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SongLoaderThread extends Thread {

    public Exception exception;
    public Song song;
    public String filename;
    private String location;
    private Path songPath;
    private URL songUrl;
    private boolean isUrl = false;

    protected SongLoaderThread() {
    }

    public SongLoaderThread(String location) throws IOException {
        this.location = location;
        if (location.startsWith("http://") || location.startsWith("https://")) {
            isUrl = true;
            songUrl = new URL(location);
        } else if (Files.exists(getSongFile(location))) {
            songPath = getSongFile(location);
        } else if (Files.exists(getSongFile(location + ".mid"))) {
            songPath = getSongFile(location + ".mid");
        } else if (Files.exists(getSongFile(location + ".midi"))) {
            songPath = getSongFile(location + ".midi");
        } else if (Files.exists(getSongFile(location + ".nbs"))) {
            songPath = getSongFile(location + ".nbs");
        } else {
            throw new IOException("Could not find file: " + location);
        }
    }

    public void run() {
        try {
            byte[] bytes;
            if (isUrl) {
                bytes = DownloadUtils.downloadToByteArray(songUrl, 10 * 1024 * 1024);
                filename = Paths.get(songUrl.toURI().getPath()).getFileName().toString();
            } else {
                bytes = Files.readAllBytes(songPath);
                filename = songPath.getFileName().toString();
            }

            try {
                song = NBSConverter.getSongFromBytes(bytes, filename);
            } catch (Exception ignored) {
            }

            if (song == null) {
                try {
                    song = MIDIConverter.getSongFromBytes(bytes, filename);
                } catch (Exception ignored) {
                }
            }

            if (song == null) {
                throw new IOException("Invalid file format");
            }

        } catch (Exception e) {
            exception = e;
        }
    }

    private Path getSongFile(String name) throws IOException {
        return FileUtils.resolveWithIOException(NoteblockPlayer.SONGS_DIR, name);
    }
}
