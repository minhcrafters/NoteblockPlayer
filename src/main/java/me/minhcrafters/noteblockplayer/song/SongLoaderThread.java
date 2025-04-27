package me.minhcrafters.noteblockplayer.song;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.utils.DownloadUtils;
import me.minhcrafters.noteblockplayer.utils.FileUtils;
import me.minhcrafters.noteblockplayer.conversion.MIDIConverter;
import me.minhcrafters.noteblockplayer.conversion.NBSConverter;
import me.minhcrafters.noteblockplayer.conversion.TxtConverter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SongLoaderThread extends Thread {

    private String location;
    private Path songPath;
    private URL songUrl;
    public Exception exception;
    public Song song;
    public String filename;

    private boolean isUrl = false;

    protected SongLoaderThread() {
    }

    public SongLoaderThread(String location) throws IOException {
        NoteblockPlayer.addChatMessage("Loading song from " + location);
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
            throw new IOException("Could not find song: " + location);
        }
    }

    public SongLoaderThread(Path file) {
        this.songPath = file;
    }

    public void run() {
        try {
            byte[] bytes;
            if (isUrl) {
                bytes = DownloadUtils.DownloadToByteArray(songUrl, 10 * 1024 * 1024);
                filename = Paths.get(songUrl.toURI().getPath()).getFileName().toString();
            } else {
                bytes = Files.readAllBytes(songPath);
                filename = songPath.getFileName().toString();
            }

            // Keep track of specific conversion errors
            Exception error = null;

            try {
                if (filename.endsWith(".mid") || filename.endsWith(".midi")) {
                    song = MIDIConverter.getSongFromBytes(bytes, filename);
                } else if (filename.endsWith(".nbs")) {
                    song = NBSConverter.getSongFromBytes(bytes, filename);
                } else if (filename.endsWith(".txt")) {
                    song = TxtConverter.getSongFromBytes(bytes, filename);
                }
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }

//            if (song == null) {
//                StringBuilder errorMsg = new StringBuilder("Invalid song format");
//                // Add specific error details if available
//                if (midiError != null) {
//                    errorMsg.append("\nMIDI Error: ").append(midiError.getMessage());
//                }
//                if (nbsError != null) {
//                    errorMsg.append("\nNBS Error: ").append(nbsError.getMessage());
//                }
//                if (txtError != null) {
//                    errorMsg.append("\nTXT Error: ").append(txtError.getMessage());
//                }
//                throw new IOException(errorMsg.toString());
//            }
        } catch (Exception e) {
            exception = e;
        }
    }

    private Path getSongFile(String name) throws IOException {
        return FileUtils.resolveWithIOException(NoteblockPlayer.SONGS_DIR, name);
    }
}
