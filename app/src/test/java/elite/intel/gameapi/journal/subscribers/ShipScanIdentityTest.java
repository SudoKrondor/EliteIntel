package elite.intel.gameapi.journal.subscribers;

import elite.intel.util.Md5Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipScanIdentityTest {

    @Test
    void buildsTheCanonicalKeyFromRawIdentityComponents() {
        String preimage = "$Pirate_Alpha;|sidewinder|Gang A";

        ShipScanIdentity identity = ShipScanIdentity.fromRaw(
                "$Pirate_Alpha;", "sidewinder", "Gang A").orElseThrow();

        assertEquals(preimage, identity.preimage());
        assertEquals(Md5Utils.generateMd5(preimage), identity.key());
    }

    @Test
    void returnsNoIdentityWhenAnyRawComponentIsMissing() {
        assertAll(
                () -> assertTrue(ShipScanIdentity.fromRaw(null, "sidewinder", "Gang A").isEmpty()),
                () -> assertTrue(ShipScanIdentity.fromRaw(" ", "sidewinder", "Gang A").isEmpty()),
                () -> assertTrue(ShipScanIdentity.fromRaw("$Pirate_Alpha;", null, "Gang A").isEmpty()),
                () -> assertTrue(ShipScanIdentity.fromRaw("$Pirate_Alpha;", " ", "Gang A").isEmpty()),
                () -> assertTrue(ShipScanIdentity.fromRaw("$Pirate_Alpha;", "sidewinder", null).isEmpty()),
                () -> assertTrue(ShipScanIdentity.fromRaw("$Pirate_Alpha;", "sidewinder", " ").isEmpty())
        );
    }
}
