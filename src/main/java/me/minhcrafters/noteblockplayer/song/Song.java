package me.minhcrafters.noteblockplayer.song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Song {
    private final ArrayList<Note> totalNotes = new ArrayList<>();

    public String name = "";
    public String author = "";
    public String description = "";

    public int position = 0; // Current note index
    private ArrayList<Layer> layers = new ArrayList<>();
    public boolean looping = false;
    public boolean paused = true;
    public long startTime = 0; // Start time in millis since unix epoch
    public long length = 0; // Milliseconds in the song
    public long time = 0; // Time since start of song
    public long loopPosition = 0; // Milliseconds into the song to start looping
    public int loopCount = 0; // Number of times to loop
    public int currentLoop = 0; // Number of loops so far

    public Song(String name, String author, String description) {
        this.name = name;
        this.author = author;
        this.description = description;
    }

    private void init() {
        totalNotes.clear(); // Clear existing notes to start fresh

        // Add all notes from all layers with their respective layer properties
        for (Layer layer : layers) {
            for (Note note : layer.getNotes()) {
                Note clonedNote = new Note(note.noteId, note.time, layer.velocity, layer.panning);
                totalNotes.add(clonedNote);
            }
        }

        // Sort all notes by time
        Collections.sort(totalNotes);

        // Update the song length based on the last note's time if there are any notes
        if (!totalNotes.isEmpty()) {
            length = totalNotes.getLast().time;
        }
    }

    /**
     * Starts playing song (does nothing if already playing)
     */
    public void play() {
        if (paused) {
            paused = false;
            startTime = System.currentTimeMillis() - time;
        }
    }

    /**
     * Pauses song (does nothing if already paused)
     */
    public void pause() {
        if (!paused) {
            paused = true;
            // Recalculates time so that the song will continue playing after the exact point it was paused
            advanceTime();
        }
    }

    public void reset() {
        paused = true;
        setTime(0);
        currentLoop = 0;
    }

    public void setTime(long t) {
        time = t;
        startTime = System.currentTimeMillis() - time;
        position = 0;

        ArrayList<Layer> layers = new ArrayList<>(this.layers);

        layers.sort(Comparator.comparingInt(a -> a.getNotes().size()));

        while (position < layers.getLast().getNotes().size() && layers.getLast().getNotes().get(position).time < t) {
            position++;
        }
    }

    public void advanceTime() {
        time = System.currentTimeMillis() - startTime;
    }


    public boolean finished() {
        return time > length && !shouldLoop();
    }

    public void loop() {
        position = 0;
        startTime += length - loopPosition;
        time -= length - loopPosition;

        ArrayList<Layer> layers = new ArrayList<>(this.layers);

        layers.sort(Comparator.comparingInt(a -> a.getNotes().size()));

        while (position < layers.getLast().getNotes().size() && layers.getLast().getNotes().get(position).time < loopPosition) {
            position++;
        }

        currentLoop++;
    }

    public boolean shouldLoop() {
        if (looping) {
            if (loopCount == 0) {
                return true;
            } else {
                return currentLoop < loopCount;
            }
        } else {
            return false;
        }
    }

    public boolean reachedNextNote() {
        if (position < totalNotes.size()) {
            return totalNotes.get(position).time <= time;
        } else {
            if (time > length && shouldLoop()) {
                loop();
                if (position < totalNotes.size()) {
                    return totalNotes.get(position).time <= time;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public Note getNextNote() {
        if (position >= totalNotes.size()) {
            if (shouldLoop()) {
                loop();
            } else {
                return null;
            }
        }
        return totalNotes.get(position++);
    }

    public ArrayList<Note> getTotalNotes() {
        return totalNotes;
    }

    /**
     * Adds a note directly to the song
     * This is for backward compatibility
     */
    public void add(Note note) {
        if (layers.isEmpty()) {
            // Create a default layer if none exists
            Layer defaultLayer = new Layer(this, note.velocity, note.panning);
            layers.add(defaultLayer);
        }

        // Add to the first layer
        layers.getFirst().addNote(note);
    }

    public void setLayers(ArrayList<Layer> layers) {
        this.layers = layers;
        init();
    }

    public ArrayList<Layer> getLayers() {
        return layers;
    }
}
