package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.BuyAmmoEvent;
import elite.intel.gameapi.journal.events.BuyDronesEvent;
import elite.intel.gameapi.journal.events.CarrierBankTransferEvent;
import elite.intel.gameapi.journal.events.CarrierBuyEvent;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.MarketBuyEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.ModuleBuyEvent;
import elite.intel.gameapi.journal.events.ModuleSellEvent;
import elite.intel.gameapi.journal.events.ModuleSellRemoteEvent;
import elite.intel.gameapi.journal.events.MultiSellExplorationDataEvent;
import elite.intel.gameapi.journal.events.NpcCrewPaidWageEvent;
import elite.intel.gameapi.journal.events.PayBountiesEvent;
import elite.intel.gameapi.journal.events.PayFinesEvent;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.RefuelAllEvent;
import elite.intel.gameapi.journal.events.RefuelPartialEvent;
import elite.intel.gameapi.journal.events.RepairAllEvent;
import elite.intel.gameapi.journal.events.RepairEvent;
import elite.intel.gameapi.journal.events.RestockVehicleEvent;
import elite.intel.gameapi.journal.events.ResurrectEvent;
import elite.intel.gameapi.journal.events.SellDronesEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.gameapi.journal.events.ShipyardBuyEvent;
import elite.intel.gameapi.journal.events.ShipyardSellEvent;
import elite.intel.gameapi.journal.events.ShipyardTransferEvent;
import elite.intel.gameapi.journal.subscribers.FinanceSubscriber;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.CreditsUpdatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reconstructs the commander's true credit balance at startup, for the case where
 * the app is launched mid-session: the live {@code JournalParser} skips everything
 * written before app start ({@code isReplay()}), so the live {@code FinanceSubscriber}
 * would otherwise miss every realized transaction that happened before launch.
 *
 * <p>Registered only on the {@link JournalPreScanner} private bus. It anchors on the
 * most recent {@code LoadGame} (the only event recording an absolute balance) and
 * applies realized deltas that occurred after that anchor but before app start
 * ({@code timestamp < APP_START}, i.e. {@link BaseEvent#isReplay()}). Events at or
 * after app start are deliberately left to the live {@code FinanceSubscriber}, so
 * nothing is ever counted twice - app start is the exact handoff boundary.
 *
 * <p>If the latest {@code LoadGame} is itself at/after app start (game launched after
 * the app), no anchor is recorded here and the live path owns the balance entirely.
 *
 * <p>The signed-delta mapping is shared with {@link FinanceSubscriber#delta} so the two
 * paths can never disagree.
 */
public class FinancePreScanAccumulator {

    private static final Logger log = LogManager.getLogger(FinancePreScanAccumulator.class);

    private boolean anchored = false;
    private long anchor = 0;
    private long runningDelta = 0;

    @Subscribe
    public void onLoadGame(LoadGameEvent e) {
        if (!e.isReplay()) return;          // a LoadGame at/after app start is handled live
        anchor = e.getCredits();
        runningDelta = 0;                   // absolute truth resets the running tally
        anchored = true;
    }

    @Subscribe
    public void onMarketSell(MarketSellEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onMissionCompleted(MissionCompletedEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRedeemVoucher(RedeemVoucherEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onSellOrganicData(SellOrganicDataEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onMultiSellExploration(MultiSellExplorationDataEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onModuleSell(ModuleSellEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onModuleSellRemote(ModuleSellRemoteEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onSellDrones(SellDronesEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onShipyardSell(ShipyardSellEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onMarketBuy(MarketBuyEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onCrewWage(NpcCrewPaidWageEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onResurrect(ResurrectEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onModuleBuy(ModuleBuyEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRepairAll(RepairAllEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRepair(RepairEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRefuelAll(RefuelAllEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRefuelPartial(RefuelPartialEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onBuyAmmo(BuyAmmoEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onRestockVehicle(RestockVehicleEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onBuyDrones(BuyDronesEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onPayFines(PayFinesEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onPayBounties(PayBountiesEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onShipyardBuy(ShipyardBuyEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onShipyardTransfer(ShipyardTransferEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onCarrierBuy(CarrierBuyEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    @Subscribe
    public void onCarrierBankTransfer(CarrierBankTransferEvent e) {
        add(e, FinanceSubscriber.delta(e));
    }

    private void add(BaseEvent e, long delta) {
        if (anchored && e.isReplay()) runningDelta += delta;
    }

    /**
     * Persists the reconstructed balance and pushes it to the UI. Call once after the
     * pre-scan has processed all journal files. No-op if no anchor was found.
     */
    public void persist() {
        if (!anchored) {
            log.info("FinancePreScan: no LoadGame anchor found, leaving credits untouched");
            return;
        }
        long balance = anchor + runningDelta;
        PlayerSession.getInstance().setPersonalCreditsAvailable(balance);
        UiBus.publish(new CreditsUpdatedEvent(balance));
        log.info("FinancePreScan: reconstructed balance = {} (anchor {} + delta {})", balance, anchor, runningDelta);
    }
}
