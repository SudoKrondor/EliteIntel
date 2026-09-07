package elite.intel.gameapi.journal.subscribers;

import elite.intel.util.Md5Utils;

import java.util.Optional;

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
