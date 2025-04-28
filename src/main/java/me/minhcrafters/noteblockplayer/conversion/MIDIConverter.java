package me.minhcrafters.noteblockplayer.conversion;

import me.minhcrafters.noteblockplayer.song.*;
import me.minhcrafters.noteblockplayer.song.Instrument;
import me.minhcrafters.noteblockplayer.utils.DownloadUtils;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MIDIConverter {
    public static final int SET_INSTRUMENT = 0xC0;
    public static final int SET_TEMPO = 0x51;
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;

    public static Song getSongFromUrl(URL url) throws IOException, InvalidMidiDataException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        Sequence sequence = MidiSystem.getSequence(DownloadUtils.DownloadToInputStream(url, 5 * 1024 * 1024));
        return getSong(sequence, Paths.get(url.toURI().getPath()).getFileName().toString());
    }

    public static Song getSongFromFile(Path file) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(file.toFile());
        return getSong(sequence, file.getFileName().toString());
    }

    public static Song getSongFromBytes(byte[] bytes, String name) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(bytes));
        return getSong(sequence, name);
    }

    public static Song getSong(Sequence sequence, String name) {
        Song song = new Song(name, "Unknown", "Converted from MIDI");

        long tpq = sequence.getResolution();

        final int SET_TEMPO = 0x51;
        ArrayList<MidiEvent> tempoEvents = new ArrayList<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage mm) {
                    if (mm.getType() == SET_TEMPO) {
                        tempoEvents.add(event);
                    }
                }
            }
        }
        tempoEvents.sort(Comparator.comparingLong(MidiEvent::getTick));

        final int CONTROL_CHANGE = ShortMessage.CONTROL_CHANGE;
        final int PAN_CONTROLLER = 10;
        final int SET_INSTRUMENT = ShortMessage.PROGRAM_CHANGE;
        final int NOTE_ON = ShortMessage.NOTE_ON;
        final int NOTE_OFF = ShortMessage.NOTE_OFF;

        // Create a layer for each MIDI channel (0-15, where 9 is percussion)
        HashMap<Integer, Layer> channelLayers = new HashMap<>();
        for (int channel = 0; channel < 16; channel++) {
            // Initialize with default values, will update panning later
            channelLayers.put(channel, new Layer(song, 100, 100));
        }

        for (Track track : sequence.getTracks()) {
            long microTime = 0;
            int[] instrumentIds = new int[16];
            int[] panning = new int[16];
            Arrays.fill(panning, 64);

            int mpq = 500000;
            int tempoEventIdx = 0;
            long prevTick = 0;

            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                while (tempoEventIdx < tempoEvents.size() &&
                        event.getTick() > tempoEvents.get(tempoEventIdx).getTick()) {
                    long deltaTick = tempoEvents.get(tempoEventIdx).getTick() - prevTick;
                    prevTick = tempoEvents.get(tempoEventIdx).getTick();
                    microTime += (mpq * deltaTick) / tpq;

                    MetaMessage mm = (MetaMessage) tempoEvents.get(tempoEventIdx).getMessage();
                    byte[] data = mm.getData();
                    int new_mpq = ((data[0] & 0xFF) << 16) |
                            ((data[1] & 0xFF) << 8) |
                            (data[2] & 0xFF);
                    if (new_mpq != 0) mpq = new_mpq;
                    tempoEventIdx++;
                }

                if (message instanceof ShortMessage sm) {
                    int channel = sm.getChannel();
                    Layer layer = channelLayers.get(channel);

                    switch (sm.getCommand()) {
                        case CONTROL_CHANGE:
                            if (sm.getData1() == PAN_CONTROLLER) {
                                int newPanning = ((sm.getData2() * 200) / 127) - 100;
                                panning[channel] = newPanning;
                                layer.panning = newPanning;
                            }
                            break;

                        case SET_INSTRUMENT:
                            instrumentIds[channel] = sm.getData1();
                            break;

                        case NOTE_ON:
                            int pitch = sm.getData1();
                            int velocity = sm.getData2();
                            if (velocity == 0) break;
                            velocity = (velocity * 100) / 127;

                            // Update layer velocity if needed
                            layer.velocity = velocity;

                            long deltaTickOn = event.getTick() - prevTick;
                            prevTick = event.getTick();
                            microTime += (mpq * deltaTickOn) / tpq;

                            Note note;
                            if (channel == 9) {
                                note = getMidiPercussionNote(pitch, velocity, panning[channel], microTime);
                            } else {
                                note = getMidiInstrumentNote(instrumentIds[channel], pitch, velocity, panning[channel], microTime);
                            }

                            if (note != null) {
                                // Add note to the appropriate layer instead of directly to the song
                                layer.addNote(note);
                            }

                            long timeOn = microTime / 1000L;
                            if (timeOn > song.length) song.length = timeOn;
                            break;

                        case NOTE_OFF:
                            long deltaTickOff = event.getTick() - prevTick;
                            prevTick = event.getTick();
                            microTime += (mpq * deltaTickOff) / tpq;
                            long timeOff = microTime / 1000L;
                            if (timeOff > song.length) song.length = timeOff;
                            break;
                    }
                }
            }
        }

        // Remove empty layers
        ArrayList<Layer> layers = new ArrayList<>();
        for (Layer layer : channelLayers.values()) {
            if (!layer.getNotes().isEmpty()) {
                layers.add(layer);
            }
        }

        // Set the layers in the song
        song.setLayers(layers);

        // Sort notes in each layer
        song.getLayers().forEach(Layer::sortNotes);

        if (!song.getTotalNotes().isEmpty()) {
            long shift = song.getTotalNotes().getFirst().time - 1000;
            if (song.getTotalNotes().getFirst().time > 1000) {
                for (Note note : song.getTotalNotes()) {
                    note.time -= shift;
                }
                song.length -= shift;
            }
        }

        return song;
    }


    public static Note getMidiInstrumentNote(int midiInstrument, int midiPitch, int velocity, int panning, long microTime) {
        me.minhcrafters.noteblockplayer.song.Instrument instrument = null;
        me.minhcrafters.noteblockplayer.song.Instrument[] instrumentList = instrumentMap.get(midiInstrument);
        if (instrumentList != null) {
            for (me.minhcrafters.noteblockplayer.song.Instrument candidateInstrument : instrumentList) {
                if (midiPitch >= candidateInstrument.offset && midiPitch <= candidateInstrument.offset + 24) {
                    instrument = candidateInstrument;
                    break;
                }
            }
        }

        if (instrument == null) {
            return null;
        }

        int pitch = midiPitch - instrument.offset;
        int noteId = pitch + instrument.instrumentId * 25;
        long time = microTime / 1000L;

        return new Note(noteId, time, velocity, panning);
    }

    private static Note getMidiPercussionNote(int midiPitch, int velocity, int panning, long microTime) {
        if (percussionMap.containsKey(midiPitch)) {
            int noteId = percussionMap.get(midiPitch);
            long time = microTime / 1000L;

            return new Note(noteId, time, velocity, panning);
        }
        return null;
    }

    public static HashMap<Integer, me.minhcrafters.noteblockplayer.song.Instrument[]> instrumentMap = new HashMap<>();

    static {
        // Piano (HARP BASS BELL)
        instrumentMap.put(0, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Acoustic Grand Piano
        instrumentMap.put(1, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Bright Acoustic Piano
        instrumentMap.put(2, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Grand Piano
        instrumentMap.put(3, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Honky-tonk Piano
        instrumentMap.put(4, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Piano 1
        instrumentMap.put(5, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Piano 2
        instrumentMap.put(6, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Harpsichord
        instrumentMap.put(7, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Clavinet

        // Chromatic Percussion (IRON_XYLOPHONE XYLOPHONE BASS)
        instrumentMap.put(8, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Celesta
        instrumentMap.put(9, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Glockenspiel
        instrumentMap.put(10, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Music Box
        instrumentMap.put(11, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Vibraphone
        instrumentMap.put(12, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Marimba
        instrumentMap.put(13, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Xylophone
        instrumentMap.put(14, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Tubular Bells
        instrumentMap.put(15, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Dulcimer

        // Organ (BIT DIDGERIDOO BELL)
        instrumentMap.put(16, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Drawbar Organ
        instrumentMap.put(17, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Percussive Organ
        instrumentMap.put(18, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Rock Organ
        instrumentMap.put(19, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Church Organ
        instrumentMap.put(20, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Reed Organ
        instrumentMap.put(21, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Accordian
        instrumentMap.put(22, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Harmonica
        instrumentMap.put(23, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Tango Accordian

        // Guitar (BIT DIDGERIDOO BELL)
        instrumentMap.put(24, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Acoustic Guitar (nylon)
        instrumentMap.put(25, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Acoustic Guitar (steel)
        instrumentMap.put(26, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Guitar (jazz)
        instrumentMap.put(27, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Guitar (clean)
        instrumentMap.put(28, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Guitar (muted)
        instrumentMap.put(29, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Overdriven Guitar
        instrumentMap.put(30, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Distortion Guitar
        instrumentMap.put(31, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Guitar Harmonics

        // Bass
        instrumentMap.put(32, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Acoustic Bass
        instrumentMap.put(33, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Bass (finger)
        instrumentMap.put(34, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Electric Bass (pick)
        instrumentMap.put(35, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Fretless Bass
        instrumentMap.put(36, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Slap Bass 1
        instrumentMap.put(37, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Slap Bass 2
        instrumentMap.put(38, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Synth Bass 1
        instrumentMap.put(39, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE}); // Synth Bass 2

        // Strings
        instrumentMap.put(40, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Violin
        instrumentMap.put(41, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Viola
        instrumentMap.put(42, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Cello
        instrumentMap.put(43, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.GUITAR, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Contrabass
        instrumentMap.put(44, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Tremolo Strings
        instrumentMap.put(45, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Pizzicato Strings
        instrumentMap.put(46, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.CHIME}); // Orchestral Harp
        instrumentMap.put(47, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Timpani

        // Ensemble
        instrumentMap.put(48, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // String Ensemble 1
        instrumentMap.put(49, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // String Ensemble 2
        instrumentMap.put(50, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Synth Strings 1
        instrumentMap.put(51, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Synth Strings 2
        instrumentMap.put(52, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Choir Aahs
        instrumentMap.put(53, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Voice Oohs
        instrumentMap.put(54, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Synth Choir
        instrumentMap.put(55, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL}); // Orchestra Hit

        // Brass
        instrumentMap.put(56, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(57, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(58, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(59, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(60, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(61, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(62, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(63, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Reed
        instrumentMap.put(64, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(65, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(66, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(67, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(68, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(69, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(70, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(71, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Pipe
        instrumentMap.put(72, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(73, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(74, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(75, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(76, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(77, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(78, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(79, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.FLUTE, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Synth Lead
        instrumentMap.put(80, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(81, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(82, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(83, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(84, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(85, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(86, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(87, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Synth Pad
        instrumentMap.put(88, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(89, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(90, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(91, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(92, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(93, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(94, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(95, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Synth Effects
//		instrumentMap.put(96, new Instrument[]{});
//		instrumentMap.put(97, new Instrument[]{});
        instrumentMap.put(98, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BIT, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(99, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(100, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(101, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(102, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(103, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Ethnic
        instrumentMap.put(104, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BANJO, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(105, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BANJO, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(106, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BANJO, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(107, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BANJO, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(108, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.BANJO, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(109, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(110, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});
        instrumentMap.put(111, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.HARP, me.minhcrafters.noteblockplayer.song.Instrument.DIDGERIDOO, me.minhcrafters.noteblockplayer.song.Instrument.BELL});

        // Percussive
        instrumentMap.put(112, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(113, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(114, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(115, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(116, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(117, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(118, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
        instrumentMap.put(119, new me.minhcrafters.noteblockplayer.song.Instrument[]{me.minhcrafters.noteblockplayer.song.Instrument.IRON_XYLOPHONE, me.minhcrafters.noteblockplayer.song.Instrument.BASS, me.minhcrafters.noteblockplayer.song.Instrument.XYLOPHONE});
    }

    public static HashMap<Integer, Integer> percussionMap = new HashMap<>();

    static {
        percussionMap.put(35, 10 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(36, 6 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(37, 6 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(38, 8 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(39, 6 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(40, 4 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(41, 6 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(42, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(43, 13 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(44, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(45, 15 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(46, 18 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(47, 20 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(48, 23 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(49, 17 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(50, 23 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(51, 24 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(52, 8 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(53, 13 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(54, 18 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(55, 18 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(56, 1 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(57, 13 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(58, 2 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(59, 13 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(60, 9 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(61, 2 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(62, 8 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(63, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(64, 15 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(65, 13 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(66, 8 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(67, 8 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(68, 3 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(69, 20 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(70, 23 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(71, 24 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(72, 24 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(73, 17 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(74, 11 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(75, 18 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(76, 9 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(77, 5 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(78, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(79, 19 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(80, 17 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(81, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(82, 22 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.SNARE.instrumentId);
        percussionMap.put(83, 24 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.CHIME.instrumentId);
        percussionMap.put(84, 24 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.CHIME.instrumentId);
        percussionMap.put(85, 21 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.HAT.instrumentId);
        percussionMap.put(86, 14 + 25 * me.minhcrafters.noteblockplayer.song.Instrument.BASEDRUM.instrumentId);
        percussionMap.put(87, 7 + 25 * Instrument.BASEDRUM.instrumentId);
    }
}