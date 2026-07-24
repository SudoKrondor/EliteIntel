package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.journal.events.*;
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
 * <p>This is also where the spoken financial announcements live: notable events hand the companion English
 * data + instruction via {@code CompanionRuntime.narrator().narrate(...)} so the LLM speaks a
 * personality-styled summary in the user's chosen language - no fixed templates, no localization bundle needed. {@code MarketSell} is the deliberate exception: its
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
        announce(voucherPayload(e), """
                A bounty/voucher payment was awarded.
                Notify the user about the credits received and which factions we received it from.
                """);
    }

    @Subscribe
    public void onSellOrganicData(SellOrganicDataEvent e) {
        apply(delta(e));
        announce(organicSalePayload(e), """
                We sold organic data and made credits.
                Provide the user with a sale summary. State the amount earned, and if totalBonus is above zero
                mention it as a first-discovery bonus. Summarise the saleByGenus breakdown by naming each genus
                and its samples count; do not read out the per-genus credit figures. Do not add up the bioData
                rows yourself - every total is precomputed.
                """);
    }

    @Subscribe
    public void onMultiSellExploration(MultiSellExplorationDataEvent e) {
        apply(delta(e));
        if (playerSession.isDiscoveryAnnouncementOn()) {
            announce(explorationSalePayload(e),
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
            announce(rebuyPayload(e),
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
        announce(shipyardBuyPayload(e),
                "Notify the commander of the new ship purchase and state netCost as the net cost.");
    }

    @Subscribe
    public void onCarrierBuy(CarrierBuyEvent e) {
        apply(delta(e));
        announce(carrierBuyPayload(e),
                "Notify the commander that a fleet carrier was purchased and state the price.");
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
     * Every announcement here carries money, so the spoken-amount rule always rides along with it.
     */
    private void announce(String data, String instruction) {
        CompanionRuntime.narrator().narrate(data, withSpokenAmountRule(instruction));
    }

    /**
     * Appends the spoken-amount rule to an announcement instruction. Split out so it can be verified.
     */
    static String withSpokenAmountRule(String instruction) {
        return instruction + SpokenAmounts.RULE;
    }

    // --- Announcement payloads. Each pairs an event's serialized YAML with a spoken sibling for every amount
    // the announcement voices. Kept as pure methods so a test can check the field names match the payload and
    // the spoken figure matches the value, without standing up the companion runtime. ---

    static String voucherPayload(RedeemVoucherEvent e) {
        return e.toYaml() + SpokenAmounts.yamlLine("amount", e.getAmount());
    }

    static String organicSalePayload(SellOrganicDataEvent e) {
        return e.toYaml()
                + SpokenAmounts.yamlLine("totalCredits", e.getTotalCredits())
                + SpokenAmounts.yamlLine("totalBonus", e.getTotalBonus());
    }

    static String explorationSalePayload(MultiSellExplorationDataEvent e) {
        return e.toYaml()
                + SpokenAmounts.yamlLine("totalEarnings", e.getTotalEarnings())
                + SpokenAmounts.yamlLine("bonus", e.getBonus());
    }

    static String rebuyPayload(ResurrectEvent e) {
        return e.toYaml() + SpokenAmounts.yamlLine("cost", e.getCost());
    }

    static String shipyardBuyPayload(ShipyardBuyEvent e) {
        // Net of any trade-in, so the spoken figure matches what actually left the account. This net figure is
        // computed here rather than carried by the event, so it brings its own numeric line as well.
        return e.toYaml() + SpokenAmounts.syntheticAmount("netCost", -delta(e));
    }

    static String carrierBuyPayload(CarrierBuyEvent e) {
        return e.toYaml() + SpokenAmounts.yamlLine("price", e.getPrice());
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

    /**
     * Value plus first-discovery bonus, as computed by the event itself so the credited amount and
     * the amount narrated to the commander can never diverge.
     */
    public static long delta(SellOrganicDataEvent e) {
        return e.getTotalCredits();
    }
}
