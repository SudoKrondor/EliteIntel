package elite.intel.gameapi.journal;

import com.google.gson.JsonObject;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.*;
import elite.intel.session.ClearSessionCacheEvent;
import elite.intel.session.LoadSessionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventRegistry {
    private static final Logger log = LogManager.getLogger(EventRegistry.class);
    private static final Map<String, Class<? extends BaseEvent>> eventMap = new HashMap<>();
    private static final Set<String> NON_TIMED_EVENTS = Set.of(
            "LoadGame", "Commander", "Statistics", "Loadout", "Rank", "Materials", "EngineerProgress", "CarrierStats", "SquadronStartup"
    );
    private static final Set<String> LONG_THRESHOLD_EVENTS = Set.of(
            "ProspectedAsteroid", "FSDJump"
    );
    private static final long THRESHOLD = 10000; // 10 seconds
    private static final long THRESHOLD_LONG = 60000; // 60 seconds

    static {
        registerEvent("EngineerCraft", EngineerCraftEvent.class);
        registerEvent("CommitCrime", CommitCrimeEvent.class);
        registerEvent("CarrierTradeOrder", CarrierTradeOrderEvent.class);
        registerEvent("MarketBuy", MarketBuyEvent.class);
        registerEvent("MarketSell", MarketSellEvent.class);
        registerEvent("Disembark", DisembarkEvent.class);
        registerEvent("SellOrganicData", SellOrganicDataEvent.class);
        registerEvent("MultiSellExplorationData", MultiSellExplorationDataEvent.class);
        registerEvent("CodexEntry", CodexEntryEvent.class);
        registerEvent("SupercruiseEntry", SupercruiseEntryEvent.class);
        registerEvent("Promotion", PromotionEvent.class);
        registerEvent("MaterialCollected", MaterialCollectedEvent.class);
        registerEvent("ScanBaryCentre", ScanBaryCentreEvent.class);
        registerEvent("Docked", DockedEvent.class);
        registerEvent("DockingGranted", DockingGrantedEvent.class);
        registerEvent("DockSRV", DockSRVEvent.class);
        registerEvent("LaunchSRV", LaunchSRVEvent.class);
        registerEvent("FSSBodySignals", FSSBodySignalsEvent.class);
        registerEvent("ApproachSettlement", ApproachSettlementEvent.class);
        registerEvent("Missions", MissionsEvent.class);
        registerEvent("ScanOrganic", ScanOrganicEvent.class);
        registerEvent("SAASignalsFound", SAASignalsFoundEvent.class);
        registerEvent("SAAScanComplete", SAAScanCompleteEvent.class);
        registerEvent("ApproachBody", ApproachBodyEvent.class);
        registerEvent("SaveSession", LoadSessionEvent.class);
        registerEvent("ClearSessionCache", ClearSessionCacheEvent.class);
        registerEvent("UserInput", UserInputEvent.class);
        registerEvent("Bounty", BountyEvent.class);
        registerEvent("Cargo", CargoEvent.class);
        registerEvent("CargoTransfer", CargoTransferEvent.class);
        registerEvent("CarrierJump", CarrierJumpEvent.class);
        registerEvent("CarrierJumpRequest", CarrierJumpRequestEvent.class);
        registerEvent("CarrierLocation", CarrierLocationEvent.class);
        registerEvent("CarrierStats", CarrierStatsEvent.class);
        registerEvent("Commander", CommanderEvent.class);
        registerEvent("EngineerProgress", EngineerProgressEvent.class);
        registerEvent("Friends", FriendsEvent.class);
        registerEvent("FSDJump", FSDJumpEvent.class);
        registerEvent("FSDTarget", FSDTargetEvent.class);
        registerEvent("FSSSignalDiscovered", FSSSignalDiscoveredEvent.class);
        registerEvent("FSSDiscoveryScan", FSSDiscoveryScanEvent.class);
        registerEvent("LaunchDrone", LaunchDroneEvent.class);
        registerEvent("LaunchFighter", LaunchFighterEvent.class);
        registerEvent("DockFighter", DockFighterEvent.class);
        registerEvent("Liftoff", LiftoffEvent.class);
        registerEvent("LoadGame", LoadGameEvent.class);
        registerEvent("Loadout", LoadoutEvent.class);
        registerEvent("Location", LocationEvent.class);
        registerEvent("Materials", MaterialsEvent.class);
        registerEvent("MiningRefined", MiningRefinedEvent.class);
        registerEvent("MissionAbandoned", MissionAbandonedEvent.class);
        registerEvent("MissionAccepted", MissionAcceptedEvent.class);
        registerEvent("MissionCompleted", MissionCompletedEvent.class);
        registerEvent("MissionFailed", MissionFailedEvent.class);
        registerEvent("MissionRedirected", MissionRedirectedEvent.class);
        registerEvent("NavRoute", NavRouteEvent.class);
        registerEvent("NavRouteClear", NavRouteClearEvent.class);
        registerEvent("NpcCrewPaidWage", NpcCrewPaidWageEvent.class);
        registerEvent("Powerplay", PowerplayEvent.class);
        registerEvent("Progress", ProgressEvent.class);
        registerEvent("ProspectedAsteroid", ProspectedAsteroidEvent.class);
        registerEvent("Rank", RankEvent.class);
        registerEvent("ReceiveText", ReceiveTextEvent.class);
        registerEvent("RedeemVoucher", RedeemVoucherEvent.class);
        registerEvent("Reputation", ReputationEvent.class);
        registerEvent("Scan", ScanEvent.class);
        registerEvent("Scanned", ScannedEvent.class);
        registerEvent("ShipTargeted", ShipTargetedEvent.class);
        registerEvent("ShipyardBuy", ShipyardBuyEvent.class);
        registerEvent("ShipyardNew", ShipyardNewEvent.class);
        registerEvent("ShipyardSwap", ShipyardSwapEvent.class);
        registerEvent("StartJump", StartJumpEvent.class);
        registerEvent("Statistics", StatisticsEvent.class);
        registerEvent("SupercruiseDestinationDrop", SupercruiseDestinationDropEvent.class);
        registerEvent("SupercruiseExit", SupercruiseExitEvent.class);
        registerEvent("SwitchSuitLoadout", SwitchSuitLoadoutEvent.class);
        registerEvent("Touchdown", TouchdownEvent.class);
        registerEvent("Shutdown", ShutdownEvent.class);
        registerEvent("SquadronStartup", SquadronStartupEvent.class);

        // Finance: realized credit movements (see FinanceSubscriber)
        registerEvent("Resurrect", ResurrectEvent.class);
        registerEvent("ModuleBuy", ModuleBuyEvent.class);
        registerEvent("ModuleSell", ModuleSellEvent.class);
        registerEvent("ModuleSellRemote", ModuleSellRemoteEvent.class);
        registerEvent("RepairAll", RepairAllEvent.class);
        registerEvent("Repair", RepairEvent.class);
        registerEvent("RefuelAll", RefuelAllEvent.class);
        registerEvent("RefuelPartial", RefuelPartialEvent.class);
        registerEvent("BuyAmmo", BuyAmmoEvent.class);
        registerEvent("RestockVehicle", RestockVehicleEvent.class);
        registerEvent("BuyDrones", BuyDronesEvent.class);
        registerEvent("SellDrones", SellDronesEvent.class);
        registerEvent("PayFines", PayFinesEvent.class);
        registerEvent("PayBounties", PayBountiesEvent.class);
        registerEvent("ShipyardSell", ShipyardSellEvent.class);
        registerEvent("ShipyardTransfer", ShipyardTransferEvent.class);
        registerEvent("CarrierBuy", CarrierBuyEvent.class);
        registerEvent("CarrierBankTransfer", CarrierBankTransferEvent.class);
    }

    private static void registerEvent(String eventName, Class<? extends BaseEvent> eventClass) {
        eventMap.put(eventName, eventClass);
    }

    public static BaseEvent createEvent(String eventName, JsonObject json) {
        Class<? extends BaseEvent> eventClass = eventMap.get(eventName);
        if (eventClass == null) {
            log.info("Event not registered or programmed: {}", eventName);
            return null;
        }

        // Check timestamp for timed events
        if (!NON_TIMED_EVENTS.contains(eventName)) {
            String timestamp = json.has("timestamp") ? json.get("timestamp").getAsString() : null;
            long threshold = LONG_THRESHOLD_EVENTS.contains(eventName) ? THRESHOLD_LONG : THRESHOLD;
            if (timestamp != null && !isRecent(timestamp, threshold)) {
                long ageSec = -1;
                try {
                    ageSec = ChronoUnit.SECONDS.between(Instant.parse(timestamp), Instant.now());
                } catch (Exception ignored) {
                }
                log.debug("Skipping outdated event: {} timestamp={} age={}s threshold={}s",
                        eventName, timestamp, ageSec, threshold / 1000);
                return null;
            }
        }

        try {
            Constructor<? extends BaseEvent> constructor = eventClass.getConstructor(JsonObject.class);
            return constructor.newInstance(json);
        } catch (NoSuchMethodException e) {
            log.error("Event class {} missing JsonObject constructor", eventClass.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            log.error("Failed to instantiate event {} for JSON: {}", eventClass.getSimpleName(), json, e);
            return null;
        }
    }

    public static BaseEvent createEventForPreScan(String eventName, JsonObject json) {
        Class<? extends BaseEvent> eventClass = eventMap.get(eventName);
        if (eventClass == null) return null;
        try {
            Constructor<? extends BaseEvent> constructor = eventClass.getConstructor(JsonObject.class);
            return constructor.newInstance(json);
        } catch (NoSuchMethodException e) {
            log.error("PreScan: event class {} missing JsonObject constructor", eventClass.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            log.warn("PreScan: failed to instantiate {} : {}", eventName, e.getMessage());
            return null;
        }
    }

    private static boolean isRecent(String timestamp, long millisThreshold) {
        try {
            Instant eventTime = Instant.parse(timestamp);
            Instant now = Instant.now();
            return !eventTime.isBefore(now.minus(millisThreshold, ChronoUnit.MILLIS));
        } catch (Exception e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }
}