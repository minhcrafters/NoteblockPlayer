package me.minhcrafters.noteblockplayer.song;

public class Note implements Comparable<Note> {
    public int noteId;
    public long time;
    public int velocity;
    public int panning;
    public long duration; // Duration in milliseconds for sustain simulation

    public Note(int note, long time) {
        this.noteId = note;
        this.time = time;
        this.velocity = 100;
        this.panning = 0;
        this.duration = 500; // Default 500ms
    }

    public Note(int note, long time, int velocity) {
        this.noteId = note;
        this.time = time;
        this.velocity = velocity;
        this.panning = 0;
        this.duration = 500; // Default 500ms
    }

    public Note(int note, long time, int velocity, int panning) {
        this.noteId = note;
        this.time = time;
        this.velocity = velocity;
        this.panning = panning;
        this.duration = 500; // Default 500ms
    }

    public Note(int note, long time, int velocity, int panning, long duration) {
        this.noteId = note;
        this.time = time;
        this.velocity = velocity;
        this.panning = panning;
        this.duration = duration;
    }

    @Override
    public int compareTo(Note other) {
        if (time < other.time) {
            return -1;
        } else if (time > other.time) {
            return 1;
        } else
            return Integer.compare(noteId, other.noteId);
    }
}
