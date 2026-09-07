package elite.intel.ai.ears;

import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.session.SystemSession;

import javax.annotation.Nullable;
import javax.sound.sampled.Mixer;
import java.util.List;
import java.util.Locale;

/**
 * Renders the microphone diagnostics entry of a support bundle.
 * <p>
 * Written to be read by whoever answers the report, not by the commander who filed it, and written to answer
 * one question in order. Did the line deliver any frames at all? If not, the microphone was never opened and
 * the device section says why. If it did, did anything in them rise above silence? If not, the line is open on
 * something that is muted or on a channel carrying nothing. If it did, did any frame reach the gate? If not,
 * the calibration is wrong for this room or this microphone. And if frames did reach the gate, the audio side
 * is healthy and the gate that matters is push-to-talk, which is reported last.
 * <p>
 * That order is why the sections are laid out the way they are, and why the summary line at the top states
 * which fork the numbers fell into instead of leaving it to be inferred.
 * <p>
 * Everything here is read at bundle time on the bundle worker: enumerating the system's audio devices can
 * block, and comparing the saved device name against the devices present <em>now</em> is most of the value.
 */
public final class MicDiagnosticsReport {

    /**
     * Below this a frame is silence rather than a quiet room; 16-bit full scale is 32767.
     */
    private static final double SILENCE_RMS = 1.0;

    private MicDiagnosticsReport() {
    }

    /**
     * The gate settings this report needs.
     * <p>
     * Four values rather than the {@link SystemSession} they come from: each read on that class is a database
     * round trip, the report wants a consistent set rather than four reads spread across its own rendering,
     * and taking data lets the layout be exercised without a live session behind it.
     */
    public record VoiceGate(boolean pushToTalkEnabled, @Nullable String controllerName, int buttonIndex,
                            boolean sleeping) {

        public static VoiceGate from(SystemSession session) {
            return new VoiceGate(
                    session.isPushToTalkEnabled(),
                    session.getPushToTalkControllerName(),
                    session.getPushToTalkButtonIndex(),
                    session.isSleeping());
        }
    }

    /**
     * Renders from live state.
     */
    public static String render() {
        return render(
                MicLevelRecorder.getInstance().snapshot(),
                inputDeviceNames(),
                VoiceGate.from(SystemSession.getInstance()),
                DeviceService.getInstance().getConnectedDevices());
    }

    /**
     * Renders from state supplied by the caller, so the layout can be exercised for a machine that has no
     * microphone and for the failure shapes that are hard to stage on a developer's own working setup.
     */
    static String render(
            MicLevelRecorder.Snapshot snapshot,
            List<String> presentInputDevices,
            VoiceGate gate,
            List<Device> connectedControllers
    ) {
        StringBuilder text = new StringBuilder("Elite Intel microphone diagnostics\n");
        text.append("(levels only - no audio is recorded or included)\n\n");
        text.append("Verdict: ").append(verdict(snapshot, gate)).append("\n\n");
        appendDevice(text, snapshot, presentInputDevices);
        appendLevels(text, snapshot);
        appendGate(text, snapshot);
        appendPushToTalk(text, gate, connectedControllers);
        return text.toString();
    }

    /**
     * The one line worth reading first. It names the earliest stage that is failing, because a later stage
     * looks broken whenever an earlier one is: a microphone delivering silence also never reaches its gate,
     * and reporting both would send the reader to recalibrate a device that is not being heard at all.
     */
    private static String verdict(MicLevelRecorder.Snapshot snapshot, VoiceGate gate) {
        MicLevelRecorder.Capture capture = snapshot.capture();
        if (capture == null) {
            return "voice input has not been started this session - nothing was captured to report on";
        }
        if (!snapshot.heardAnything()) {
            return "the microphone was opened but delivered no audio at all - see Input device";
        }
        if (snapshot.maxRms() < SILENCE_RMS) {
            return "audio is arriving but every frame is silent - the line is open on a muted or empty input";
        }
        if (snapshot.framesOverGate() == 0) {
            return "audio is arriving but no frame ever reached the gate - see Gate, and recalibrate";
        }
        if (!capture.gateClearsNoiseFloor()) {
            return "speech is reaching the gate, but the gate sits too close to the room noise to be reliable"
                    + " - recalibrate somewhere quiet";
        }
        if (gate.pushToTalkEnabled()) {
            return "the microphone is healthy - if nothing is heard, push-to-talk is the gate, see below";
        }
        return "the microphone is healthy - speech is being heard and is reaching the gate";
    }

    private static void appendDevice(
            StringBuilder text, MicLevelRecorder.Snapshot snapshot, List<String> presentInputDevices) {
        text.append("Input device\n");
        MicLevelRecorder.Capture capture = snapshot.capture();
        if (capture == null) {
            text.append("  (voice input has not run this session)\n");
        } else {
            String requested = capture.requestedDevice();
            text.append("  Chosen in settings: ")
                    .append(requested == null || requested.isBlank() ? "(system default)" : requested).append('\n');
            text.append("  Actually opened:    ")
                    .append(capture.resolvedDevice() == null ? "(system default line)" : capture.resolvedDevice())
                    .append('\n');
            // Stated outright rather than left to a comparison of the two lines above: a saved device that is
            // no longer plugged in falls back to the system default silently, and that fallback is the single
            // most common reason a microphone that used to work stops being heard.
            if (requested != null && !requested.isBlank() && capture.resolvedDevice() == null) {
                text.append("  *** the chosen device was not found; capture fell back to the system default ***\n");
            }
            text.append(String.format(Locale.ROOT, "  Capture format:     %d Hz, %d-bit, %s, %s-endian%n",
                    capture.sampleRate(), capture.sampleSizeBits(),
                    capture.channels() == 1 ? "mono" : capture.channels() + " channels",
                    capture.bigEndian() ? "big" : "little"));
            text.append("  Line buffer:        ").append(capture.bufferSizeBytes()).append(" bytes\n");
            text.append("  Capture started:    ").append(capture.startedAt()).append('\n');
            if (snapshot.stoppedAt() != null) {
                text.append("  Capture stopped:    ").append(snapshot.stoppedAt()).append('\n');
            }
        }
        text.append("  Input devices present now (").append(presentInputDevices.size()).append("):\n");
        if (presentInputDevices.isEmpty()) {
            text.append("    (none - this machine reports no recording device at all)\n");
        } else {
            presentInputDevices.forEach(name -> text.append("    ").append(name).append('\n'));
        }
        text.append('\n');
    }

    private static void appendLevels(StringBuilder text, MicLevelRecorder.Snapshot snapshot) {
        text.append("Levels (RMS amplitude, 16-bit scale, full scale 32767)\n");
        text.append("  Frames captured:  ").append(snapshot.frames()).append('\n');
        if (!snapshot.heardAnything()) {
            // A frame count of zero is the whole finding; the statistics below it would all read 0 and
            // invite the reader to diagnose a quiet microphone instead of an absent one.
            text.append("  *** no audio frames were delivered - the line produced nothing ***\n\n");
            return;
        }
        text.append("  First frame:      ").append(snapshot.firstFrameAt()).append('\n');
        text.append("  Last frame:       ").append(snapshot.lastFrameAt()).append('\n');
        text.append(String.format(Locale.ROOT,
                "  Recent %d frames: min %.1f, median %.1f, 95th %.1f, session peak %.1f%n",
                snapshot.recentFrameCount(), snapshot.minRms(), snapshot.medianRms(),
                snapshot.p95Rms(), snapshot.maxRms()));
        if (snapshot.maxRms() < SILENCE_RMS) {
            text.append("  *** every frame is silence - nothing is arriving on the captured channel ***\n");
        }
        text.append('\n');
    }

    private static void appendGate(StringBuilder text, MicLevelRecorder.Snapshot snapshot) {
        MicLevelRecorder.Capture capture = snapshot.capture();
        if (capture == null) return;
        text.append("Gate (the level speech must reach to be collected)\n");
        text.append(String.format(Locale.ROOT, "  Noise floor:      %.1f%n", capture.noiseFloor()));
        text.append(String.format(Locale.ROOT, "  Opens at:         %.1f%n", capture.gateOpen()));
        text.append(String.format(Locale.ROOT, "  Closes at:        %.1f%n", capture.gateClose()));
        text.append("  Levels came from: ")
                .append(capture.calibrationWasStored() ? "saved calibration" : "a calibration run at startup")
                .append('\n');
        text.append("  Frames at or over the gate: ").append(snapshot.framesOverGate()).append('\n');
        text.append("  Last time the gate was reached: ")
                .append(snapshot.lastGateCrossingAt() == null ? "never" : snapshot.lastGateCrossingAt())
                .append('\n');
        if (!capture.gateClearsNoiseFloor()) {
            text.append("  *** the gate does not clear the noise floor by a usable margin ***\n");
        }
        text.append('\n');
    }

    /**
     * Push-to-talk is reported even when the audio side is healthy, because when it is armed it is the only
     * thing that opens the microphone: levels alone never produce a transcript, so a report that stopped at
     * the levels would call a silent app healthy.
     */
    private static void appendPushToTalk(
            StringBuilder text, VoiceGate gate, List<Device> connectedControllers) {
        text.append("Push-to-talk\n");
        if (!gate.pushToTalkEnabled()) {
            text.append("  Off - the microphone is hands-free");
            text.append(gate.sleeping() ? ", and currently asleep (a wake phrase reopens it)\n" : "\n");
            return;
        }
        String controller = gate.controllerName();
        text.append("  On - only the mapped button opens the microphone\n");
        text.append("  Controller: ")
                .append(controller == null || controller.isBlank() ? "(none mapped)" : controller).append('\n');
        text.append("  Button:     ").append(gate.buttonIndex()).append('\n');
        boolean present = controller != null && !controller.isBlank()
                && connectedControllers.stream().anyMatch(device -> controller.equals(device.name()));
        if (!present) {
            text.append("  *** that controller is not connected, so nothing can open the microphone ***\n");
        }
        text.append("  Controllers connected now (").append(connectedControllers.size()).append("):\n");
        if (connectedControllers.isEmpty()) {
            text.append("    (none)\n");
        } else {
            connectedControllers.forEach(device -> text.append("    ").append(device.name())
                    .append(" (").append(device.buttonCount()).append(" buttons)\n"));
        }
    }

    /**
     * The recording devices the machine reports right now. Failure is reported in the bundle rather than
     * thrown: a machine whose audio subsystem cannot be enumerated is exactly the machine being asked about.
     */
    private static List<String> inputDeviceNames() {
        try {
            return AudioDeviceEnumerator.getInputDevices().stream().map(Mixer.Info::getName).toList();
        } catch (RuntimeException | LinkageError e) {
            return List.of("(could not enumerate input devices: " + e + ")");
        }
    }
}
