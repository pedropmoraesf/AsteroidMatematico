package br.grassinimoraes.divasteroides;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

final class EmergencySiren {
    private static final int SAMPLE_RATE = 22050;
    private final short[] pcm;
    private AudioTrack track;

    EmergencySiren() {
        pcm = buildSiren();
    }

    synchronized void play() {
        stop();
        try {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    pcm.length * 2, AudioTrack.MODE_STATIC);
            track.write(pcm, 0, pcm.length);
            track.setStereoVolume(.72f, .72f);
            track.play();
        } catch (Exception ignored) {
            stop();
        }
    }

    synchronized void stop() {
        if (track == null) return;
        try { track.stop(); } catch (Exception ignored) {}
        try { track.release(); } catch (Exception ignored) {}
        track = null;
    }

    private static short[] buildSiren() {
        final double beatSeconds = 60.0 / 80.0;
        final int beatSamples = (int) (SAMPLE_RATE * beatSeconds);
        final short[] data = new short[beatSamples * 10];
        int out = 0;
        for (int beat = 0; beat < 10; beat++) {
            double phase = 0;
            for (int i = 0; i < beatSamples; i++) {
                double t = i / (double) SAMPLE_RATE;
                double sample = 0;
                if (t < .55) {
                    double frequency = t < .38
                            ? 720.0 - 300.0 * (t / .38)
                            : 420.0 + 100.0 * ((t - .38) / .17);
                    phase += 2.0 * Math.PI * frequency / SAMPLE_RATE;
                    double fadeIn = Math.min(1.0, t / .04);
                    double fadeOut = Math.min(1.0, (.55 - t) / .06);
                    double envelope = Math.max(0.0, Math.min(fadeIn, fadeOut));
                    sample = envelope * (Math.sin(phase) + .20 * Math.sin(phase * 2.0)) * .32;
                }
                data[out++] = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 32767);
            }
        }
        return data;
    }
}
