package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.gameapi.signals.ConflictZoneIntensity;
import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.WarZone;
import elite.intel.util.StringUtls;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns what the conflict-zone ledger knows into lines VEGA can speak - the twin of
 * {@link HuntingGroundSpeech}.
 */
final class ConflictZoneSpeech {

    private ConflictZoneSpeech() {
    }

    /**
     * What a system holds and who is fighting there, as one or two sentences: "Ceos holds two
     * high intensity and four low intensity conflict zones. A war: Sirius Corporation against RSR."
     * The sides are left out when no arrival has named them yet.
     */
    static String zones(WarZone warZone) {
        StringBuilder spoken = new StringBuilder(StringUtls.localizedResponse(
                "handler.war.zonesIn", warZone.starSystem(), zonesPhrase(warZone.zones())));
        if (warZone.sidesKnown()) {
            spoken.append(" ").append(StringUtls.localizedResponse("handler.war.sides",
                    warTypeWord(warZone.warType()), warZone.faction1(), warZone.faction2()));
        }
        return spoken.toString();
    }

    private static String zonesPhrase(ConflictZoneProfile profile) {
        Map<ConflictZoneIntensity, Integer> present = profile.present();
        String counts = present.entrySet().stream()
                .map(entry -> StringUtls.localizedResponse(intensityKey(entry.getKey()), entry.getValue()))
                .collect(Collectors.joining(", "));
        return StringUtls.localizedResponse("handler.war.zonesPhrase", counts);
    }

    private static String warTypeWord(String warType) {
        return StringUtls.localizedResponse("civilwar".equalsIgnoreCase(warType)
                ? "handler.war.type.civilwar"
                : "handler.war.type.war");
    }

    private static String intensityKey(ConflictZoneIntensity intensity) {
        return switch (intensity) {
            case LOW -> "handler.war.intensity.low";
            case MEDIUM -> "handler.war.intensity.medium";
            case HIGH -> "handler.war.intensity.high";
            case POWERPLAY -> "handler.war.intensity.powerplay";
        };
    }
}
