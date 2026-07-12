package elite.intel.ai.ears;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the placement of the VAD gate between the ambient noise floor and average speech, and the
 * single criterion that decides whether a room is usable. The rest of the calibrator drives real
 * audio lines and is exercised by hand.
 * <p>
 * Levels are linear RMS amplitudes in 16-bit sample units, as measured by the calibrator.
 */
class AudioCalibratorTest {

    private static final double GATE_BELOW_SPEECH_DB = 12.0;
    private static final double MIN_GATE_ABOVE_NOISE_DB = 6.0;

    /**
     * Decibels relative to 16-bit full scale, matching how the mic meter reads these levels.
     */
    private static double db(double amplitude) {
        return 20 * Math.log10(amplitude / 32768.0);
    }

    /**
     * Rooms spanning a treated studio (floor -73 dBFS) to a noisy one (floor -32 dBFS), each clearing
     * the 18 dB of separation that lets both gate bounds be honoured at once.
     */
    private static final String USABLE_ROOMS = """
            7.2,   1617
            10,    8000
            30,    4000
            100,   3000
            200,   4000
            400,   5000
            800,   8000
            """;

    @Nested
    @DisplayName("gate placement")
    class GatePlacement {

        @ParameterizedTest(name = "floor={0} speech={1}")
        @CsvSource(textBlock = USABLE_ROOMS)
        @DisplayName("gate never rises above the speech anchor nor falls below the noise bound")
        void gateHonoursBothBounds(double floor, double speech) {
            double gate = AudioCalibrator.gateOpenLevel(floor, speech);

            assertTrue(db(speech) - db(gate) >= GATE_BELOW_SPEECH_DB - 1e-9,
                    "gate must sit at least " + GATE_BELOW_SPEECH_DB + " dB under speech");
            assertTrue(db(gate) - db(floor) >= MIN_GATE_ABOVE_NOISE_DB - 1e-9,
                    "gate must clear the noise floor by at least " + MIN_GATE_ABOVE_NOISE_DB + " dB");
        }

        @Test
        @DisplayName("in a quiet room the speech anchor governs, independent of the noise floor")
        void speechAnchorGovernsInAQuietRoom() {
            double speech = 1617;
            double quiet = AudioCalibrator.gateOpenLevel(7.2, speech);
            double quieter = AudioCalibrator.gateOpenLevel(2.0, speech);

            assertEquals(quiet, quieter, 1e-9, "a better noise floor must not drag the gate down");
            assertEquals(GATE_BELOW_SPEECH_DB, db(speech) - db(quiet), 1e-9);
        }

        @Test
        @DisplayName("in a noisy room the noise bound takes over")
        void noiseBoundGovernsInANoisyRoom() {
            double floor = 800;
            double speech = 4000; // only 14 dB of separation: speech - 12 dB would sit under floor + 6 dB
            double gate = AudioCalibrator.gateOpenLevel(floor, speech);

            assertEquals(MIN_GATE_ABOVE_NOISE_DB, db(gate) - db(floor), 1e-9);
        }

        @Test
        @DisplayName("a louder voice at a fixed noise floor raises the gate")
        void gateRisesWithSpeechLevel() {
            assertTrue(AudioCalibrator.gateOpenLevel(200, 8000) > AudioCalibrator.gateOpenLevel(200, 2000));
        }

        /**
         * The regression this guards: a failed speech measurement returns an average of zero, and an
         * early return on it once discarded the noise floor, collapsing the gate to a flat absolute
         * fallback. In any room noisier than that fallback the gate then sat BENEATH the ambient
         * noise, so the VAD opened on the room and never closed.
         */
        @ParameterizedTest(name = "floor={0}")
        @CsvSource({"7.2", "200", "800", "1500"})
        @DisplayName("a failed speech measurement still leaves the gate clear of the noise floor")
        void degenerateSpeechStillClearsTheNoiseFloor(double floor) {
            double gate = AudioCalibrator.gateOpenLevel(floor, 0);

            assertTrue(gate > floor, "gate must never sit under ambient noise, was " + gate + " vs floor " + floor);
            assertTrue(AudioCalibrator.gateClearsNoiseFloor(floor, gate),
                    "even a degenerate calibration must satisfy the startup check");
        }

        @Test
        @DisplayName("a mic that captured nothing at all yields no gate")
        void silenceEverywhereIsDegenerate() {
            assertEquals(0, AudioCalibrator.gateOpenLevel(0, 0));
        }
    }

    @Nested
    @DisplayName("room quality is judged in dB, never against an absolute amplitude")
    class RoomQuality {

        /**
         * The reported regression: a treated room at -73.2 dBFS with speech at -26.1 dBFS, i.e. 47 dB
         * of separation. The old code clamped the gate to {@code noiseFloor + 120 = 127.2} and told
         * the commander to speak louder; the startup check then warned again because 127.2 < 250.
         */
        @Test
        @DisplayName("47 dB of separation on pro gear is accepted, gated low, and never warned about")
        void proGearInATreatedRoomIsAccepted() {
            double floor = 7.2;
            double speech = 1617.2;

            assertTrue(AudioCalibrator.separationDb(floor, speech) > 47.0);

            double gate = AudioCalibrator.gateOpenLevel(floor, speech);
            assertEquals(406.3, gate, 0.5, "gate should land 12 dB under speech");
            assertTrue(gate < 127.2 * 4, "gate must not be dragged up by any absolute constant");
            assertTrue(AudioCalibrator.gateClearsNoiseFloor(floor, gate),
                    "a low gate from a quiet room must not trip the startup warning");
        }

        @ParameterizedTest(name = "floor={0} speech={1}")
        @CsvSource(textBlock = USABLE_ROOMS)
        @DisplayName("every usable room produces a gate the startup check accepts")
        void calibrationAndStartupCheckAgree(double floor, double speech) {
            double gate = AudioCalibrator.gateOpenLevel(floor, speech);
            assertTrue(AudioCalibrator.gateClearsNoiseFloor(floor, gate),
                    "calibrator and STT startup check must never disagree");
        }

        @Test
        @DisplayName("a gate sitting in the noise is rejected however large its amplitude")
        void gateInTheNoiseIsRejected() {
            assertFalse(AudioCalibrator.gateClearsNoiseFloor(3000, 4000), "only 2.5 dB over the floor");
            assertTrue(AudioCalibrator.gateClearsNoiseFloor(3000, 6000), "6 dB over the floor");
        }

        @Test
        @DisplayName("separation is scale-free: halving both levels changes nothing")
        void separationIsScaleFree() {
            assertEquals(AudioCalibrator.separationDb(200, 4000),
                    AudioCalibrator.separationDb(100, 2000), 1e-9);
        }

        @Test
        @DisplayName("degenerate measurements are ordered at the extremes, not treated as usable")
        void degenerateMeasurements() {
            assertEquals(Double.NEGATIVE_INFINITY, AudioCalibrator.separationDb(200, 0), "muted mic");
            assertEquals(Double.POSITIVE_INFINITY, AudioCalibrator.separationDb(0, 4000), "silent floor");
            assertFalse(AudioCalibrator.gateClearsNoiseFloor(200, 0), "no gate at all");
        }
    }
}
