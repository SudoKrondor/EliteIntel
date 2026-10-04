package elite.intel.bindforge.devicefiles;

/**
 * Decides who put a device entry in an installation's {@code DeviceMappings.xml}.
 * <p>
 * A function of its own, rather than a condition inside whichever scanner needs it, because the rule it
 * encodes is written in four documents and was enforced in none - and it is a rule about <em>permission</em>.
 * Getting it wrong means BindForge offering to rename or clear a device the user does not own.
 *
 * <h2>The rule</h2>
 * An entry Frontier ships is {@link Provenance#FRONTIER}. Everything else is {@link Provenance#UNKNOWN}.
 * <p>
 * <strong>Absence from Frontier's list is deliberately not treated as proof the entry is the player's.</strong>
 * The shipped reference is a capture taken at one moment, and Frontier adds hardware support without asking
 * us first. Under a two-state rule, every device Frontier shipped after that capture would be recorded as the
 * player's, and BindForge would then offer to rename or clear entries belonging to the game. {@code UNKNOWN}
 * means <em>do not claim it, do not touch it</em>, and costs nothing while the reference is current.
 * <p>
 * {@link Provenance#USER_PREEXISTING} is therefore never produced here. It is set when the user confirms an
 * entry is theirs, during first setup - the same shape the rest of the design uses wherever the honest answer
 * is unknowable: ask a question the user can answer rather than compute one BindForge cannot.
 * {@link Provenance#BINDFORGE} is set by the write that creates an entry, not by reading one back.
 */
public final class ProvenanceRule {

    private ProvenanceRule() {
    }

    /**
     * @param deviceName the element tag as that installation's file spells it
     * @param stock      Frontier's shipped list, as captured
     */
    public static Provenance of(String deviceName, FrontierStockDevices stock) {
        return stock.ships(deviceName) ? Provenance.FRONTIER : Provenance.UNKNOWN;
    }
}
