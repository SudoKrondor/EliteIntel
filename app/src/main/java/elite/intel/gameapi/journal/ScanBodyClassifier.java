package elite.intel.gameapi.journal;

import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;

/**
 * Classifies a body: from its journal Scan event when there is one, and from its designation alone for the
 * paths that never see a Scan. Shared by every writer and reader of a body's kind so the live bus, the
 * pre-scan replay and the system tallies can never disagree about what a body is.
 */
public class ScanBodyClassifier {

    /**
     * A body designation once the system name is stripped: optional star letters ("A", "AB"), then a
     * planet number, then any number of moon letters ("6 a", "A 2 b", "AB 1 a a"). Star letters alone name a star.
     */
    private static final Pattern DESIGNATION = Pattern.compile("^(?:[A-Z]+ )?(\\d+)((?: [a-z])*)$");
    private static final Pattern STAR_DESIGNATION = Pattern.compile("^[A-Z]+$");

    private ScanBodyClassifier() {
    }

    /**
     * Classifies a body from its name alone, for the paths that never see a Scan event.
     *
     * <p>WHY: the FSS writes only {@code FSSBodySignals} for a body the commander already resolved on an
     * earlier visit - the game does not repeat the {@code Scan} - so a session that starts inside such a
     * system learns a moon's bio signals but never what kind of body carries them. An unclassified row is
     * invisible to every "how many moons" tally, which is how a system with seven bio moons was reported
     * as having one. Frontier's designations are regular enough to read the kind off the name: the
     * system name, optional star letters, a planet number, then a letter per level of moon.
     *
     * @return the kind the name denotes, or {@code null} when the name is not a designation under
     * {@code starSystem} (Sol's named bodies, a missing system name, a station).
     */
    public static LocationDto.LocationType classifyByName(String starSystem, String bodyName) {
        if (starSystem == null || starSystem.isBlank() || bodyName == null) return null;
        String prefix = starSystem.trim() + " ";
        String name = bodyName.trim();
        if (!name.regionMatches(true, 0, prefix, 0, prefix.length())) return null;
        String designation = name.substring(prefix.length()).trim();
        if (designation.isEmpty()) return null;

        LocationDto.LocationType ringOrBelt = ringOrBelt(designation);
        if (ringOrBelt != null) return ringOrBelt;
        if (STAR_DESIGNATION.matcher(designation).matches()) return STAR;

        Matcher m = DESIGNATION.matcher(designation);
        if (!m.matches()) return null;
        return m.group(2).isEmpty() ? PLANET : MOON;
    }

    /**
     * The kind of a stored record: what a scan established, or, when nothing has classified it yet, what its
     * designation says. A kind a scan established is never second-guessed; a name that is not a designation
     * leaves the record as it was.
     *
     * <p>WHY one method for every caller: the FSS report that files the row and the system tally that reads it
     * back both need this rule, and rows written before the FSS path learned to classify itself still exist in
     * commanders' databases, so the reader cannot rely on the writer having run it.
     */
    public static LocationDto.LocationType resolve(LocationDto location) {
        LocationDto.LocationType stored = location.getLocationType();
        if (!isUnclassified(stored)) return stored;
        LocationDto.LocationType byName = classifyByName(location.getStarName(), location.getPlanetName());
        return byName == null ? stored : byName;
    }

    /**
     * Whether a stored classification still says nothing about what the body is.
     */
    public static boolean isUnclassified(LocationDto.LocationType type) {
        return type == null || type == UNCLASSIFIED;
    }

    public static LocationDto.LocationType classify(ScanEvent event) {
        String bodyName = event.getBodyName() == null ? "" : event.getBodyName();

        LocationDto.LocationType ringOrBelt = ringOrBelt(bodyName);
        if (ringOrBelt != null) return ringOrBelt;

        // WHY: StarType is the journal's authoritative star marker, populated for every star and never
        // for a planet. Surface temperature is NOT a discriminator: a rocky body orbiting close to a hot
        // star routinely exceeds 1000 K, and such planets were being classified as stars.
        boolean isStar = event.getStarType() != null && !event.getStarType().isEmpty();

        // WHY: settled before the parent walk. A binary companion lists its sibling as a Star parent,
        // which would otherwise read as "orbits a star, therefore a planet".
        if (isStar && event.getDistanceFromArrivalLS() == 0) return PRIMARY_STAR;
        if (isStar) return STAR;

        List<ScanEvent.Parent> parents = event.getParents();
        if (parents == null || parents.isEmpty()) return UNCLASSIFIED;

        for (ScanEvent.Parent parent : parents) {
            if (parent.getStar() != null && parent.getStar() >= 0) return PLANET;
            if (parent.getPlanet() != null && parent.getPlanet() > 0) return MOON;
        }
        return UNCLASSIFIED;
    }

    /**
     * WHY: rings follow the ED "<parent> <letter> Ring" convention. Match the suffix precisely
     * rather than contains("Ring") so a system whose name contains "Ring" doesn't misclassify its
     * bodies. A ring's parent is a planet, so without this it would fall through to MOON.
     */
    private static LocationDto.LocationType ringOrBelt(String name) {
        if (name.matches(".* [A-Z] Ring")) return PLANETARY_RING;
        if (name.contains("Belt Cluster")) return BELT_CLUSTER;
        return null;
    }
}
