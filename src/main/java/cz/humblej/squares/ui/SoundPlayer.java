package cz.humblej.squares.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class SoundPlayer {
    private static final float SAMPLE_RATE = 44100.0f;
    private static volatile boolean enabled = true;

    private SoundPlayer() {
    }

    public static void gameStart() {
        playAsync(new Tone(660, 70), new Tone(880, 90));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        SoundPlayer.enabled = enabled;
    }

    public static void edge() {
        playAsync(new Tone(520, 55));
    }

    public static void square() {
        playAsync(new Tone(620, 70), new Tone(820, 90));
    }

    public static void gameOver() {
        playAsync(new Tone(880, 120), new Tone(660, 120), new Tone(520, 160));
    }

    public static void chatMessage() {
        playAsync(new Tone(1040, 55), new Tone(1320, 85));
    }

    private static void playAsync(Tone... tones) {
        if (!enabled) {
            return;
        }

        Thread thread = new Thread(() -> play(tones), "squares-sound");
        thread.setDaemon(true);
        thread.start();
    }

    private static void play(Tone... tones) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);

        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();

            for (Tone tone : tones) {
                line.write(toneBytes(tone), 0, sampleCount(tone));
            }

            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException exception) {
            // Sound is optional; the game should keep running on systems without audio output.
        }
    }

    private static byte[] toneBytes(Tone tone) {
        int samples = sampleCount(tone);
        byte[] data = new byte[samples];

        for (int i = 0; i < samples; i++) {
            double angle = (2.0 * Math.PI * i * tone.frequency) / SAMPLE_RATE;
            double envelope = 1.0 - ((double) i / samples);
            data[i] = (byte) (Math.sin(angle) * 55.0 * envelope);
        }

        return data;
    }

    private static int sampleCount(Tone tone) {
        return (int) ((tone.durationMs / 1000.0) * SAMPLE_RATE);
    }

    private static final class Tone {
        private final int frequency;
        private final int durationMs;

        private Tone(int frequency, int durationMs) {
            this.frequency = frequency;
            this.durationMs = durationMs;
        }
    }
}
