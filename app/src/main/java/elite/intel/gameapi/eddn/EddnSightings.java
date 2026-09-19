package elite.intel.gameapi.eddn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.ConflictZoneManager;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.*;
import elite.intel.util.json.GsonFactory;

import java.lang.reflect.Type;
import java.util.List;

/**
 * What this app learns from the EDDN relay: where the resource extraction sites are, where the
 * wars are, and who is fighting them.
 * <p>
 * Two schemas are read. {@code fsssignaldiscovered/1} is one commander's FSS sweep of one system -
 * every signal it found, batched - and is exactly the burst the live journal subscriber counts,
 * so it is counted the same way and lands in the same ledgers. {@code journal/1} carries FSDJump
 * and Location, whose Conflicts block names the sides of a war or, by its absence, says the war
 * is over. Everything else on the relay is ignored before it is parsed.
 * <p>
 * Nothing here is announced. The ledgers fill up quietly and are read back when the commander
 * asks for a hunting ground or a war zone.
 */
final class EddnSightings implements EddnListener.EnvelopeHandler {

    private static final String SCHEMA_FSS_SIGNALS = "https://eddn.edcd.io/schemas/fsssignaldiscovered/1";
    private static final String SCHEMA_JOURNAL = "https://eddn.edcd.io/schemas/journal/1";

    /**
     * The Conflicts block read into the same DTOs the journal events carry, so the relay and the
     * commander's own arrivals are read by one rule ({@link ActiveWar#firstIn}).
     */
    private static final Type CONFLICTS = new TypeToken<List<FSDJumpEvent.Conflict>>() {
    }.getType();

    private final HuntingGroundManager huntingGrounds;
    private final ConflictZoneManager conflictZones;

    EddnSightings() {
        this(HuntingGroundManager.getInstance(), ConflictZoneManager.getInstance());
    }

    EddnSightings(HuntingGroundManager huntingGrounds, ConflictZoneManager conflictZones) {
        this.huntingGrounds = huntingGrounds;
        this.conflictZones = conflictZones;
    }

    /**
     * Reads one envelope off the relay.
     *
     * @return true when the message was one this app learns from, whatever it held
     */
    @Override
    public boolean accept(String envelope) {
        boolean fssSignals = envelope.contains(SCHEMA_FSS_SIGNALS);
        boolean journal = !fssSignals && envelope.contains(SCHEMA_JOURNAL);
        if (!fssSignals && !journal) return false;

        JsonObject root = JsonParser.parseString(envelope).getAsJsonObject();
        String schemaRef = text(root, "$schemaRef");
        JsonElement body = root.get("message");
        if (schemaRef == null || body == null || !body.isJsonObject()) return false;
        JsonObject message = body.getAsJsonObject();

        if (schemaRef.equals(SCHEMA_FSS_SIGNALS)) {
            recordSweep(message);
            return true;
        }
        if (schemaRef.equals(SCHEMA_JOURNAL)) {
            return recordArrival(message);
        }
        return false;
    }

    /**
     * One commander's FSS sweep of one system. The resource sites and the conflict zones in it are
     * counted the way the live subscriber counts a sweep - one sweep key for the whole batch, zones
     * by identity - and written once each.
     */
    private void recordSweep(JsonObject message) {
        String starSystem = text(message, "StarSystem");
        Long systemAddress = longOrNull(message, "SystemAddress");
        Coordinates coordinates = coordinates(starSystem, message);
        JsonElement signals = message.get("signals");
        if (starSystem == null || signals == null || !signals.isJsonArray()) return;

        ResourceSiteSweep resourceSites = new ResourceSiteSweep();
        ConflictZoneSweep conflictZoneSweep = new ConflictZoneSweep();
        ResourceSiteProfile sites = null;
        ConflictZoneProfile zones = null;
        String sitesSeenAt = null;
        String zonesSeenAt = null;
        String sweepKey = systemAddress + "@eddn";

        for (JsonElement element : signals.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject signal = element.getAsJsonObject();
            String symbol = text(signal, "SignalName");
            String timestamp = text(signal, "timestamp");

            ResourceSiteGrade grade = ResourceSiteGrade.fromSymbol(symbol);
            if (grade != null) {
                sites = resourceSites.add(sweepKey, grade);
                sitesSeenAt = latest(sitesSeenAt, timestamp);
                continue;
            }
            ConflictZoneSignal zone = ConflictZoneSignal.fromSymbol(symbol);
            if (zone != null) {
                zones = conflictZoneSweep.add(sweepKey, zone);
                zonesSeenAt = latest(zonesSeenAt, timestamp);
            }
        }

        if (sites != null) {
            huntingGrounds.recordResourceSites(starSystem, systemAddress, coordinates, sites, sitesSeenAt);
        }
        if (zones != null) {
            conflictZones.recordZones(starSystem, systemAddress, coordinates, zones, zonesSeenAt);
        }
    }

    /**
     * An arrival somewhere: the Conflicts block says who is at war in the system, and a block with
     * no active war says the zones there are gone.
     * <p>
     * WHY peace is recorded from the relay at all: without it a war only ever ends by ageing out a
     * week after the last sighting, and the commander could be sent to a zone that closed days ago.
     */
    private boolean recordArrival(JsonObject message) {
        String event = text(message, "event");
        if (!"FSDJump".equals(event) && !"Location".equals(event)) return false;

        String starSystem = text(message, "StarSystem");
        String timestamp = text(message, "timestamp");
        if (starSystem == null || timestamp == null) return false;

        ActiveWar war = ActiveWar.firstIn(GsonFactory.getGson().fromJson(message.get("Conflicts"), CONFLICTS));
        if (war == null) {
            conflictZones.recordPeace(starSystem, timestamp);
        } else {
            conflictZones.recordWar(starSystem, longOrNull(message, "SystemAddress"), coordinates(starSystem, message),
                    war.warType(), war.faction1(), war.faction2(), timestamp);
        }
        return true;
    }

    private static Coordinates coordinates(String starSystem, JsonObject message) {
        JsonElement element = message.get("StarPos");
        if (starSystem == null || element == null || !element.isJsonArray()) return null;
        JsonArray pos = element.getAsJsonArray();
        if (pos.size() < 3) return null;
        try {
            return new Coordinates(starSystem, pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String latest(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static Long longOrNull(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
