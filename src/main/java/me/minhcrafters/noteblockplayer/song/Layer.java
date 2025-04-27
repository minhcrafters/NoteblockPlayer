package me.minhcrafters.noteblockplayer.song;

import java.util.ArrayList;
import java.util.Collections;

public class Layer {
    private final ArrayList<Note> notes = new ArrayList<>();

    public boolean[] requiredNotes = new boolean[400];
    public Song song;

    public int velocity = 100;
    public int panning = 100;

    public Layer(Song song) {
        this.song = song;
    }

    public Layer(Song song, int velocity, int panning) {
        this.song = song;
        this.velocity = velocity;
        this.panning = panning;
    }

    public void sortNotes() {
        Collections.sort(notes);
    }

    public Note getNote(int i) {
        return notes.get(i);
    }

    public ArrayList<Note> getNotes() {
        return notes;
    }

    public void addNote(Note e) {
        notes.add(e);
        requiredNotes[e.noteId] = true;
    }
}
