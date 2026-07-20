package elite.intel.gameapi.journal;

import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;

import java.util.List;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.*;

/**
 * Classifies a scanned body from its journal Scan event. Shared by every path that persists a scan
 * so the live and pre-scan writers can never disagree about what a body is.
 */
public class ScanBodyClassifier {

    private ScanBodyClassifier() {
    }

    public static LocationDto.LocationType classify(ScanEvent event) {
        String bodyName = event.getBodyName() == null ? "" : event.getBodyName();

        // WHY: rings follow the ED "<parent> <letter> Ring" convention. Match the suffix precisely
        // rather than contains("Ring") so a system whose name contains "Ring" doesn't misclassify its
        // bodies. A ring's parent is a planet, so without this it would fall through to MOON.
        if (bodyName.matches(".* [A-Z] Ring")) return PLANETARY_RING;
        if (bodyName.contains("Belt Cluster")) return BELT_CLUSTER;

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
}