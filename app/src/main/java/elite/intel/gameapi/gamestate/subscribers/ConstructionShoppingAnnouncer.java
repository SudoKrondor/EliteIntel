package elite.intel.gameapi.gamestate.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.gameapi.colonisation.*;
import elite.intel.gameapi.colonisation.CarrierStockpile.Stash;
import elite.intel.gameapi.colonisation.ConstructionCargo.Outstanding;
import elite.intel.gameapi.colonisation.ShoppingShelves.Shop;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Says when a market has nothing left to give a build.
 * <p>
 * <b>Why it is worth saying at all.</b> Stocking a carrier is a long shop: buy a hold-full, shuttle it
 * across, come back, and repeat until the shelves have nothing the build still wants. The moment that
 * happens is invisible - the commander is looking at a commodity screen that still shows plenty of stock,
 * because most of it is not on the manifest, or is stock they now own the equal of. Without a word said,
 * they keep flying back to a shop that is finished.
 * <p>
 * <b>What it deliberately does not do</b> is decide what happens next. Flying the stockpile in and moving it
 * to another market are both reasonable, and which one is right depends on how much of the build is left and
 * how far the next market is - so the commander is told the shop is done and left to choose. The HUD card
 * shows what the build still wants in the same moment, from the same figures.
 * <p>
 * Once per market. The trigger is possession changing, which is checked far more often than it moves, so the
 * announcement is latched on the market it was made for - and unlatched if that market has something to
 * offer again, which happens when the depot's manifest is refreshed and the build asks for more.
 */
public class ConstructionShoppingAnnouncer {

    /**
     * The market we have already said this about, or {@code 0} for none. A MarketID, so docking at a
     * different shop arms it again without any explicit reset.
     */
    private final AtomicLong announcedFor = new AtomicLong(0);

    /**
     * Cargo moved: bought, sold, transferred to the carrier. The only way a shop gets bought out by our own
     * hand, and the event that carries the corrected hold rather than the intent to change it - a
     * {@code MarketBuy} would race the file that proves it.
     */
    @Subscribe
    public void onCargoChanged(GameEvents.CargoEvent event) {
        checkShop();
    }

    /**
     * A market screen opened. Catches the shop that was already finished before we got here - nothing about
     * our cargo changes when the commander docks at a market they can no longer use.
     */
    @Subscribe
    public void onMarket(GameEvents.MarketEvent event) {
        checkShop();
    }

    private void checkShop() {
        Site site = ConstructionSiteManager.getInstance().currentSite();
        if (!ActiveConstructionSite.isLive(site)) {
            announcedFor.set(0);
            return;
        }
        Optional<Shop> shop = ShoppingShelves.getInstance().current();
        if (shop.isEmpty()) return;

        List<Outstanding> manifest = ConstructionCargo.outstanding(
                ConstructionSiteManager.getInstance().requirements(site.getMarketId()),
                ConstructionCargo.heldBySymbol(PlayerSession.getInstance().getShipCargo()));
        if (manifest.isEmpty()) return;

        // Only while a carrier is actually working this build. A commander hauling with the ship alone buys
        // what fits and flies it out; there is no stockpile to finish, so there is nothing to announce.
        Optional<Stash> stash = CarrierStockpile.forBuild(site, symbols(manifest));
        if (stash.isEmpty()) return;

        long marketId = shop.get().marketId();
        boolean boughtOut = ConstructionShopping.isBoughtOut(
                ConstructionShopping.soldHere(manifest, shop.get().stock(), stash.get()));
        if (!boughtOut) {
            // It can go short again: another commander delivers, the architect adds to the build, and the
            // shelves here matter once more.
            announcedFor.compareAndSet(marketId, 0);
            return;
        }
        if (announcedFor.getAndSet(marketId) == marketId) return;

        EventNarrator.say(localizedEvent("event.construction.marketBoughtOut", shopName(shop.get())));
    }

    private static Set<String> symbols(List<Outstanding> manifest) {
        return manifest.stream().map(Outstanding::symbol).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The market as a human names it, falling back to a generic word for a shop we hold only an id for -
     * "this market is finished" is still the whole point of the sentence.
     */
    private static String shopName(Shop shop) {
        String name = shop.stationName();
        return name == null || name.isBlank() ? localizedEvent("event.construction.thisMarket") : name;
    }
}
