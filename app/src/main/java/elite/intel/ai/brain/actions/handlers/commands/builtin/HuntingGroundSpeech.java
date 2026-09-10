package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.missions.MassacrePair;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns what the hunting ground ledger knows into lines VEGA can speak.
 * <p>
 * Shared by the commands that report hunting grounds and pairs so the same finding is described the
 * same way whichever question asked for it.
 */
final class HuntingGroundSpeech {

    private HuntingGroundSpeech() {
    }

    /**
     * What a system holds, as one clause - "two hazardous, two high and four low resource sites" -
     * or null when the grades were never counted, which a caller says differently rather than
     * pretending the system has none.
     */
    private static String sites(ResourceSiteProfile profile) {
        if (profile == null || !profile.gradesKnown()) return null;

        Map<ResourceSiteGrade, Integer> present = profile.present();
        String counts = present.entrySet().stream()
                .map(entry -> StringUtls.localizedResponse(gradeKey(entry.getKey()), entry.getValue()))
                .collect(Collectors.joining(", "));
        return StringUtls.localizedResponse("handler.res.sitesPhrase", counts);
    }

    /**
     * The sites clause, or the "never counted" line naming the system.
     */
    static String sitesOrUnknown(String starSystem, ResourceSiteProfile profile) {
        String sites = sites(profile);
        return sites == null
                ? StringUtls.localizedResponse("handler.pirate.sitesNotCounted", starSystem)
                : StringUtls.localizedResponse("handler.pirate.sitesIn", starSystem, sites);
    }

    /**
     * The whole finding for one provider and its hunting ground: who is issuing, where, against what,
     * how far, what waits at the far end and which boards to walk.
     */
    static String pair(MassacrePair pair) {
        StringBuilder spoken = new StringBuilder(StringUtls.localizedResponse(
                "handler.pirate.pairFound",
                pair.stackDepth(),
                pair.providerSystem(),
                pair.targetFaction(),
                pair.targetSystem(),
                Math.round(pair.distanceLy())
        ));

        spoken.append(" ").append(sitesOrUnknown(pair.targetSystem(), pair.sites()));

        if (!pair.stations().isEmpty()) {
            spoken.append(" ").append(StringUtls.localizedResponse(
                    "handler.pirate.pairStations", join(pair.stations())));
        }
        return spoken.toString();
    }

    static String join(List<String> values) {
        return String.join(", ", values);
    }

    private static String gradeKey(ResourceSiteGrade grade) {
        return switch (grade) {
            case LOW -> "handler.res.grade.low";
            case STANDARD -> "handler.res.grade.standard";
            case HIGH -> "handler.res.grade.high";
            case HAZARDOUS -> "handler.res.grade.hazardous";
        };
    }
}
