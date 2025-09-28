package me.minhcrafters.noteblockplayer.conversion;

import me.minhcrafters.noteblockplayer.song.Layer;
import me.minhcrafters.noteblockplayer.song.Note;
import me.minhcrafters.noteblockplayer.song.Song;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TxtConverter {
    public static Song getSongFromBytes(byte[] bytes, String fileName) throws IOException {
        Song song = new Song(fileName, "Unknown", "Converted from text.");
        String strContent = new String(bytes, StandardCharsets.UTF_8);

        String[] lines = strContent.split("\\r?\\n");
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String line = lines[lineNum - 1].strip();

            if (line.startsWith("#"))
                continue;

            String[] split = line.split(":");
            if (split.length != 3)
                throw new IOException("Invalid format at line " + lineNum);
            int tick, pitch, instrument;
            try {
                tick = Integer.parseInt(split[0]);
                pitch = Integer.parseInt(split[1]);
                instrument = Integer.parseInt(split[2]);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid format at line " + lineNum);
            }

            int noteId = pitch + instrument * 25;
            song.add(new Note(noteId, tick * 50L));
            song.length = song.getTotalNotes().getLast().time + 50;
        }

        song.getLayers().forEach(Layer::sortNotes);

        return song;
    }
}
