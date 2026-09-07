package elite.intel.gameapi.journal.subscribers;

import elite.intel.util.Md5Utils;

import java.util.Optional;

/**
 * Canonical identity shared by {@code Bounty} and {@code ShipTargeted}: {@code PilotName} maps to
 * {@code PilotName}, {@code Target} maps to {@code Ship}, and {@code VictimFaction} maps to
 * {@code Faction}. These are raw journal fields; localised fields are presentation-only, and
 * {@code LegalStatus} is scan state rather than target identity. If any component is unavailable,
 * no fuzzy or partial identity is produced.
 */
record ShipScanIdentity(String key, String preimage) {

    static Optional<ShipScanIdentity> fromRaw(String pilotName, String shipType, String faction) {
        if (isMissing(pilotName) || isMissing(shipType) || isMissing(faction)) {
            return Optional.empty();
        }

        String preimage = pilotName + "|" + shipType + "|" + faction;
        return Optional.of(new ShipScanIdentity(Md5Utils.generateMd5(preimage), preimage));
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
