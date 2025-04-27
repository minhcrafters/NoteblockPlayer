package me.minhcrafters.noteblockplayer.command.commands;

import me.minhcrafters.noteblockplayer.command.Command;
import me.minhcrafters.noteblockplayer.song.SongHandler;
import me.minhcrafters.noteblockplayer.song.Layer;
import me.minhcrafters.noteblockplayer.song.Note;
import me.minhcrafters.noteblockplayer.song.Song;

import java.util.ArrayList;
import java.util.Random;

public class TestSong extends Command {
    public String getName() {
        return "testSong";
    }

    public String[] getSyntax() {
        return new String[0];
    }

    public String getDescription() {
        return "Creates a song for testing with multiple layers, velocities, and panning values";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            Song song = new Song("test_song", "you", "A comprehensive test song with multiple layers.");

            // Create 4 layers with different velocity and panning settings
            ArrayList<Layer> layers = new ArrayList<>();

            // Layer 1: Bass with center panning and medium velocity
            Layer bassLayer = new Layer(song, 80, 100);
            for (int i = 0; i < 100; i++) {
                bassLayer.addNote(new Note(i % 25, i * 200, 80, 100));
            }
            layers.add(bassLayer);

            // Layer 2: Melody with right panning and high velocity
            Layer melodyLayer = new Layer(song, 100, 150);
            for (int i = 0; i < 120; i++) {
                melodyLayer.addNote(new Note(100 + (i % 25), i * 166, 100, 150));
            }
            layers.add(melodyLayer);

            // Layer 3: Harmony with left panning and medium-high velocity
            Layer harmonyLayer = new Layer(song, 90, 50);
            for (int i = 0; i < 80; i++) {
                harmonyLayer.addNote(new Note(150 + (i % 25), i * 250, 90, 50));
            }
            layers.add(harmonyLayer);

            // Layer 4: Random notes with varying velocities and pannings
            Layer randomLayer = new Layer(song, 70, 100);
            Random random = new Random();

            for (int i = 0; i < 100; i++) {
                int noteId = 200 + random.nextInt(50);
                int velocity = 60 + random.nextInt(41); // 60-100
                int panning = 50 + random.nextInt(101); // 50-150
                randomLayer.addNote(new Note(noteId, i * 200, velocity, panning));
            }
            layers.add(randomLayer);

            // Set the layers in the song
            song.setLayers(layers);

            // Sort the notes in each layer
            layers.forEach(Layer::sortNotes);

            // Calculate the song length based on the latest note
            long maxTime = 0;
            for (Note note : song.getTotalNotes()) {
                if (note.time > maxTime) {
                    maxTime = note.time;
                }
            }

            song.length = maxTime + 1000; // Add 1 second buffer at the end

            SongHandler.getInstance().setSong(song);
            return true;
        } else {
            return false;
        }
    }
}
