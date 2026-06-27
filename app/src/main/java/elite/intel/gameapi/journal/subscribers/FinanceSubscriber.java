package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.SensorDataEvent;
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
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.CreditsUpdatedEvent;

/**
 * Single home for every journal event that changes the commander's personal
 * credit balance during a live session.
 *
 * <p>{@code LoadGame} is the only event that records the absolute, authoritative
 * balance, so it sets it outright. Every other event carries a <em>realized</em>
 * delta - money actually paid in or out at a station - never a potential reward
 * (bounty/combat vouchers, exploration data, mission rewards at accept time)
 * which is forfeited if the commander dies before cashing in.
 *
 * <p>Mutates the balance via {@link PlayerSession#adjustCredits(long)} and
 * publishes {@link CreditsUpdatedEvent} on UiBus so the display updates live.
 * If another journal event is found to move money, add it here (and to
 * {@code FinancePreScanAccumulator}).
 *
 * <p>This is also where the spoken financial announcements live: notable events
 * publish a {@link SensorDataEvent} (English data + instruction) so the LLM speaks
 * a personality-styled summary in the user's chosen language - no fixed templates,
 * no localization bundle needed. {@code MarketSell} is the deliberate exception: its
 * announcement stays in {@code MarketSellEventSubscriber} because it is tied to the
 * trade-route feature.
 */
public class FinanceSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onLoadGame(LoadGameEvent event) {
        long balance = event.getCredits();
        playerSession.setPersonalCreditsAvailable(balance);
        UiBus.publish(new CreditsUpdatedEvent(balance));
    }

    // --- Realized inflows (money posted to the account) ---

    @Subscribe
    public void onMarketSell(MarketSellEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onMissionCompleted(MissionCompletedEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onRedeemVoucher(RedeemVoucherEvent e) {
        apply(delta(e));
        announce(e.toYaml(), """
                A bounty/voucher payment was awarded.
                Notify the user about the credits received and which factions we received it from.
                """);
    }

    @Subscribe
    public void onSellOrganicData(SellOrganicDataEvent e) {
        apply(delta(e));
        announce(e.toYaml(), """
                We sold organic data and made credits.
                Provide the user with a sale summary. Start with the total amount collected, then provide a breakdown by genus.
                """);
    }

    @Subscribe
    public void onMultiSellExploration(MultiSellExplorationDataEvent e) {
        apply(delta(e));
        if (playerSession.isDiscoveryAnnouncementOn()) {
            announce(e.toYaml(),
                    "Report the exploration data sale. State the total credits earned, the bonus, and the number of star systems sold.");
        }
    }

    @Subscribe
    public void onModuleSell(ModuleSellEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onModuleSellRemote(ModuleSellRemoteEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onSellDrones(SellDronesEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onShipyardSell(ShipyardSellEvent e) {
        apply(delta(e));
    }

    // --- Realized outflows (money deducted) ---

    @Subscribe
    public void onMarketBuy(MarketBuyEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onCrewWage(NpcCrewPaidWageEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onModuleBuy(ModuleBuyEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onResurrect(ResurrectEvent e) {
        apply(delta(e));
        if (!e.isBankrupt() && e.getCost() > 0) {
            announce(e.toYaml(),
                    "Notify the commander that the ship insurance rebuy was paid and state the cost.");
        }
    }

    @Subscribe
    public void onRepairAll(RepairAllEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onRepair(RepairEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onRefuelAll(RefuelAllEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onRefuelPartial(RefuelPartialEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onBuyAmmo(BuyAmmoEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onRestockVehicle(RestockVehicleEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onBuyDrones(BuyDronesEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onPayFines(PayFinesEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onPayBounties(PayBountiesEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onShipyardTransfer(ShipyardTransferEvent e) {
        apply(delta(e));
    }

    @Subscribe
    public void onShipyardBuy(ShipyardBuyEvent e) {
        apply(delta(e));
        announce(e.toYaml(), "Notify the commander of the new ship purchase and state the net cost.");
    }

    @Subscribe
    public void onCarrierBuy(CarrierBuyEvent e) {
        apply(delta(e));
        announce(e.toYaml(), "Notify the commander that a fleet carrier was purchased and state the price.");
    }

    // --- Mixed (sign depends on direction) ---

    @Subscribe
    public void onCarrierBankTransfer(CarrierBankTransferEvent e) {
        apply(delta(e));
    }

    private void apply(long delta) {
        if (delta == 0) return;
        long newBalance = playerSession.adjustCredits(delta);
        UiBus.publish(new CreditsUpdatedEvent(newBalance));
    }

    /**
     * Hands English data + instruction to the LLM, which speaks it in the user's language with personality.
     */
    private void announce(String data, String instruction) {
        GameEventBus.publish(new SensorDataEvent(data, instruction));
    }

    // Signed deltas (positive = inflow, negative = outflow). Public/static so the
    // startup reconstruction (FinancePreScanAccumulator) shares the exact same mapping.

    public static long delta(MarketSellEvent e) {
        return e.getTotalSale();
    }

    public static long delta(MissionCompletedEvent e) {
        return e.getReward();
    }

    public static long delta(RedeemVoucherEvent e) {
        return e.getAmount();
    }

    public static long delta(MultiSellExplorationDataEvent e) {
        return e.getTotalEarnings();
    }

    public static long delta(ModuleSellEvent e) {
        return e.getSellPrice();
    }

    public static long delta(ModuleSellRemoteEvent e) {
        return e.getSellPrice();
    }

    public static long delta(SellDronesEvent e) {
        return e.getTotalSale();
    }

    public static long delta(ShipyardSellEvent e) {
        return e.getShipPrice();
    }

    public static long delta(MarketBuyEvent e) {
        return -e.getTotalCost();
    }

    public static long delta(NpcCrewPaidWageEvent e) {
        return -e.getAmount();
    }

    public static long delta(RepairAllEvent e) {
        return -e.getCost();
    }

    public static long delta(RepairEvent e) {
        return -e.getCost();
    }

    public static long delta(RefuelAllEvent e) {
        return -e.getCost();
    }

    public static long delta(RefuelPartialEvent e) {
        return -e.getCost();
    }

    public static long delta(BuyAmmoEvent e) {
        return -e.getCost();
    }

    public static long delta(RestockVehicleEvent e) {
        return -e.getCost();
    }

    public static long delta(BuyDronesEvent e) {
        return -e.getTotalCost();
    }

    public static long delta(PayFinesEvent e) {
        return -e.getAmount();
    }

    public static long delta(PayBountiesEvent e) {
        return -e.getAmount();
    }

    public static long delta(ShipyardTransferEvent e) {
        return -e.getTransferPrice();
    }

    public static long delta(CarrierBuyEvent e) {
        return -e.getPrice();
    }

    /**
     * No deduction when the rebuy could not be paid (commander went bankrupt).
     */
    public static long delta(ResurrectEvent e) {
        return e.isBankrupt() ? 0 : -e.getCost();
    }

    /**
     * Net cost: purchase price minus any trade-in for the module being replaced.
     */
    public static long delta(ModuleBuyEvent e) {
        return e.getSellPrice() - e.getBuyPrice();
    }

    /**
     * Net cost: ship price minus any trade-in for the old ship that was sold.
     */
    public static long delta(ShipyardBuyEvent e) {
        return e.getSellPrice() - e.getShipPrice();
    }

    /**
     * Deposit moves money to the carrier (outflow); withdraw brings it back (inflow).
     */
    public static long delta(CarrierBankTransferEvent e) {
        return e.getWithdraw() - e.getDeposit();
    }

    public static long delta(SellOrganicDataEvent e) {
        // WHY: a SellOrganicData with no BioData list legitimately credits nothing; this is an
        // empty-set case, not a masked error.
        if (e.getBioData() == null) return 0;
        return e.getBioData().stream().mapToLong(b -> b.getValue() + b.getBonus()).sum();
    }
}
