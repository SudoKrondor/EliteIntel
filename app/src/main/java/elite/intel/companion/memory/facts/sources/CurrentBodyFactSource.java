package elite.intel.companion.memory.facts.sources;

import elite.intel.companion.memory.facts.LocalizedFactRelevance;
import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.memory.facts.MemoryFactSource;
import elite.intel.companion.memory.facts.RegisterMemoryFactSource;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.PlayerSituation;
import elite.intel.session.Status;
import elite.intel.util.SolarDayCalculator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Context-gated fact source for the body the commander is at: it contributes only when the current situation is at or
 * near a body (landed, gliding, in orbit, on a ring, in an SRV, or on foot on a planet), classified by
 * {@link Status#getSituation}. When active it grounds the companion in the body's character (class, landable, gravity,
 * atmosphere, temperature, bio/geo signals, terraformable, rings) as one compact, length-capped line. The ambient
 * source admits itself for location, stellar-body, biome, or surface-material subjects; it stays silent away from a
 * body and coexists with {@link CurrentSystemFactSource} under the block's per-source and total caps.
 */
@RegisterMemoryFactSource
public final class CurrentBodyFactSource implements MemoryFactSource {

    /** Provenance label for the {@code <fact source="...">} attribute. */
    private static final String ID = "body";
    private static final List<String> RELEVANCE_ALIAS_KEYS = List.of(
            "query_current_location",
            "query_distance_to_body",
            "query_biome_analysis",
            "query_planet_materials");

    /** The situations that count as being at or near a body; away from these the source stays silent. */
    private static final Set<PlayerSituation> AT_BODY = EnumSet.of(
            PlayerSituation.IN_SHIP_LANDED,
            PlayerSituation.IN_SHIP_GLIDE,
            PlayerSituation.IN_SHIP_ORBIT,
            PlayerSituation.IN_SHIP_RING,
            PlayerSituation.IN_SRV,
            PlayerSituation.ON_FOOT_PLANET);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isRelevant(MemoryFactContext context) {
        return LocalizedFactRelevance.matches(context, 2, RELEVANCE_ALIAS_KEYS);
    }

    @Override
    public List<String> factsFor(MemoryFactContext context) {
        // Situation gate first, without the body record: getSituation consults the location only to detect a ring,
        // which already counts as an at-body situation, so a null location cannot let an at-body turn slip through.
        // This keeps the body-record DB read off the common not-at-body turn.
        if (!AT_BODY.contains(Status.getInstance().getSituation(null))) {
            return List.of();
        }
        LocationDto body = LocationManager.getInstance().findByLocationData(PlayerSession.getInstance().getLocationData());
        String name = FactLine.value(body.getPlanetShortName());
        if (name == null) {
            name = FactLine.value(body.getPlanetName());
        }
        String fact = format(name, body.getPlanetClass(), body.isLandable(), body.getGravity(), body.getAtmosphere(),
                body.getSurfaceTemperature(), body.getBioSignals(), body.getGeoSignals(),
                body.isTerraformable(), body.isHasRings(), body.isTidalLocked(), SolarDayCalculator.solarDaySeconds(body));
        return fact.isBlank() ? List.of() : List.of(fact);
    }

    /**
     * Builds the single compact body line from the body name and its attributes: skips empty/default fields, presents
     * gravity in Earth gravities and temperature in Celsius, and appends parts (highest-value first) capped in length
     * by {@link FactLine#capped}. Returns empty only when there is neither a name nor any attribute. Pure and
     * package-visible for testing.
     */
    static String format(String name, String planetClass, boolean landable, double gravityG, String atmosphere,
                         double temperatureKelvin, int bioSignals, int geoSignals,
                         boolean terraformable, boolean hasRings, boolean tidalLocked, double solarDaySeconds) {
        List<String> parts = new ArrayList<>();
        addIf(parts, FactLine.value(planetClass));
        if (landable) {
            parts.add("landable");
        }
        addIf(parts, gravity(gravityG));
        addIf(parts, prefix("atmosphere", FactLine.value(atmosphere)));
        addIf(parts, celsius(temperatureKelvin));
        addIf(parts, dayLength(solarDaySeconds));
        if (bioSignals > 0) {
            parts.add(signals(bioSignals, "bio"));
        }
        if (geoSignals > 0) {
            parts.add(signals(geoSignals, "geo"));
        }
        if (terraformable) {
            parts.add("terraformable");
        }
        if (hasRings) {
            parts.add("rings");
        }
        if (tidalLocked) {
            parts.add("tidally locked");
        }

        boolean hasName = name != null && !name.isBlank();
        if (!hasName && parts.isEmpty()) {
            return "";
        }
        String head = hasName ? "current body " + name.strip() : "current body";
        return FactLine.capped(head, parts);
    }

    private static void addIf(List<String> parts, String value) {
        if (value != null) {
            parts.add(value);
        }
    }

    private static String prefix(String label, String value) {
        return value == null ? null : label + " " + value;
    }

    /** A signal count with the kind pluralised, e.g. "1 bio signal" / "2 geo signals". */
    private static String signals(int count, String kind) {
        return count + " " + kind + (count == 1 ? " signal" : " signals");
    }

    /**
     * Surface gravity in Earth gravities ("g"), or null when unknown. The app stores gravity already converted to G
     * (see {@link elite.intel.util.LocationUtils#gravityFix}), not the journal's raw m/s2 field.
     */
    private static String gravity(double gravityG) {
        if (gravityG <= 0) {
            return null;
        }
        String s = String.format(Locale.ROOT, "%.2f", gravityG).replaceAll("0+$", "").replaceAll("\\.$", "");
        return "gravity " + s + "g";
    }

    /** Surface temperature in Celsius (the journal stores Kelvin), or null when unknown. */
    private static String celsius(double kelvin) {
        return kelvin > 0 ? Math.round(kelvin - 273) + "°C" : null;
    }

    /**
     * Compact solar-day rendering, e.g. "day 18h" or "day 6h 40m" (minutes only when non-zero), or null when the
     * length is unknown ({@code seconds <= 0}). The full value comes from {@link SolarDayCalculator}.
     */
    private static String dayLength(double seconds) {
        if (seconds <= 0) {
            return null;
        }
        long totalMinutes = Math.round(seconds / 60.0);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours <= 0 && minutes <= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("day ");
        if (hours > 0) {
            sb.append(hours).append('h');
        }
        if (minutes > 0) {
            if (hours > 0) {
                sb.append(' ');
            }
            sb.append(minutes).append('m');
        }
        return sb.toString();
    }
}
