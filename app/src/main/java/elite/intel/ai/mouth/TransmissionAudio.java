package elite.intel.ai.mouth;

import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;

/** Optional treatment of a whole radio or away-from-ship VEGA utterance at 24 kHz mono PCM-16 LE. */
public final class TransmissionAudio {
    public record Options(boolean tones, boolean degradation) {
        public static final Options NONE = new Options(false, false);
    }

    private static final int SAMPLE_RATE = 24_000;
    private static final int TONE_SAMPLES = SAMPLE_RATE / 4;
    private static final int GAP_SAMPLES = SAMPLE_RATE * 35 / 1000;
    private static final int FADE_SAMPLES = SAMPLE_RATE * 5 / 1000;
    private static final double PRE_FILTER_DRIVE = 2.0;
    private static final double OUTPUT_DRIVE = 2.5;

    private TransmissionAudio() { }

    public static Options forRequest(VocalisationRequestEvent event) {
        SystemSession settings = SystemSession.getInstance();
        boolean eligible = (event.isRadio() && settings.isEffectsOnRadio())
                || (event.isVegaSpeech() && settings.isEffectsOnVegaAway()
                && (Status.getInstance().isOnFoot() || Status.getInstance().isInSrv()));
        return eligible ? new Options(settings.isTransmissionTones(), settings.isEnhancedRadioEffect()) : Options.NONE;
    }

    /** Apply enhanced processing when selected, or the standard filter to radio speech. */
    public static void processVoice(byte[] pcm, boolean radio, Options options) {
        if (options.degradation()) degrade(pcm);
        else if (radio) RadioFilter.apply(pcm);
    }

    /** Gentle pre-filter harmonics, a higher voice band and controlled dynamics; no synthetic static. */
    public static void degrade(byte[] pcm) {
        Biquad highPass = Biquad.highPass(1200);
        Biquad lowPass = Biquad.lowPass(7000);
        Biquad presence = Biquad.presence(4000, 5);
        double envelope = 0;
        double attack = Math.exp(-1.0 / (SAMPLE_RATE * 0.005));
        double release = Math.exp(-1.0 / (SAMPLE_RATE * 0.080));
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short raw = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xff));
            // Shape before the high-pass so darker voices gain harmonics inside the passed band.
            // Dividing by the drive keeps quiet samples at roughly their original level.
            double saturated = Math.tanh(raw / 32768.0 * PRE_FILTER_DRIVE) / PRE_FILTER_DRIVE;
            double band = presence.process(lowPass.process(highPass.process(saturated)));
            double level = Math.abs(band);
            envelope = (level > envelope ? attack : release) * envelope
                    + (1 - (level > envelope ? attack : release)) * level;
            double reduction = envelope > 0.10 ? Math.pow(0.10 / envelope, 3.0 / 4.0) : 1;
            // 25% more makeup drive than before; the tanh ceiling keeps peaks smooth.
            int sample = (int) Math.round(32767 * 0.92 * Math.tanh(band * reduction * OUTPUT_DRIVE));
            pcm[i] = (byte) sample;
            pcm[i + 1] = (byte) (sample >>> 8);
        }
    }

    private static final class Biquad {
        private final double b0, b1, b2, a1, a2;
        private double z1, z2;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0;
            this.b1 = b1 / a0;
            this.b2 = b2 / a0;
            this.a1 = a1 / a0;
            this.a2 = a2 / a0;
        }

        private double process(double input) {
            double output = b0 * input + z1;
            z1 = b1 * input - a1 * output + z2;
            z2 = b2 * input - a2 * output;
            return output;
        }

        private static Biquad highPass(double hz) {
            double cosine = Math.cos(2 * Math.PI * hz / SAMPLE_RATE);
            double alpha = Math.sin(2 * Math.PI * hz / SAMPLE_RATE) / Math.sqrt(2);
            return new Biquad((1 + cosine) / 2, -(1 + cosine), (1 + cosine) / 2,
                    1 + alpha, -2 * cosine, 1 - alpha);
        }

        private static Biquad lowPass(double hz) {
            double cosine = Math.cos(2 * Math.PI * hz / SAMPLE_RATE);
            double alpha = Math.sin(2 * Math.PI * hz / SAMPLE_RATE) / Math.sqrt(2);
            return new Biquad((1 - cosine) / 2, 1 - cosine, (1 - cosine) / 2,
                    1 + alpha, -2 * cosine, 1 - alpha);
        }

        private static Biquad presence(double hz, double gainDb) {
            double omega = 2 * Math.PI * hz / SAMPLE_RATE;
            double cosine = Math.cos(omega);
            double alpha = Math.sin(omega) / (2 * 0.9);
            double amplitude = Math.pow(10, gainDb / 40);
            return new Biquad(1 + alpha * amplitude, -2 * cosine, 1 - alpha * amplitude,
                    1 + alpha / amplitude, -2 * cosine, 1 - alpha / amplitude);
        }
    }

    /** Attach the beeps to the first/last sentence, so interruption and queue cancellation work normally. */
    public static byte[] frame(byte[] voice, boolean opening, boolean closing, float volume) {
        if (!opening && !closing) return voice;
        int prefix = opening ? (TONE_SAMPLES + GAP_SAMPLES) * 2 : 0;
        int suffix = closing ? (GAP_SAMPLES + TONE_SAMPLES) * 2 : 0;
        byte[] framed = new byte[prefix + voice.length + suffix];
        if (opening) writeTone(framed, 0, 2525, volume);
        System.arraycopy(voice, 0, framed, prefix, voice.length);
        if (closing) writeTone(framed, prefix + voice.length + GAP_SAMPLES * 2, 2475, volume);
        return framed;
    }

    private static void writeTone(byte[] pcm, int offset, int frequency, float volume) {
        double amplitude = 0.16 * Math.max(0, Math.min(1, volume));
        for (int n = 0; n < TONE_SAMPLES; n++) {
            double envelope = Math.min(1, Math.min(n, TONE_SAMPLES - 1 - n) / (double) FADE_SAMPLES);
            int sample = (int) Math.round(32767 * amplitude * envelope
                    * Math.sin(2 * Math.PI * frequency * n / SAMPLE_RATE));
            pcm[offset + n * 2] = (byte) sample;
            pcm[offset + n * 2 + 1] = (byte) (sample >>> 8);
        }
    }
}
