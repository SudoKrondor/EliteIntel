package elite.intel.ui.widget;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.ears.AudioCalibrator;
import elite.intel.ai.ears.AudioMonitorEvent;
import elite.intel.eventbus.AudioMonitorBus;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The microphone's verdict on itself, in words, under the {@link HudMicMeter}.
 * <p>
 * The meter shows the numbers; this says what they mean and what to do about it, because the commander
 * this is for does not read meters. It draws the same two values the meter does - the calibrated floor
 * and gate carried on every {@link AudioMonitorEvent} - and judges them with
 * {@link AudioCalibrator#gateClearsNoiseFloor}, the one criterion the calibrator and the STT startup
 * check already share, so the three can never disagree about whether the microphone is usable.
 * <p>
 * Three states, one banner each: no calibration yet, a gate that cannot clear the room (a quiet
 * microphone or a loud room - the advice names the lever the commander actually has), and healthy,
 * which shows nothing at all. The banners are fixed text toggled by visibility rather than one banner
 * re-worded, so the layout is settled once and nothing re-flows while someone is speaking into it.
 */
public class HudMicHealthHint extends JPanel {

    private final HudBanner calibrationRequired;
    private final HudBanner quiet;

    /**
     * Last verdict shown, so a frame that changes nothing costs no Swing work.
     */
    private volatile Verdict shown = Verdict.HEALTHY;

    enum Verdict {HEALTHY, UNCALIBRATED, QUIET}

    public HudMicHealthHint() {
        super(new BorderLayout(0, HudPalette.HUD_GAP_TIGHT));
        setOpaque(false);
        calibrationRequired = HudBanner.multiline(getText("settings.audio.micHint.calibrationRequired"), StatusBadge.State.STANDBY);
        quiet = HudBanner.multiline(getText("settings.audio.micHint.quiet"), StatusBadge.State.STANDBY);
        calibrationRequired.setVisible(false);
        quiet.setVisible(false);
        add(calibrationRequired, BorderLayout.NORTH);
        add(quiet, BorderLayout.SOUTH);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        AudioMonitorBus.register(this);
    }

    @Override
    public void removeNotify() {
        AudioMonitorBus.unregister(this);
        super.removeNotify();
    }

    /**
     * Audio-monitor bus subscriber; runs off the EDT.
     */
    @Subscribe
    public void onAudioFrame(AudioMonitorEvent event) {
        Verdict verdict = judge(event.getNoiseFloor(), event.getRmsHigh());
        if (verdict == shown) return;
        shown = verdict;
        SwingUtilities.invokeLater(() -> {
            calibrationRequired.setVisible(verdict == Verdict.UNCALIBRATED);
            quiet.setVisible(verdict == Verdict.QUIET);
            revalidate();
            repaint();
        });
    }

    /**
     * Mirrors the STT startup check: no thresholds means calibration has never run, and a gate that
     * cannot clear the floor means the calibration it did run found nothing usable.
     */
    static Verdict judge(double noiseFloor, double gateOpen) {
        if (noiseFloor == 0 || gateOpen == 0) return Verdict.UNCALIBRATED;
        if (!AudioCalibrator.gateClearsNoiseFloor(noiseFloor, gateOpen)) return Verdict.QUIET;
        return Verdict.HEALTHY;
    }
}
