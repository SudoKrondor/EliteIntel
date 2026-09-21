package elite.intel.ui.widget;

import elite.intel.ai.ears.AudioCalibrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static elite.intel.ui.widget.HudMicHealthHint.Verdict.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The hint's verdict must be the STT startup check's verdict, or the settings panel would tell a
 * commander one thing and the voice at service start another. Levels are linear RMS amplitudes in
 * 16-bit sample units, as the audio-monitor frames carry them.
 */
class HudMicHealthHintTest {

    @ParameterizedTest(name = "floor {0}, gate {1}")
    @CsvSource({"0, 0", "0, 500", "100, 0"})
    void noThresholdsMeansNoCalibration(double floor, double gate) {
        assertEquals(UNCALIBRATED, HudMicHealthHint.judge(floor, gate));
    }

    @ParameterizedTest(name = "floor {0}, gate {1}")
    @CsvSource({
            "100, 120",   // the degenerate fallback pinned just over a real floor
            "500, 700",   // under 6 dB clear of the room
            "800, 800",   // gate in the noise
    })
    void aGateInTheRoomIsQuiet(double floor, double gate) {
        assertEquals(QUIET, HudMicHealthHint.judge(floor, gate));
    }

    @ParameterizedTest(name = "floor {0}, gate {1}")
    @CsvSource({"7.2, 400", "30, 1000", "100, 750", "800, 2000"})
    void aGateClearOfTheRoomIsHealthy(double floor, double gate) {
        assertEquals(HEALTHY, HudMicHealthHint.judge(floor, gate));
    }

    @Test
    void agreesWithTheStartupCheckAtTheMargin() {
        double floor = 100;
        double gate = floor * Math.pow(10, 6.0 / 20); // exactly MIN_GATE_ABOVE_NOISE_DB clear
        assertEquals(AudioCalibrator.gateClearsNoiseFloor(floor, gate) ? HEALTHY : QUIET,
                HudMicHealthHint.judge(floor, gate));
    }
}
