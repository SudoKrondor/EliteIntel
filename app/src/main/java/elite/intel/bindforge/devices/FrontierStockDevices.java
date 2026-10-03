package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceEntry.HardwareId;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private final Map<String, DeviceEntry> byName;
    private final Map<HardwareId, DeviceEntry> byHardware;

    private FrontierStockDevices(List<DeviceEntry> entries) {
        Map<String, DeviceEntry> names = new LinkedHashMap<>();
        Map<HardwareId, DeviceEntry> hardware = new LinkedHashMap<>();
        for (DeviceEntry entry : entries) {
            names.put(entry.name(), entry);
            // WHY: every pair an entry claims, not just its primary. <DualShock4> matches two physically
            // different controllers through its alternatives, and <GamePad> covers eighty - a lookup on the
            // primary alone would call most of Frontier's own hardware unrecognised.
            for (HardwareId id : entry.hardware()) {
                hardware.putIfAbsent(id, entry);
            }
        }
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

    /** Every element name Frontier ships - 51 of them in this capture. */
    public Set<String> names() {
        return byName.keySet();
    }

    /** Every entry, in the order the file lists them. */
    public List<DeviceEntry> entries() {
        return List.copyOf(byName.values());
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
