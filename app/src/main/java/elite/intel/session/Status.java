package elite.intel.session;

import elite.intel.db.dao.StatusDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.json.GsonFactory;

public class Status extends StatusFlags {

    private static volatile Status instance; // Singleton instance
    private boolean isFighterOut = false;
    private boolean okToAnnounceLoadout = true;

    private Status() {
        //
    }

    public static Status getInstance() {
        if (instance == null) {
            synchronized (Status.class) {
                if (instance == null) {
                    instance = new Status();
                }
            }
        }
        return instance;
    }

    public GameEvents.StatusEvent getStatus() {
        return Database.withDao(StatusDao.class, dao -> {
            StatusDao.Status status = dao.getStatus();
            GameEvents.StatusEvent result = new GameEvents.StatusEvent();
            result.setAltitude(status.getAltitude());
            result.setBalance(status.getBalance());
            result.setCargo(status.getCargo());
            result.setEvent(status.getEvent());
            result.setFlags(status.getFlags());
            result.setFlags2(status.getFlags2());
            result.setFireGroup(status.getFireGroup());
            result.setGuiFocus(status.getGuiFocus());
            result.setHeading(status.getHeading());
            result.setLatitude(status.getLatituge());
            result.setLongitude(status.getLongitude());
            result.setPlanetRadius(status.getPlanetRadius());
            result.setPips(pipsFromString(status.getPips()));
            result.setLegalState(status.getLegalState());
            result.setDestination(destinationFromJson(status.getDestination()));
            result.setOxygen(status.getOxygen());
            result.setHealth(status.getHealth());
            result.setTemperature(status.getTemperature());
            result.setSelectedWeapon(status.getSelectedWeapon());
            result.setGravity(status.getGravity());
            return result;
        });
    }

    public void setStatus(GameEvents.StatusEvent event) {
        Database.withDao(StatusDao.class, dao -> {
            StatusDao.Status status = new StatusDao.Status();
            status.setAltitude(event.getAltitude());
            status.setBalance(event.getBalance());
            status.setCargo(event.getCargo());
            status.setEvent(event.getEvent());
            status.setFlags(event.getFlags());
            status.setFlags2(event.getFlags2());
            status.setFireGroup(event.getFireGroup());
            status.setGuiFocus(event.getGuiFocus());
            status.setHeading(event.getHeading());
            status.setLatituge(event.getLatitude());
            status.setLongitude(event.getLongitude());
            status.setPlanetRadius(event.getPlanetRadius());
            status.setTimestamp(event.getTimestamp());
            status.setPips(pipsToString(event.getPips()));
            status.setLegalState(event.getLegalState());
            status.setDestination(destinationToJson(event.getDestination()));
            status.setOxygen(event.getOxygen());
            status.setHealth(event.getHealth());
            status.setTemperature(event.getTemperature());
            status.setSelectedWeapon(event.getSelectedWeapon());
            status.setGravity(event.getGravity());
            dao.save(status);
            return null;
        });
    }

    // --- Serialization helpers ---

    private static String pipsToString(int[] pips) {
        if (pips == null || pips.length < 3) return null;
        return pips[0] + "," + pips[1] + "," + pips[2];
    }

    private static int[] pipsFromString(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(",");
        if (parts.length < 3) return null;
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())};
    }

    private static String destinationToJson(GameEvents.StatusEvent.Destination dest) {
        if (dest == null) return null;
        return GsonFactory.getGson().toJson(dest);
    }

    private static GameEvents.StatusEvent.Destination destinationFromJson(String json) {
        if (json == null || json.isBlank()) return null;
        return GsonFactory.getGson().fromJson(json, GameEvents.StatusEvent.Destination.class);
    }

    // --- GuiFocus ---

    /**
     * Returns the decoded GUI panel/mode the player currently has open.
     */
    public GuiFocus getGuiFocus() {
        return GuiFocus.fromValue(getStatus().getGuiFocus());
    }

    /**
     * Returns true if the player has no panel open (pure cockpit/on-foot view).
     */
    public boolean isNoFocus() {
        return getGuiFocus() == GuiFocus.NO_FOCUS;
    }

    public boolean isInternalPanelOpen() {
        return getGuiFocus() == GuiFocus.INTERNAL_PANEL;
    }

    public boolean isExternalPanelOpen() {
        return getGuiFocus() == GuiFocus.EXTERNAL_PANEL;
    }

    public boolean isCommsPanelOpen() {
        return getGuiFocus() == GuiFocus.COMMS_PANEL;
    }

    public boolean isRolePanelOpen() {
        return getGuiFocus() == GuiFocus.CENTRAL_PANEL;
    }

    public boolean isStationServicesOpen() {
        return getGuiFocus() == GuiFocus.STATION_SERVICES;
    }

    public boolean isGalaxyMapOpen() {
        return getGuiFocus() == GuiFocus.GALAXY_MAP;
    }

    public boolean isSystemMapOpen() {
        return getGuiFocus() == GuiFocus.SYSTEM_MAP;
    }

    public boolean isOrreryOpen() {
        return getGuiFocus() == GuiFocus.ORRERY;
    }

    public boolean isFssModeActive() {
        return getGuiFocus() == GuiFocus.FSS_MODE;
    }

    public boolean isSaaModeActive() {
        return getGuiFocus() == GuiFocus.SAA_MODE;
    }

    public boolean isCodexOpen() {
        return getGuiFocus() == GuiFocus.CODEX;
    }

    // --- Pips ---

    /**
     * Returns energy distribution as an int[3] array in half-pips: [SYS, ENG, WPN].
     * Each value is 0–8 (0 = no power, 8 = full 4 pips). May be null if not in a ship.
     */
    public int[] getPips() {
        return getStatus().getPips();
    }

    public int getSysPips() {
        int[] pips = getPips();
        return pips != null ? pips[0] : 0;
    }

    public int getEngPips() {
        int[] pips = getPips();
        return pips != null ? pips[1] : 0;
    }

    public int getWpnPips() {
        int[] pips = getPips();
        return pips != null ? pips[2] : 0;
    }

    // --- LegalState ---

    /**
     * Returns the player's current legal state string as reported by the game.
     * Known values: Clean, IllegalCargo, Speeding, Wanted, Hostile,
     * PassengerWanted, Warrant, Allied, Thargoid.
     */
    public String getLegalState() {
        return getStatus().getLegalState();
    }

    public boolean isClean() {
        return "Clean".equals(getLegalState());
    }

    public boolean isWanted() {
        return "Wanted".equals(getLegalState());
    }

    public boolean isHostile() {
        return "Hostile".equals(getLegalState());
    }

    public boolean isSpeeding() {
        return "Speeding".equals(getLegalState());
    }

    public boolean hasIllegalCargo() {
        return "IllegalCargo".equals(getLegalState());
    }

    public boolean hasPassengerWanted() {
        return "PassengerWanted".equals(getLegalState());
    }

    public boolean hasWarrant() {
        return "Warrant".equals(getLegalState());
    }

    // --- Destination ---

    /**
     * Returns the current nav-lock destination, or null if none is set.
     * Contains System address, Body id, and Name.
     */
    public GameEvents.StatusEvent.Destination getDestination() {
        return getStatus().getDestination();
    }

    public String getDestinationName() {
        GameEvents.StatusEvent.Destination dest = getDestination();
        return dest != null ? dest.getName() : null;
    }

    public boolean hasDestination() {
        return getDestination() != null;
    }

    // --- On-foot vitals (Odyssey only, present only when on foot) ---

    /**
     * Suit oxygen level, 0.0 to 1.0. Only present when on foot.
     * Returns -1.0 if not available.
     */
    public double getOxygen() {
        Double oxygen = getStatus().getOxygen();
        return oxygen != null ? oxygen : -1.0;
    }

    /**
     * Suit health level, 0.0 to 1.0. Only present when on foot.
     * Returns -1.0 if not available.
     */
    public double getHealth() {
        Double health = getStatus().getHealth();
        return health != null ? health : -1.0;
    }

    /**
     * Ambient temperature in Kelvin. Only present when on foot.
     * Returns -1.0 if not available.
     */
    public double getTemperature() {
        Double temperature = getStatus().getTemperature();
        return temperature != null ? temperature : -1.0;
    }

    /**
     * Gravity relative to 1G. Only present when on foot.
     * Returns -1.0 if not available.
     */
    public double getGravity() {
        Double gravity = getStatus().getGravity();
        return gravity != null ? gravity : -1.0;
    }

    /**
     * Currently selected weapon name. Only present when on foot.
     * Returns null if not available.
     */
    public String getSelectedWeapon() {
        return getStatus().getSelectedWeapon();
    }

    public boolean hasWeaponSelected() {
        String weapon = getSelectedWeapon();
        return weapon != null && !weapon.isBlank();
    }

    // --- Flags (ship/flight) ---

    public boolean isInSrv() {
        return isInSrv(getStatus().getFlags());
    }

    public boolean isInMainShip() {
        return isInMainShip(getStatus().getFlags());
    }

    public boolean isInFighter() {
        return isInFighter(getStatus().getFlags());
    }

    public boolean isInWing() {
        return isInWing(getStatus().getFlags());
    }

    public boolean isHardpointsDeployed() {
        return isHardpointsDeployed(getStatus().getFlags());
    }

    public boolean isCargoScoopDeployed() {
        return isCargoScoopDeployed(getStatus().getFlags());
    }

    public boolean isLightsOn() {
        return isLightsOn(getStatus().getFlags());
    }

    public boolean isSilentRunning() {
        return isSilentRunning(getStatus().getFlags());
    }

    public boolean isScoopingFuel() {
        return isScoopingFuel(getStatus().getFlags());
    }

    public boolean isSrvHandbrake() {
        return isSrvHandbrake(getStatus().getFlags());
    }

    public boolean isNightVision() {
        return isNightVision(getStatus().getFlags());
    }

    public boolean isAnalysisMode() {
        return isHudAnalysisMode(getStatus().getFlags());
    }

    public boolean isFsdJump() {
        return isFsdJump(getStatus().getFlags());
    }

    public boolean isSrvHighBeam() {
        return isSrvHighBeam(getStatus().getFlags());
    }

    public boolean isFsdCharging() {
        return isFsdCharging(getStatus().getFlags());
    }

    public boolean isFsdCooldown() {
        return isFsdCooldown(getStatus().getFlags());
    }

    public boolean isFsdMassLocked() {
        return isFsdMassLocked(getStatus().getFlags());
    }

    public boolean isDocked() {
        return isDocked(getStatus().getFlags());
    }

    public boolean isLanded() {
        return isLanded(getStatus().getFlags());
    }

    public boolean isLandingGearDown() {
        return isLandingGearDown(getStatus().getFlags());
    }

    public boolean isShieldsUp() {
        return isShieldsUp(getStatus().getFlags());
    }

    public boolean isInSupercruise() {
        return isInSupercruise(getStatus().getFlags());
    }

    public boolean isFlightAssistOff() {
        return isFlightAssistOff(getStatus().getFlags());
    }

    public boolean isSrvTurretView() {
        return isSrvTurretView(getStatus().getFlags());
    }

    public boolean isSrvTurretRetracted() {
        return isSrvTurretRetracted(getStatus().getFlags());
    }

    public boolean isSrvDriveAssist() {
        return isSrvDriveAssist(getStatus().getFlags());
    }

    public boolean isLowFuel() {
        return isLowFuel(getStatus().getFlags());
    }

    public boolean isOverheating() {
        return isOverheating(getStatus().getFlags());
    }

    public boolean hasLatLong() {
        return hasLatLong(getStatus().getFlags());
    }

    public boolean isInDanger() {
        return isInDanger(getStatus().getFlags());
    }

    public boolean isBeingInterdicted() {
        return isBeingInterdicted(getStatus().getFlags());
    }

    public boolean isAltFromAvgRadius() {
        return isAltFromAvgRadius(getStatus().getFlags2());
    }

    // --- Flags2 (on-foot/suit) ---

    public boolean isOnFoot() {
        return isOnFoot(getStatus().getFlags2());
    }

    public boolean isAimDownSight() {
        return isAimingDownSights(getStatus().getFlags2());
    }

    public boolean isLowOxygen() {
        return isLowOxygenF2(getStatus().getFlags2());
    }

    public boolean isLowHealth() {
        return isLowHealthF2(getStatus().getFlags2());
    }

    public boolean isHot() {
        return isHot(getStatus().getFlags2());
    }

    public boolean isCold() {
        return isCold(getStatus().getFlags2());
    }

    public boolean isVeryHot() {
        return isVeryHot(getStatus().getFlags2());
    }

    public boolean isVeryCold() {
        return isVeryCold(getStatus().getFlags2());
    }

    public boolean isInTaxi() {
        return isInTaxi(getStatus().getFlags2());
    }

    public boolean isInMulticrew() {
        return isInMulticrew(getStatus().getFlags2());
    }

    public boolean isOnFootInStation() {
        return isOnFootInStation(getStatus().getFlags2());
    }

    public boolean isOnFootOnPlanet() {
        return isOnFootOnPlanet(getStatus().getFlags2());
    }

    public boolean isOnFootInHangar() {
        return isOnFootInHangar(getStatus().getFlags2());
    }

    public boolean isOnFootSocialSpace() {
        return isOnFootSocialSpace(getStatus().getFlags2());
    }

    public boolean isOnFootExterior() {
        return isOnFootExterior(getStatus().getFlags2());
    }

    public boolean isBreathableAtmosphere() {
        return isBreathableAtmosphere(getStatus().getFlags2());
    }

    public boolean isGlideMode() {
        return isGlideModeF2(getStatus().getFlags2());
    }

    public boolean isTelepresenceMulticrew() {
        return isTelepresenceMulticrew(getStatus().getFlags2());
    }

    public boolean isPhysicalMulticrew() {
        return isPhysicalMulticrew(getStatus().getFlags2());
    }

    public boolean isFsdHyperdriveCharging() {
        return isFsdHyperdriveCharging(getStatus().getFlags2());
    }

    public boolean isFighterOut() {
        return isFighterOut;
    }

    public void setFighterOut(boolean fighterOut) {
        isFighterOut = fighterOut;
    }

    public boolean isOkToAnnounceLoadout() {
        return okToAnnounceLoadout;
    }

    public void setOkToAnnounceLoadout(boolean okToAnounceLoadout) {
        this.okToAnnounceLoadout = okToAnounceLoadout;
    }

    public int getFireGroup() {
        return getStatus().getFireGroup();
    }

    // --- Composite situation ---

    /**
     * Classifies the commander's current physical context (in ship/SRV/fighter/taxi/on foot, and where:
     * docked, landed, supercruise, ring, orbit, deep space, station, planet surface) from the given Status
     * flags and location. Pure flag decoding, so it does not touch the database. Prefer this overload on the
     * live Status-event path so classification uses the event's own flags rather than a snapshot that may not
     * be persisted yet. {@code location} is consulted only to detect a planetary ring (asteroid belt) and may
     * be null.
     */
    public PlayerSituation getSituation(long flags, long flags2, LocationDto location) {
        // On foot (Odyssey): refine by where the commander is standing.
        if (isOnFoot(flags2)) {
            if (isOnFootInStation(flags2)) return PlayerSituation.ON_FOOT_STATION;
            if (isOnFootInHangar(flags2)) return PlayerSituation.ON_FOOT_HANGAR;
            if (isOnFootSocialSpace(flags2)) return PlayerSituation.ON_FOOT_SOCIAL;
            if (isOnFootOnPlanet(flags2) || isOnFootExterior(flags2)) return PlayerSituation.ON_FOOT_PLANET;
            return PlayerSituation.ON_FOOT;
        }
        if (isInTaxi(flags2)) return PlayerSituation.IN_TAXI;
        if (isInSrv(flags)) return PlayerSituation.IN_SRV;
        if (isInFighter(flags)) return PlayerSituation.IN_FIGHTER;

        // In the main ship: docked/landed take priority, then flight mode, then the surroundings.
        if (isDocked(flags)) return PlayerSituation.IN_SHIP_DOCKED;
        if (isLanded(flags)) return PlayerSituation.IN_SHIP_LANDED;
        if (isGlideModeF2(flags2)) return PlayerSituation.IN_SHIP_GLIDE;
        if (isInSupercruise(flags)) return PlayerSituation.IN_SHIP_SUPERCRUISE;
        if (location != null && location.getLocationType() == LocationDto.LocationType.PLANETARY_RING) {
            return PlayerSituation.IN_SHIP_RING;
        }
        // Normal space with a surface reference means we are close to a body (orbital flight).
        if (hasLatLong(flags)) return PlayerSituation.IN_SHIP_ORBIT;
        if (isInMainShip(flags)) return PlayerSituation.IN_SHIP_DEEP_SPACE;
        return PlayerSituation.UNKNOWN;
    }

    /**
     * Convenience overload that classifies from the persisted status snapshot. Use for on-demand refreshes,
     * not the live event path (which should pass the event's flags to avoid a persistence race).
     */
    public PlayerSituation getSituation(LocationDto location) {
        GameEvents.StatusEvent status = getStatus();
        return getSituation(status.getFlags(), status.getFlags2(), location);
    }

    /**
     * A detached Status whose flag decoding reflects the given raw flags instead of the live game snapshot,
     * with no database access and no effect on the singleton. Every flag-derived getter routes through
     * {@link #getStatus()}, which this overrides to return the fixed flags, so context-gated visibility checks
     * ({@code isVisibleForLLM}) can be evaluated for a what-if situation - see {@link StatusFlags#flagsForSituation}.
     * Only flag-derived getters are meaningful; non-flag fields (balance, cargo, ...) are empty.
     */
    public static Status detached(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = new GameEvents.StatusEvent();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        return new Status() {
            @Override public GameEvents.StatusEvent getStatus() {
                return snapshot;
            }
        };
    }

    /**
     * A {@link #detached(long, long)} Status standing in for the given what-if situation, so context-gated
     * visibility can be evaluated for it without actually being in that situation. Composes
     * {@link StatusFlags#flagsForSituation} with {@link #detached(long, long)}.
     */
    public static Status detached(PlayerSituation situation) {
        long[] flags = flagsForSituation(situation);
        return detached(flags[0], flags[1]);
    }
}