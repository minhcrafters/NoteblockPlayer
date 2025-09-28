package me.minhcrafters.noteblockplayer.conversion;

import me.minhcrafters.noteblockplayer.NoteblockPlayer;
import me.minhcrafters.noteblockplayer.song.Instrument;
import me.minhcrafters.noteblockplayer.song.Layer;
import me.minhcrafters.noteblockplayer.song.Note;
import me.minhcrafters.noteblockplayer.song.Song;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class NBSConverter {
    public static Instrument[] instrumentIndex = new Instrument[] {
            Instrument.HARP,
            Instrument.BASS,
            Instrument.BASEDRUM,
            Instrument.SNARE,
            Instrument.HAT,
            Instrument.GUITAR,
            Instrument.FLUTE,
            Instrument.BELL,
            Instrument.CHIME,
            Instrument.XYLOPHONE,
            Instrument.IRON_XYLOPHONE,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.BIT,
            Instrument.BANJO,
            Instrument.PLING,
    };

    private static class NBSNote {
        public int tick;
        public short layer;
        public byte instrument;
        public byte key;
        public byte velocity = 100;
        public byte panning = 100;
        public short pitch = 0;
    }

    private static class NBSLayer {
        public String name;
        public byte lock = 0;
        public byte volume;
        public byte stereo = 100;
    }

    public static Song getSongFromBytes(byte[] bytes, String fileName) throws IOException {
        if (bytes == null || bytes.length < 2) {
            throw new IOException("Invalid NBS file: File too small");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            short songLength = 0;
            byte format = 0;
            byte vanillaInstrumentCount = 0;
            songLength = buffer.getShort(); // If it's not 0, then it uses the old format
            if (songLength == 0) {
                if (!buffer.hasRemaining()) {
                    throw new IOException("Invalid NBS file: Unexpected end of file while reading format");
                }
                format = buffer.get();
            }

            if (format >= 1) {
                vanillaInstrumentCount = buffer.get();
            }
            if (format >= 3) {
                songLength = buffer.getShort();
            }

            short layerCount = buffer.getShort();
            String songName = getString(buffer, bytes.length);
            String songAuthor = getString(buffer, bytes.length);
            String songOriginalAuthor = getString(buffer, bytes.length);
            String songDescription = getString(buffer, bytes.length);
            short tempo = buffer.getShort();
            byte autoSaving = buffer.get();
            byte autoSavingDuration = buffer.get();
            byte timeSignature = buffer.get();
            int minutesSpent = buffer.getInt();
            int leftClicks = buffer.getInt();
            int rightClicks = buffer.getInt();
            int blocksAdded = buffer.getInt();
            int blocksRemoved = buffer.getInt();
            String origFileName = getString(buffer, bytes.length);

            byte loop = 0;
            byte maxLoopCount = 0;
            short loopStartTick = 0;
            if (format >= 4) {
                loop = buffer.get();
                maxLoopCount = buffer.get();
                loopStartTick = buffer.getShort();
            }

            ArrayList<NBSNote> nbsNotes = new ArrayList<>();
            short tick = -1;
            while (true) {
                int tickJumps = buffer.getShort();
                if (tickJumps == 0)
                    break;
                tick += (short) tickJumps;

                short layer = -1;
                while (true) {
                    int layerJumps = buffer.getShort();
                    if (layerJumps == 0)
                        break;
                    layer += (short) layerJumps;
                    NBSNote note = new NBSNote();
                    note.tick = tick;
                    note.layer = layer;
                    note.instrument = buffer.get();
                    note.key = buffer.get();
                    if (format >= 4) {
                        note.velocity = buffer.get();
                        note.panning = buffer.get();
                        note.pitch = buffer.getShort();
                    }
                    nbsNotes.add(note);
                }
            }

            ArrayList<NBSLayer> nbsLayers = new ArrayList<>();
            if (buffer.hasRemaining()) {
                for (int i = 0; i < layerCount; i++) {
                    NBSLayer layer = new NBSLayer();
                    layer.name = getString(buffer, bytes.length);
                    if (format >= 4) {
                        layer.lock = buffer.get();
                    }
                    layer.volume = buffer.get();
                    if (format >= 2) {
                        layer.stereo = buffer.get();
                    }
                    nbsLayers.add(layer);
                }
            }

            Song song = new Song(!songName.trim().isEmpty() ? songName : fileName, songAuthor, songDescription);

            if (loop > 0) {
                song.looping = true;
                song.loopPosition = getMilliTime(loopStartTick, tempo);
                song.loopCount = maxLoopCount;
            }

            // Create initial layers from NBSLayers
            ArrayList<Layer> initialLayers = new ArrayList<>();
            for (int i = 0; i < Math.max(layerCount, nbsLayers.size()); i++) {
                byte layerVolume = 100;
                byte layerStereo = 100;

                // Get volume and stereo settings from NBSLayer if available
                if (i < nbsLayers.size()) {
                    NBSLayer nbsLayer = nbsLayers.get(i);
                    layerVolume = nbsLayer.volume;
                    layerStereo = (byte) (nbsLayer.stereo - 100);
                }

                // Create a new Layer with song reference and settings
                Layer layer = new Layer(song, layerVolume, layerStereo);
                boolean hasNotes = false;

                // Add all notes that belong to this layer
                for (NBSNote nbsNote : nbsNotes) {
                    if (nbsNote.layer == i) {
                        hasNotes = true;
                        // Create a Note from NBSNote
                        Instrument instrument;
                        // Make sure the instrument index is within valid range
                        if (nbsNote.instrument >= 0 && nbsNote.instrument < instrumentIndex.length) {
                            instrument = instrumentIndex[nbsNote.instrument];
                        } else {
                            // Default to HARP for out-of-range instruments
                            instrument = Instrument.HARP;
                        }

                        while (nbsNote.key < 33) {
                            nbsNote.key += 12;
                        }
                        while (nbsNote.key > 57) {
                            nbsNote.key -= 12;
                        }

                        int pitch = nbsNote.key - 33;
                        int noteId = instrument.instrumentId * 25 + pitch;

                        // Create a new Note with the tick converted to milliseconds
                        Note note = new Note(
                                noteId,
                                getMilliTime(nbsNote.tick, tempo),
                                nbsNote.velocity,
                                nbsNote.panning - 100);

                        layer.addNote(note);
                    }
                }

                if (hasNotes) {
                    // Only add the layer if it has notes
                    layer.sortNotes();
                    initialLayers.add(layer);
                }
            }

            // Condense layers that have fewer notes than average
            ArrayList<Layer> condensedLayers = condenseLayers(initialLayers);

            // Set the layers to the song
            song.setLayers(condensedLayers);

            return song;
        } catch (BufferUnderflowException e) {
            throw new IOException("Invalid NBS file: Unexpected end of file - " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new IOException("Invalid NBS file: Index out of bounds - " + e.getMessage()
                    + ". This may be caused by an instrument ID that exceeds the supported range (0-"
                    + (instrumentIndex.length - 1) + ", probable cause: custom instruments)");
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to parse NBS file: " + e.getMessage(), e);
        }
    }

    private static String getString(ByteBuffer buffer, int maxSize) throws IOException {
        if (!buffer.hasRemaining()) {
            return "";
        }

        int length = buffer.getInt();
        if (length < 0 || length > maxSize) {
            throw new IOException("Invalid string length: " + length);
        }

        if (length == 0) {
            return "";
        }

        if (buffer.remaining() < length) {
            throw new IOException(
                    "Buffer underflow: requested " + length + " bytes but only " + buffer.remaining() + " available");
        }

        byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr);
    }

    private static int getMilliTime(int tick, int tempo) {
        return 1000 * tick * 100 / tempo;
    }

    private static ArrayList<Layer> condenseLayers(ArrayList<Layer> originalLayers) {
        if (originalLayers.size() <= 1) {
            return originalLayers; // No condensation needed if there's 0 or 1 layer
        }

        // Get max spacing from config
        int maxNoteSpacing = NoteblockPlayer.getConfig().maxNoteSpacing;

        // Initialize a list for the final condensed layers
        ArrayList<Layer> condensedLayers = new ArrayList<>();

        // Get all notes from all layers and sort them by position
        ArrayList<NoteWithLayerInfo> allNotes = new ArrayList<>();
        for (int layerIdx = 0; layerIdx < originalLayers.size(); layerIdx++) {
            Layer layer = originalLayers.get(layerIdx);
            for (Note note : layer.getNotes()) {
                allNotes.add(new NoteWithLayerInfo(note, layer, layerIdx));
            }
        }

        // Sort all notes by their position (time)
        allNotes.sort(Comparator.comparingLong(a -> a.note.time));

        if (allNotes.isEmpty()) {
            return originalLayers; // No notes to process
        }

        // Group notes into clusters based on temporal proximity
        ArrayList<ArrayList<NoteWithLayerInfo>> clusters = new ArrayList<>();
        ArrayList<NoteWithLayerInfo> currentCluster = new ArrayList<>();
        currentCluster.add(allNotes.getFirst());

        for (int i = 1; i < allNotes.size(); i++) {
            NoteWithLayerInfo prevNote = allNotes.get(i - 1);
            NoteWithLayerInfo currNote = allNotes.get(i);

            long timeDifference = currNote.note.time - prevNote.note.time;

            if (timeDifference <= maxNoteSpacing) {
                // Notes are close enough, add to the current cluster
                currentCluster.add(currNote);
            } else {
                // Start a new cluster
                clusters.add(currentCluster);
                currentCluster = new ArrayList<>();
                currentCluster.add(currNote);
            }
        }

        // Add the last cluster if it has notes
        if (!currentCluster.isEmpty()) {
            clusters.add(currentCluster);
        }

        // Process each cluster
        for (ArrayList<NoteWithLayerInfo> cluster : clusters) {
            // Calculate how many layers we need for this cluster
            // based on how many notes need to be played simultaneously

            // Group notes by exact position
            java.util.Map<Integer, ArrayList<NoteWithLayerInfo>> notesByPosition = new java.util.HashMap<>();
            for (NoteWithLayerInfo noteInfo : cluster) {
                int position = (int) noteInfo.note.time;
                if (!notesByPosition.containsKey(position)) {
                    notesByPosition.put(position, new ArrayList<>());
                }
                notesByPosition.get(position).add(noteInfo);
            }

            // Find the maximum number of simultaneous notes needed
            int maxSimultaneousNotes = 0;
            for (ArrayList<NoteWithLayerInfo> notesAtPosition : notesByPosition.values()) {
                maxSimultaneousNotes = Math.max(maxSimultaneousNotes, notesAtPosition.size());
            }

            // Create the layers needed for this cluster
            ArrayList<Layer> clusterLayers = new ArrayList<>();
            for (int i = 0; i < maxSimultaneousNotes; i++) {
                // Use properties of the first note in the cluster for the new layer
                NoteWithLayerInfo firstNoteInfo = cluster.getFirst();
                Layer originalLayer = firstNoteInfo.sourceLayer;
                Layer newLayer = new Layer(
                        originalLayer.song,
                        originalLayer.velocity,
                        originalLayer.panning);
                clusterLayers.add(newLayer);
            }

            // Distribute notes from this cluster to the new layers
            for (int position : notesByPosition.keySet()) {
                ArrayList<NoteWithLayerInfo> notesAtThisPosition = notesByPosition.get(position);

                for (int i = 0; i < notesAtThisPosition.size(); i++) {
                    // Add each note to a different layer to avoid overlaps
                    clusterLayers.get(i).addNote(notesAtThisPosition.get(i).note);
                }
            }

            // Add the cluster layers to the final result
            for (Layer layer : clusterLayers) {
                layer.sortNotes();
                condensedLayers.add(layer);
            }
        }

        return condensedLayers;
    }

    // Helper class to keep track of note, source layer, and layer index
    private static class NoteWithLayerInfo {
        Note note;
        Layer sourceLayer;
        int layerIndex;

        NoteWithLayerInfo(Note note, Layer sourceLayer, int layerIndex) {
            this.note = note;
            this.sourceLayer = sourceLayer;
            this.layerIndex = layerIndex;
        }
    }
}
