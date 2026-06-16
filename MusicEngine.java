

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.File;

public class MusicEngine {
    private Synthesizer synth;
    private MidiChannel[] channels;
    private boolean midiReady = false;

    private Clip bgClip = null;
    private boolean hasAudio = false;
    private long pausePosition = 0; // untuk resume

    private static final int[][] SONG_SCALES = {
        {60,62,65,67,69,72,74},
        {62,64,67,69,71,74,76},
        {64,65,67,69,71,72,74},
        {60,63,65,67,70,72,75},
        {61,63,66,68,70,73,75},
        {62,65,67,69,72,74,77},
    };
    private static final int[] INSTRUMENTS = {1,4,25,11,0,40};
    private int[] scale;
    private int noteIdx = 0;

    public MusicEngine() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channels = synth.getChannels();
            midiReady = true;
        } catch (Exception e) { midiReady = false; }
    }

    public void setSong(int idx, String songTitle) {
        this.scale = SONG_SCALES[idx % SONG_SCALES.length];
        this.noteIdx = 0;
        if (midiReady) {
            try {
                channels[0].programChange(INSTRUMENTS[idx % INSTRUMENTS.length]);
                channels[1].programChange(33);
            } catch (Exception ignored) {}
        }
        stopAudio();
        tryLoadAudio(idx, songTitle, 0);
    }

    public void setSongResume(int idx, String songTitle, long position) {
        this.scale = SONG_SCALES[idx % SONG_SCALES.length];
        this.noteIdx = 0;
        stopAudio();
        tryLoadAudio(idx, songTitle, position);
    }

    private void tryLoadAudio(int idx, String title, long resumePos) {
        String[] attempts = {
            "music/" + (idx+1) + ".wav",
            "music/" + title.toLowerCase().replace(" ","_") + ".wav",
        };
        for (String path : attempts) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                    bgClip = AudioSystem.getClip();
                    bgClip.open(ais);
                    if (resumePos > 0) bgClip.setMicrosecondPosition(resumePos);
                    try {
                        FloatControl vol = (FloatControl) bgClip.getControl(FloatControl.Type.MASTER_GAIN);
                        vol.setValue(-3f);
                    } catch (Exception ignored) {}
                    bgClip.start();
                    hasAudio = true;
                    System.out.println("Memutar" + (resumePos>0?" (lanjut)":"") + ": " + path);
                    return;
                } catch (Exception e) {
                    System.out.println("Gagal load " + path + ": " + e.getMessage());
                }
            }
        }
        hasAudio = false;
    }

    /** Simpan posisi lagu saat ini, lalu pause */
    public long pauseAudio() {
        if (bgClip != null && bgClip.isRunning()) {
            pausePosition = bgClip.getMicrosecondPosition();
            bgClip.stop();
        }
        return pausePosition;
    }

    public long getAudioPosition() {
        if (bgClip != null) return bgClip.getMicrosecondPosition();
        return 0;
    }

    public boolean hasAudio() { return hasAudio; }

    public boolean isAudioFinished() {
        if (bgClip != null) return !bgClip.isRunning() && bgClip.getMicrosecondPosition() >= bgClip.getMicrosecondLength() - 500000;
        // Kalau ga ada audio, estimasi dari waktu (120 detik)
        return false;
    }

    public void stopAudio() {
        if (bgClip != null) { bgClip.stop(); bgClip.close(); bgClip = null; }
        hasAudio = false; pausePosition = 0;
    }

    public void playHitNote(int col) {
        // Matiin MIDI kalau ada lagu asli
        if (hasAudio || !midiReady) return;
        try {
            int note = scale[(noteIdx + col) % scale.length];
            channels[0].noteOn(note, 85);
            final int n = note;
            new Thread(() -> {
                try { Thread.sleep(180); channels[0].noteOff(n); } catch (Exception ignored) {}
            }).start();
            noteIdx = (noteIdx+1) % scale.length;
        } catch (Exception ignored) {}
    }

    public void playMissSound() {
        if (hasAudio || !midiReady) return;
        try { channels[9].noteOn(38, 70); } catch (Exception ignored) {}
    }

    public void playPerfectSound() {
        if (hasAudio || !midiReady) return;
        try {
            int note = scale[scale.length-1]+12;
            channels[0].noteOn(note, 100);
            final int n = note;
            new Thread(() -> {
                try { Thread.sleep(250); channels[0].noteOff(n); } catch (Exception ignored) {}
            }).start();
        } catch (Exception ignored) {}
    }

    public void close() {
        stopAudio();
        if (midiReady && synth != null) synth.close();
    }
}
