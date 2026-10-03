package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceEntry.HardwareId;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Frontier's shipped {@code DeviceMappings.xml}, as captured on 2026-09-06 and carried inside the jar.
 * <p>
 * It exists because no file on disk answers the question BindForge has to ask before touching anything:
 * <strong>which entries did Frontier ship, and which did the player add?</strong> The format has no
 * attribute, grouping or marker separating them, and nothing else recovers it either - the install folder is
 * replaced wholesale by updates, {@code .binds} strips comments, and timestamps do not survive a verify. A
 * reference copy is the only reliable answer: anything in the player's file but absent from this one was not
 * shipped by Frontier.
 * <p>
 * <strong>This copy goes stale, and that is designed for rather than ignored.</strong> Frontier adds hardware
 * and BindForge will not always ship an update first, so absence from this list never proves an entry is the
 * player's - only that it is not one Frontier shipped <em>as of this capture</em>. Callers treat that as
 * {@code unknown}: do not claim it, do not touch it.
 */
public final class FrontierStockDevices {

    /** Travels in the jar rather than on disk, so it cannot be edited by a game update or by the user. */
    static final String RESOURCE = "/bindforge/FrontierStock-DeviceMappings.xml";

    private static final class Holder {
        private static final FrontierStockDevices INSTANCE = load();
    }

    /**
     * The entries as the file lists them.
     * <p>
     * Order lives here rather than in the maps below, because an immutable {@code Map} does not keep any.
     * {@code Map.copyOf} iterates in an unspecified, hash-derived order that is additionally salted per JVM
     * run - so a list rendered from it would come out differently on every launch. The maps are lookups and
     * need no order at all; this field is what has one.
     */
    private final List<DeviceEntry> inFileOrder;

    private final Map<String, DeviceEntry> byName;
    private final Map<HardwareId, DeviceEntry> byHardware;

    /** Package-private as a test seam: the shipped resource cannot hold a malformed list, and a test must. */
    FrontierStockDevices(List<DeviceEntry> entries) {
        Map<String, DeviceEntry> names = new HashMap<>();
        Map<HardwareId, DeviceEntry> hardware = new HashMap<>();
        for (DeviceEntry entry : entries) {
            // WHY: a repeated element tag throws rather than quietly replacing the entry already under it.
            // It cannot happen in the shipped capture - 51 distinct names, verified - so it would mean a
            // later capture arrived broken, and the damage would be invisible: an entry would vanish from
            // Frontier's list, the device it names would start looking like the user's, and the sha256 test
            // guarding this file would report only that the bytes changed, which is expected after a
            // re-capture. Loud, for the same reason load() refuses to carry on without the resource at all.
            DeviceEntry clash = names.put(entry.name(), entry);
            if (clash != null) {
                throw new IllegalStateException(
                        "Frontier stock reference names <" + entry.name() + "> twice: " + RESOURCE);
            }
            // WHY: every pair an entry claims, not just its primary. <DualShock4> matches two physically
            // different controllers through its alternatives, and <GamePad> covers eighty - a lookup on the
            // primary alone would call most of Frontier's own hardware unrecognised.
            //
            // First wins, and deliberately unlike the names above: two entries legitimately claiming one pair
            // is a thing Frontier's own file could do, and resolving to the earlier of them is a choice rather
            // than a broken file.
            for (HardwareId id : entry.hardware()) {
                hardware.putIfAbsent(id, entry);
            }
        }
        this.inFileOrder = List.copyOf(entries);
        this.byName = Map.copyOf(names);
        this.byHardware = Map.copyOf(hardware);
    }

    /**
     * The reference, parsed once.
     * <p>
     * Read eagerly on first use and held, because it never changes within a run: it is a resource in the jar,
     * and re-parsing it per device row would be work with no possible new answer.
     */
    public static FrontierStockDevices getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Whether Frontier ships an entry under this element name.
     * <p>
     * Matched exactly, because the element tag is what the game reads and XML tags are case-sensitive - a
     * hand-typed {@code t-rudder} is a different element from Frontier's {@code T-Rudder}, and is not
     * Frontier's. The looser comparison that ignores case, {@code -} and {@code _} belongs to alias-collision
     * checking, which is a different question: whether a <em>new</em> name would be confusing.
     */
    public boolean ships(String deviceName) {
        return byName.containsKey(deviceName);
    }

    /** Frontier's entry under that name, or empty when it ships none. */
    public Optional<DeviceEntry> entry(String deviceName) {
        return Optional.ofNullable(byName.get(deviceName));
    }

    /**
     * Frontier's entry covering a piece of attached hardware, or empty when none does.
     * <p>
     * Matched on VID/PID rather than by name, because this answers a different question from {@link
     * #ships(String)}: not "is this entry Frontier's" but "is this controller already named by the game",
     * which is what marks a device BUILT-IN and what means there is nothing for BindForge to create.
     */
    public Optional<DeviceEntry> covering(String vid, String pid) {
        return Optional.ofNullable(byHardware.get(new HardwareId(vid, pid)));
    }

    /**
     * Every element name Frontier ships - 51 of them in this capture - in the order the file lists them.
     * <p>
     * Derived from the ordered list rather than taken from the lookup map's key set, which has no order worth
     * showing anyone.
     */
    public List<String> names() {
        return inFileOrder.stream().map(DeviceEntry::name).toList();
    }

    /** Every entry, in the order the file lists them. */
    public List<DeviceEntry> entries() {
        return inFileOrder;
    }

    private static FrontierStockDevices load() {
        try (InputStream xml = FrontierStockDevices.class.getResourceAsStream(RESOURCE)) {
            // WHY: a missing resource throws rather than degrading to an empty list. Empty would mean
            // "Frontier ships nothing", under which every stock entry on the user's machine looks like theirs
            // and BindForge would offer to rename or clear Frontier's own devices. A packaging mistake must
            // be loud.
            if (xml == null) {
                throw new IllegalStateException("Frontier stock reference is missing from the jar: " + RESOURCE);
            }
            return new FrontierStockDevices(DeviceMappingsParser.parse(xml, RESOURCE));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the Frontier stock reference: " + RESOURCE, e);
        }
    }
}
