package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which plugged-in controllers onboarding still owes an offer in this run (Alan, 2026-10-15, A7).
 * <p>
 * <strong>Once per run per controller.</strong> A controller offered once - by the automatic offer after activation,
 * by hot-plug, or from the gear's NAME - is not offered again when it is plugged back in, whatever the user answered:
 * a loose cable or a hub resetting would otherwise put the dialog up again and again. CHECK AGAIN, the gear and the
 * next run still ask, as they did before A7.
 * <p>
 * <strong>Whether it needs a name is not decided here.</strong> Every controller that connects waits, and onboarding's
 * own reading leaves out any that has one - so a controller with an entry is offered silently and to no one.
 * <p>
 * In memory and thread-safe: connects and disconnects arrive on the device bus's thread, the offers are taken on the
 * EDT. Nothing here reads a file or the database, so it is safe on the bus thread, which also carries push-to-talk.
 */
public final class HotPlugOffers {

    /** Every controller connected now, by the Device Service's id, so a disconnect - which carries only the id - can be placed. */
    private final Map<Integer, HardwareId> connected = new HashMap<>();
    /** Connected, not offered yet, in the order they came. */
    private final Set<HardwareId> waiting = new LinkedHashSet<>();
    /** Offered in this run. */
    private final Set<HardwareId> offered = new HashSet<>();

    /** A controller connected: it waits for an offer, unless it has had one in this run. */
    public synchronized void connected(int deviceId, HardwareId hardware) {
        connected.put(deviceId, hardware);
        if (!offered.contains(hardware)) waiting.add(hardware);
    }

    /**
     * A controller left before it was offered: it stops waiting, unless another controller on the same VID/PID is
     * still connected. Plugged in again, it waits again.
     */
    public synchronized void disconnected(int deviceId) {
        HardwareId hardware = connected.remove(deviceId);
        if (hardware != null && !connected.containsValue(hardware)) waiting.remove(hardware);
    }

    /** Whether any controller is waiting for an offer. */
    public synchronized boolean anyWaiting() {
        return !waiting.isEmpty();
    }

    /** Whether any controller is waiting for an offer that may be made now - one not among {@code heldBack}. */
    public synchronized boolean anyWaitingBesides(Collection<HardwareId> heldBack) {
        return waiting.stream().anyMatch(hardware -> !heldBack.contains(hardware));
    }

    /** Takes every waiting controller to offer now, in the order they came; each counts as offered from here on. */
    public synchronized Set<HardwareId> take() {
        return take(Set.of());
    }

    /**
     * Takes every waiting controller but those held back, in the order they came; each taken counts as offered from
     * here on. A held-back one - a candidate of an identity question still open (Alan, 2026-10-15, A12) - keeps
     * waiting, so it is offered once the question no longer holds it.
     */
    public synchronized Set<HardwareId> take(Collection<HardwareId> heldBack) {
        Set<HardwareId> taken = new LinkedHashSet<>();
        for (HardwareId hardware : waiting) {
            if (!heldBack.contains(hardware)) taken.add(hardware);
        }
        offered.addAll(taken);
        waiting.removeAll(taken);
        return taken;
    }

    /**
     * Controllers offered by another way in - the automatic offer after activation, or NAME - count as offered, so
     * plugging one in again does not ask a second time.
     */
    public synchronized void offered(Collection<HardwareId> hardware) {
        offered.addAll(hardware);
        waiting.removeAll(hardware);
    }
}
