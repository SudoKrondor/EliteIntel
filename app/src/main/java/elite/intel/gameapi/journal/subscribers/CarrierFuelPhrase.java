package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.dto.CarrierDataDto;

/**
 * How confidently the carrier's depot level may be spoken.
 *
 * <p>The game reports the depot only when the commander opens carrier management. Every jump in between is
 * charged from the plotted leg, so the figure is arithmetic on top of an older reading and drifts with any
 * tonne that moves another way. Announcing that as a flat number tells the commander something we do not
 * know; hedging a figure the game just gave us is just as wrong the other way, since it invites him to go
 * and check what was already certain.
 *
 * <p>English on purpose: this goes into the narration payload as data, and the companion writes the spoken
 * sentence in the commander's own language from it.
 */
final class CarrierFuelPhrase {

    private CarrierFuelPhrase() {
    }

    /**
     * The depot level as it may be quoted: a plain figure when measured, a hedged one when we worked it out.
     */
    static String of(CarrierDataDto carrierData) {
        int tons = carrierData.getFuelLevel();
        return carrierData.isFuelLevelMeasured() ? tons + " tons" : "approximately " + tons + " tons";
    }
}
