package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CartographicDataManager;
import elite.intel.gameapi.cartography.CartographicBody;
import elite.intel.gameapi.journal.events.*;

/**
 * Keeps the ledger of unsold exploration data level with the journal: what each scan, honk and mapping added,
 * what a sale took away, and what a death destroyed. Silent - the ledger only feeds the discovery card.
 * <p>
 * Registered on the pre-scan bus as well as the live one, so a commander who explored with the app closed
 * still sees the data on board. Replaying is safe because every write is idempotent and the journal's order
 * is kept: a scan replayed after the app already stored it writes the same row, and a sale or death replayed
 * after it removes it again. So it runs on the bus thread, never a thread of its own, which would lose that
 * order.
 */
public class CartographicDataSubscriber {

    /**
     * A nav beacon's bulk dump. It is not data the commander gathered, and pricing it as such put a trip's
     * worth of bubble systems at ten times what the sale paid.
     */
    private static final String NAV_BEACON_SCAN = "NavBeaconDetail";

    private final CartographicDataManager ledger = CartographicDataManager.getInstance();

    @Subscribe
    public void onLoadout(LoadoutEvent event) {
        ledger.boardShip(event.getShipId());
    }

    @Subscribe
    public void onScan(ScanEvent event) {
        if (NAV_BEACON_SCAN.equalsIgnoreCase(event.getScanType())) return;
        boolean star = event.getStarType() != null && !event.getStarType().isBlank();
        boolean planet = event.getPlanetClass() != null && !event.getPlanetClass().isBlank();
        if (!star && !planet) return; // a belt cluster, worth nothing
        if (event.getBodyName() == null) return;

        ledger.recordBody(event.getSystemAddress(), event.getStarSystem(), new CartographicBody(
                event.getBodyName(),
                star && event.getDistanceFromArrivalLS() == 0,
                star ? event.getStarType() : null,
                event.getStellarMass(),
                planet ? event.getPlanetClass() : null,
                event.getTerraformState() != null && !event.getTerraformState().isBlank(),
                event.getMassEM(),
                event.isWasDiscovered(),
                event.isWasMapped(),
                false,
                false));
    }

    @Subscribe
    public void onSurfaceMapped(SAAScanCompleteEvent event) {
        if (event.getBodyName() == null) return;
        ledger.recordMapping(event.getBodyName(), event.getProbesUsed() <= event.getEfficiencyTarget());
    }

    @Subscribe
    public void onHonk(FSSDiscoveryScanEvent event) {
        ledger.recordHonk(event.getSystemAddress(), event.getSystemName(), event.getBodyCount());
    }

    @Subscribe
    public void onAllBodiesFound(FSSAllBodiesFoundEvent event) {
        ledger.recordAllBodiesFound(event.getSystemAddress());
    }

    @Subscribe
    public void onSold(MultiSellExplorationDataEvent event) {
        if (event.getDiscovered() == null) return;
        ledger.sold(event.getDiscovered().stream()
                .map(MultiSellExplorationDataEvent.DiscoveredSystem::getSystemName)
                .toList());
    }

    @Subscribe
    public void onDied(DiedEvent event) {
        ledger.lost();
    }
}
