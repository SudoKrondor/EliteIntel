package elite.intel.session;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.PlayerDao;
import elite.intel.db.dao.ShipScansDao;
import elite.intel.db.managers.*;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.ReputationEvent;
import elite.intel.gameapi.journal.events.dto.*;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.util.OsDetector;
import elite.intel.util.Ranks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Represents a player's session encompassing various aspects of gameplay including
 * missions, bounties, mining activities, reputation, ship loadouts, and other relevant data points.
 * <p>
 * This class acts as a singleton and centralizes the management of state and data related
 * to the player's progress and session-specific details.
 */
public class PlayerSession {

    private static final String UUD = java.util.UUID.randomUUID().toString();
    private static volatile PlayerSession instance;
    /// Data managers.
    private final ShipScansManager shipScans = ShipScansManager.getInstance();
    private final MissionManager missions = MissionManager.getInstance();
    private final BountyManager bounties = BountyManager.getInstance();
    private final MiningTargetManager miningTargets = MiningTargetManager.getInstance();
    private final StationMarketsManager markets = StationMarketsManager.getInstance();
    private final RankAndProgressManager rankAndProgress = RankAndProgressManager.getInstance();
    private final FleetCarrierManager fleetCarriers = FleetCarrierManager.getInstance();
    private final SquadronCarrierManager squadronCarriers = SquadronCarrierManager.getInstance();
    private final BioSamplesManager bioSamples = BioSamplesManager.getInstance();
    private final ShipLoadoutManager shipLoadouts = ShipLoadoutManager.getInstance();
    private final GenusAnnouncementManager genusAnouncements = GenusAnnouncementManager.getInstance();
    private final CargoHoldManager cargoHold = CargoHoldManager.getInstance();
    private final ReputationManager reputationManager = ReputationManager.getInstance();
    private final TargetLocationManager targetLocationManager = TargetLocationManager.getInstance();
    private final FsdTargetManager fsdTargetManager = FsdTargetManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private boolean shipAutoDeparted = false;

    private PlayerSession() {
        GameEventBus.register(this);
    }

    public String getUUD() {
        return UUD;
    }

    public static PlayerSession getInstance() {
        if (instance == null) {
            synchronized (PlayerSession.class) {
                if (instance == null) {
                    instance = new PlayerSession();
                }
            }
        }
        return instance;
    }

    public void putShipScan(String key, String scan) {
        ShipScansDao.ShipScan data = new ShipScansDao.ShipScan();
        data.setScan(scan);
        data.setKey(key);
        shipScans.saveScan(data);
    }

    public String getShipScan(String key) {
        return shipScans.get(key);
    }


    public void addBountyReward(long reward) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalBountyClaimed(player.getTotalBountyClaimed() + reward);
            dao.save(player);
            return Void.class;
        });
    }

    public void addBounty(BountyDto bounty) {
        bounties.add(bounty);
    }

    public void removeBounty(BountyDto bounty) {
        bounties.remove(bounty);
    }

    public long getTotalBountyClaimed() {
        return Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            return player.getTotalBountyClaimed();
        });
    }

    public void addMission(MissionDto mission) {
        missions.save(mission);
    }


    public void removeMission(Long missionId) {
        missions.remove(missionId);
    }

    public MissionDto getMission(Long missionId) {
        return missions.getMission(missionId);
    }


    public RankAndProgressDto getRankAndProgressDto() {
        return rankAndProgress.get();
    }

    public void setRankAndProgressDto(RankAndProgressDto rankAndProgressDto) {
        rankAndProgress.save(rankAndProgressDto);
    }

    @Subscribe
    public void onBounty(BountyDto data) {
        bounties.add(data);
    }

    public Set<BountyDto> getBounties() {
        return bounties.getAll();
    }

    public String getPrimaryStarName() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getCurrentPrimaryStar());
    }

    public void clearBounties() {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalBountyClaimed(0);
            dao.save(player);
            return Void.class;
        });
        bounties.clear();
    }

    public void setBountyCollectedLiveTime(long amount) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setBountyCollectedLifetime(amount);
            dao.save(player);
            return Void.class;
        });
    }

    public long getBountyCollectedLiveTime() {
        return Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            return player.getBountyCollectedLifetime();
        });
    }

    public void setCurrentLocationId(long bodyId, long systemAddress) {
        if (bodyId == 0 || systemAddress == 0) {
            return;
        }
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCurrentLocationId(bodyId);
            player.setSystemAddress(systemAddress);
            dao.save(player);
            return Void.class;
        });
    }

    public LocationData<Long, Long> getLocationData() {
        return Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            Long currentLocationId = player.getCurrentLocationId();
            Long systemAddress = player.getSystemAddress();
            return new LocationData<>(systemAddress, currentLocationId);
        });
    }


    public CarrierDataDto getFleetCarrierData() {
        return fleetCarriers.get();
    }

    public void setFleetCarrierData(CarrierDataDto carrierData) {
        fleetCarriers.save(carrierData);
    }

    public Set<String> getMiningTargets() {
        return miningTargets.getAll();
    }

    public void addMiningTarget(String miningTarget) {
        miningTargets.add(miningTarget);
    }

    public void removeMiningTarget(String target) {
        miningTargets.remove(target);
    }

    public void clearMiningTargets() {
        miningTargets.clear();
    }

    public void saveMarket(GameEvents.MarketEvent data) {
        markets.save(data);
    }

    public List<BioSampleDto> getBioCompletedSamples() {
        return bioSamples.listAll();
    }

    public void addBioSample(BioSampleDto bioSampleDto) {
        bioSamples.add(bioSampleDto);
    }

    public void clearBioSamples() {
        bioSamples.clear();
    }

    public void setShipLoadout(ShipLoadOutDto event) {
        shipLoadouts.save(event);
    }

    public ShipLoadOutDto getShipLoadout() {
        return shipLoadouts.get();
    }

    public void setShipCargo(GameEvents.CargoEvent event) {
        cargoHold.save(event);
    }

    public GameEvents.CargoEvent getShipCargo() {
        return cargoHold.get();
    }

    public void setReputation(ReputationEvent event) {
        reputationManager.save(event);
    }

    public void setLastScan(LocationDto lastScan) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setLastScanId(lastScan.getBodyId());
            dao.save(player);
            return Void.class;
        });
    }

    public LocationDto getLastScan() {
        return Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            return LocationManager.getInstance().getLocation(player.getCurrentPrimaryStar(), player.getLastScanId());
        });
    }

    public TargetLocation getTracking() {
        return targetLocationManager.get();
    }

    public void setTracking(TargetLocation tracking) {
        targetLocationManager.save(tracking);
    }


    public void setTotalHyperspaceDistance(long totalHyperspaceDistance) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalHyperspaceDistance(totalHyperspaceDistance);
            dao.save(player);
            return Void.class;
        });
    }

    public void setInsuranceClaims(int insuranceClaims) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setInsuranceClaims(insuranceClaims);
            dao.save(player);
            return Void.class;
        });
    }

    public void setTotalProfitsFromExploration(long totalProfitsFromExploration) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalProfitsFromExploration(totalProfitsFromExploration);
            dao.save(player);
            return Void.class;
        });
    }

    public void setExobiologyProfits(long exobiologyProfits) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setExobiologyProfits(exobiologyProfits);
            dao.save(player);
            return Void.class;
        });
    }

    public void setHighestSingleTransaction(long highestSingleTransaction) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setHighestSingleTransaction(highestSingleTransaction);
            dao.save(player);
            return Void.class;
        });
    }

    public String getFinalDestination() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getFinalDestination());
    }

    public void setFinalDestination(String finalDestination) {
        Database.withDao(PlayerDao.class, playerDao -> {
            PlayerDao.Player player = playerDao.get();
            player.setFinalDestination(finalDestination);
            playerDao.save(player);
            return Void.class;
        });
    }

    public void setMarketProfits(long marketProfits) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setMarketProfits(marketProfits);
            dao.save(player);
            return Void.class;
        });
    }

    public void setCurrentShip(String currentShip) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCurrentShip(currentShip);
            dao.save(player);
            return Void.class;
        });
    }

    public String getPlayerMissionStatement() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getPlayerMissionStatement());
    }

    public void setHomeSystem(LocationDto newHome) {
        if (newHome != null) {
            Database.withDao(PlayerDao.class, dao -> {
                PlayerDao.Player player = dao.get();
                player.setHomeSystemId(newHome.getSystemAddress());
                dao.save(player);
                return Void.class;
            });
        }
    }

    public LocationDto getHomeSystem() {
        Long homeSystemId = Database.withDao(PlayerDao.class, dao -> dao.get().getHomeSystemId());
        return LocationManager.getInstance().findBySystemAddress(homeSystemId);
    }

    public void setPlayerMissionStatement(String playerMissionStatement) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setPlayerMissionStatement(playerMissionStatement);
            dao.save(player);
            return Void.class;
        });
    }

    public void setCrewWagsPayout(long crewWagsPayout) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCrewWagsPayout(crewWagsPayout);
            dao.save(player);
            return Void.class;
        });
    }

    public void setCurrentShipName(String currentShipName) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCurrentShipName(currentShipName);
            dao.save(player);
            return Void.class;
        });
    }


    /**
     * Sets the absolute personal credit balance. {@code synchronized} on the same monitor
     * as {@link #adjustCredits(long)} (see its WHY) so a set cannot interleave with a
     * concurrent read-modify-write.
     */
    public synchronized void setPersonalCreditsAvailable(long personalCreditsAvailable) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setPersonalCreditsAvailable(personalCreditsAvailable);
            dao.save(player);
            return Void.class;
        });
    }

    /**
     * Atomically applies a signed delta to the stored personal credit balance and returns
     * the new balance.
     * <p>WHY synchronized: at startup two threads write credits - the journal pre-scan
     * (FinancePreScanAccumulator, on its own virtual thread) and the live journal parser
     * (FinanceSubscriber). Sharing this monitor with {@link #setPersonalCreditsAvailable(long)}
     * keeps the read-modify-write atomic and prevents lost updates.
     */
    public synchronized long adjustCredits(long delta) {
        return Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            long newBalance = player.getPersonalCreditsAvailable() + delta;
            player.setPersonalCreditsAvailable(newBalance);
            dao.save(player);
            return newBalance;
        });
    }

    public void setShipsOwned(int shipsOwned) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setShipsOwned(shipsOwned);
            dao.save(player);
            return Void.class;
        });
    }

    public String getPlayerName() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getPlayerName());
    }

    public void setPlayerName(String playerName) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setPlayerName(playerName);
            dao.save(player);
            return Void.class;
        });
    }

    public String getLastKnownCarrierLocation() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getLastKnownCarrierLocation());
    }

    public void setLastKnownCarrierLocation(String lastKnownCarrierLocation) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setLastKnownCarrierLocation(lastKnownCarrierLocation);
            dao.save(player);
            return Void.class;
        });
    }

    public String getCarrierDepartureTime() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getCarrierDepartureTime());
    }

    public void setCarrierDepartureTime(String carrierDepartureTime) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCarrierDepartureTime(carrierDepartureTime);
            dao.save(player);
            return Void.class;
        });
    }

    public String getPlayerHighestMilitaryRank() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getPlayerHighestMilitaryRank());
    }

    public void setPlayerHighestMilitaryRank(String playerHighestMilitaryRank) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setPlayerHighestMilitaryRank(playerHighestMilitaryRank);
            dao.save(player);
            return Void.class;
        });
    }

    public void setSpeciesFirstLogged(int speciesFirstLogged) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setSpeciesFirstLogged(speciesFirstLogged);
            dao.save(player);
            return Void.class;
        });
    }

    public void setTotalSystemsVisited(int totalSystemsVisited) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalSystemsVisited(totalSystemsVisited);
            dao.save(player);
            return Void.class;
        });
    }

    public void setTotalBountyClaimed(long totalBountyClaimed) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalBountyClaimed(totalBountyClaimed);
            dao.save(player);
            return Void.class;
        });
    }

    public void setGoodsSoldThisSession(int goodsSoldThisSession) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setGoodsSoldThisSession(goodsSoldThisSession);
            dao.save(player);
            return Void.class;
        });
    }

    public void setTotalDistanceTraveled(double totalDistanceTraveled) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setTotalDistanceTraveled(totalDistanceTraveled);
            dao.save(player);
            return Void.class;
        });
    }

    public void setFsdTarget(FsdTarget fsdTarget) {
        fsdTargetManager.save(fsdTarget);
    }

    public FsdTarget getFsdTarget() {
        return fsdTargetManager.get();
    }

    public Boolean isRadioTransmissionOn() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getRadioTransmissionOn());
    }

    public void setRadioTransmissionOn(Boolean radioTransmissionOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setRadioTransmissionOn(radioTransmissionOn);
            dao.save(player);
            return Void.class;
        });
    }

    public Boolean isMiningAnnouncementOn() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().isMiningAnnouncementOn());
    }

    public void setMiningAnnouncementOn(Boolean miningAnnouncementOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setMiningAnnouncementOn(miningAnnouncementOn);
            dao.save(player);
            return Void.class;
        });
    }

    public void setNavigationAnnouncementOn(Boolean navigationAnnouncementOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setNavigationAnnouncementOn(navigationAnnouncementOn);
            dao.save(player);
            return Void.class;
        });
    }

    public Boolean isDiscoveryAnnouncementOn() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().isDiscoveryAnnouncementOn());
    }

    public void setDiscoveryAnnouncementOn(Boolean discoveryAnnouncementOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setDiscoveryAnnouncementOn(discoveryAnnouncementOn);
            dao.save(player);
            return Void.class;
        });
    }


    public Boolean isRouteAnnouncementOn() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().isRouteAnnouncementOn());
    }

    public void setRouteAnnouncementOn(Boolean routeAnnouncementOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setRouteAnnouncementOn(routeAnnouncementOn);
            dao.save(player);
            return Void.class;
        });
    }

    public void addAnnouncedGenusPayment(String genus) {
        genusAnouncements.put(genus, true);
    }

    public void clearGenusPaymentAnnounced() {
        genusAnouncements.clear();
    }


    public void setCurrentWealth(long currentWealth) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCurrentWealth(currentWealth);
            dao.save(player);
            return Void.class;
        });
    }

    public Boolean paymentHasBeenAnnounced(String genus) {

        Boolean b = genusAnouncements.get(genus);
        return b != null && b;
    }

    public void setGameVersion(String gameversion) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setGameVersion(gameversion);
            dao.save(player);
            return Void.class;
        });
    }

    public void setGameBuild(String gameBuild) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setGameBuild(gameBuild);
            dao.save(player);
            return Void.class;
        });
    }

    public void setInGameName(String inGameName) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setInGameName(inGameName);
            dao.save(player);
            return Void.class;
        });
    }

    public String getInGameName() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getInGameName());
    }

    public void setCurrentPrimaryStarName(String starName) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setCurrentPrimaryStar(starName);
            dao.save(player);
            return Void.class;
        });
    }

    public void setJournalPath(String path) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setJournalDirectory(path);
            dao.save(player);
            return Void.class;
        });
    }

    public Path getJournalPath() {
        return Database.withDao(PlayerDao.class, dao -> {
            String directory = trimToNull(dao.get().getJournalDirectory());
            if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
                return directory == null ? Paths.get(System.getProperty("user.home"), "Saved Games", "Frontier Developments", "Elite Dangerous") : Paths.get(directory);
            } else if (OsDetector.getOs() == OsDetector.OS.LINUX) {
                return directory == null ? Paths.get(System.getProperty("user.home"), ".var", "app", "elite.intel.app", "ed-journal") : Paths.get(directory);
            } else {
                return directory == null ? Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Frontier Developments", "Elite Dangerous") : Paths.get(directory);
            }
        });
    }

    public void setBindingsDir(String path) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setBindingsDirectory(path);
            dao.save(player);
            return Void.class;
        });
    }

    public Path getBindingsDir() {
        return Database.withDao(PlayerDao.class, dao -> {
            String directory = trimToNull(dao.get().getBindingsDirectory());
            if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
                return directory == null ? Paths.get(System.getProperty("user.home"), "AppData", "Local", "Frontier Developments", "Elite Dangerous", "Options", "Bindings") : Paths.get(directory);
            } else if (OsDetector.getOs() == OsDetector.OS.LINUX) {
                return directory == null ? Paths.get(System.getProperty("user.home"), ".var", "app", "elite.intel.app", "ed-bindings") : Paths.get(directory);
            } else {
                return directory == null ? Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Frontier Developments", "Elite Dangerous", "Options", "Bindings") : Paths.get(directory);
            }
        });
    }

    public void setAlternativeName(String alternativeName) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setAlternativeName(alternativeName);
            dao.save(player);
            return Void.class;
        });
    }

    public String getAlternativeName() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getAlternativeName());
    }


    public void clearShipScans() {
        shipScans.clear();
    }

    public void setCarrierStats(CarrierStatsEvent event) {
        fleetCarriers.setCarrierStats(event);
    }


    public long getHighestTransaction() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getHighestSingleTransaction());
    }

    public long getTradeProfits() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getMarketProfits());
    }

    public long getPersonalCredits() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getPersonalCreditsAvailable());
    }

    public double getTotalDistanceTraveled() {
        return Database.withDao(PlayerDao.class, playerDao -> playerDao.get().getTotalDistanceTraveled());
    }

    public long getTotalHyperspaceDistance() {
        return Database.withDao(PlayerDao.class, playerDao -> playerDao.get().getTotalHyperspaceDistance());
    }

    public long getTotalProfitsFromExploration() {
        return Database.withDao(PlayerDao.class, playerDao -> playerDao.get().getTotalProfitsFromExploration());
    }

    public long getTotalSystemsVisited() {
        return Database.withDao(PlayerDao.class, playerDao -> playerDao.get().getTotalSystemsVisited());
    }

    public long getTotalExobiologyProfits() {
        return Database.withDao(PlayerDao.class, playerDao -> playerDao.get().getExobiologyProfits());
    }


    public void setCurrentPartial(String genus) {
        Database.withDao(PlayerDao.class, playerDao -> {
            PlayerDao.Player player = playerDao.get();
            player.setCurrentGenus(genus);
            playerDao.save(player);
            return Void.class;
        });
    }

    public String getCurrentPartial() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().getCurrentGenus());
    }

    public CarrierDataDto getSquadronCarrierData() {
        return squadronCarriers.get();
    }

    public void setSquadronCarrierData(CarrierDataDto carrierData) {
        squadronCarriers.save(carrierData);
    }

    public boolean isRadarContactAnnouncementOn() {
        return Database.withDao(PlayerDao.class, dao -> dao.get().isRadarAnnouncementOn());
    }

    public void setRadarContactAnnouncementOn(boolean isOn) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setRadarAnnouncementOn(isOn);
            dao.save(player);
            return Void.class;
        });
    }

    public String getVariablePlayerName() {
        String alternativeName = trimToNull(getAlternativeName());
        String playerName = alternativeName != null ? alternativeName : trimToNull(getPlayerName());
        String playerMilitaryRank = Ranks.getLocalizedRankName(getPlayerHighestMilitaryRank());
        String playerHonorific = Ranks.getPlayerHonorific(
                getRankAndProgressDto().getCombatRankEmpire(),
                getRankAndProgressDto().getCombatRankFederation()
        );


        List<String> result = Arrays.stream(
                new String[]{alternativeName, playerHonorific, playerName, playerMilitaryRank}
        ).map(name -> trimToNull(name)).filter(Objects::nonNull).toList();
        if (result.isEmpty()) {
            return "Commander";
        }

        return result.get(new Random().nextInt(result.size()));
    }

    public String getConfiguredPlayerName() {
        String alternativeName = trimToNull(getAlternativeName());
        if (alternativeName != null) return alternativeName;

        String playerName = trimToNull(getPlayerName());
        if (playerName != null) return playerName;

        String inGameName = trimToNull(getInGameName());
        return inGameName != null ? inGameName : "Commander";
    }

    public void setShipAutoDeparted(boolean isDeparted) {
        this.shipAutoDeparted = isDeparted;
    }

    public boolean isShipAutoDeparted() {
        return shipAutoDeparted;
    }
}

