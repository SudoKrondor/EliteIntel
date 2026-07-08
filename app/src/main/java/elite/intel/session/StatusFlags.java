package elite.intel.session;

public abstract class StatusFlags {

    // Flags (Ship/Flight States) - Odyssey-specific bitmasks
    private static final long DOCKED = 1L;
    private static final long LANDED = 2L;
    private static final long LANDING_GEAR_DOWN = 4L;
    private static final long SHIELDS_UP = 8L;
    private static final long SUPERCRUISE = 16L;
    private static final long FLIGHT_ASSIST_OFF = 32L;
    private static final long HARDPOINTS_DEPLOYED = 64L;
    private static final long IN_WING = 128L;
    private static final long LIGHTS_ON = 256L;
    private static final long CARGO_SCOOP_DEPLOYED = 512L;
    private static final long SILENT_RUNNING = 1024L;
    private static final long SCOOPING_FUEL = 2048L;
    private static final long SRV_HANDBRAKE = 4096L;
    private static final long SRV_TURRET_VIEW = 8192L;
    private static final long SRV_TURRET_RETRACTED = 16384L;
    private static final long SRV_DRIVE_ASSIST = 32768L;
    private static final long FSD_MASS_LOCKED = 65536L;
    private static final long FSD_CHARGING = 131072L;
    private static final long FSD_COOLDOWN = 262144L;
    private static final long LOW_FUEL = 524288L;
    private static final long OVERHEATING = 1048576L;
    private static final long HAS_LAT_LONG = 2097152L;
    private static final long IN_DANGER = 4194304L;
    private static final long BEING_INTERDICTED = 8388608L;
    private static final long IN_MAIN_SHIP = 16777216L;
    private static final long IN_FIGHTER = 33554432L;
    private static final long IN_SRV = 67108864L;
    private static final long HUD_ANALYSIS_MODE = 134217728L;
    private static final long NIGHT_VISION = 268435456L;
    private static final long ALT_FROM_AVG_RADIUS = 536870912L;
    private static final long FSD_JUMP = 1073741824L;
    private static final long SRV_HIGH_BEAM = 2147483648L;
    private static final long SRV_SPECIFIC_FLAGS_1 = 203427848L;
    private static final long SRV_SPECIFIC_FLAGS_2 = 203444494L;

    // Flags2 (On-Foot/Suit States) - Odyssey-specific
    private static final long ON_FOOT = 1L;
    private static final long IN_TAXI = 2L;
    private static final long IN_MULTICREW = 4L;
    private static final long ON_FOOT_IN_STATION = 8L;
    private static final long ON_FOOT_ON_PLANET = 16L;
    private static final long AIMING_DOWN_SIGHTS = 32L;
    private static final long LOW_OXYGEN_F2 = 64L;
    private static final long LOW_HEALTH_F2 = 128L;
    private static final long COLD = 256L;
    private static final long HOT = 512L;
    private static final long VERY_COLD = 1024L;
    private static final long VERY_HOT = 2048L;
    private static final long GLIDE_MODE_F2 = 4096L;
    private static final long ON_FOOT_IN_HANGAR = 8192L;
    private static final long ON_FOOT_SOCIAL_SPACE = 16384L;
    private static final long ON_FOOT_EXTERIOR = 32768L;
    private static final long BREATHABLE_ATMOSPHERE = 65536L;
    private static final long TELEPRESENCE_MULTICREW = 131072L;
    private static final long PHYSICAL_MULTICREW = 262144L;
    private static final long FSD_HYPERDRIVE_CHARGING = 524288L;

    /**
     * Decoded values of the GuiFocus field from Status.json.
     * Tells you which panel or mode the player currently has open.
     * Note: tab-level state within a panel is not exposed by the journal API.
     */
    public enum GuiFocus {
        NO_FOCUS(0),
        INTERNAL_PANEL(1),   // Right panel
        EXTERNAL_PANEL(2),   // Left panel
        COMMS_PANEL(3),      // Top panel
        ROLE_PANEL(4),       // Bottom panel
        STATION_SERVICES(5),
        GALAXY_MAP(6),
        SYSTEM_MAP(7),
        ORRERY(8),
        FSS_MODE(9),
        SAA_MODE(10),
        CODEX(11),
        UNKNOWN(-1);

        private final int value;

        GuiFocus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static GuiFocus fromValue(int value) {
            for (GuiFocus focus : values()) {
                if (focus.value == value) return focus;
            }
            return UNKNOWN;
        }
    }

    // --- Flags bitmask helpers ---

    public boolean isDocked(long flags) {
        return (flags & DOCKED) != 0;
    }

    public boolean isLanded(long flags) {
        return (flags & LANDED) != 0;
    }

    public boolean isLandingGearDown(long flags) {
        return (flags & LANDING_GEAR_DOWN) != 0;
    }

    public boolean isShieldsUp(long flags) {
        return (flags & SHIELDS_UP) != 0;
    }

    public boolean isInSupercruise(long flags) {
        return (flags & SUPERCRUISE) != 0;
    }

    public boolean isFlightAssistOff(long flags) {
        return (flags & FLIGHT_ASSIST_OFF) != 0;
    }

    public boolean isHardpointsDeployed(long flags) {
        return (flags & HARDPOINTS_DEPLOYED) != 0;
    }

    public boolean isInWing(long flags) {
        return (flags & IN_WING) != 0;
    }

    public boolean isLightsOn(long flags) {
        return (flags & LIGHTS_ON) != 0;
    }

    public boolean isCargoScoopDeployed(long flags) {
        return (flags & CARGO_SCOOP_DEPLOYED) != 0;
    }

    public boolean isSilentRunning(long flags) {
        return (flags & SILENT_RUNNING) != 0;
    }

    public boolean isScoopingFuel(long flags) {
        return (flags & SCOOPING_FUEL) != 0;
    }

    public boolean isSrvHandbrake(long flags) {
        return (flags & SRV_HANDBRAKE) != 0;
    }

    public boolean isSrvTurretView(long flags) {
        return (flags & SRV_TURRET_VIEW) != 0;
    }

    public boolean isSrvTurretRetracted(long flags) {
        return (flags & SRV_TURRET_RETRACTED) != 0;
    }

    public boolean isSrvDriveAssist(long flags) {
        return (flags & SRV_DRIVE_ASSIST) != 0;
    }

    public boolean isFsdMassLocked(long flags) {
        return (flags & FSD_MASS_LOCKED) != 0;
    }

    public boolean isFsdCharging(long flags) {
        return (flags & FSD_CHARGING) != 0;
    }

    public boolean isFsdCooldown(long flags) {
        return (flags & FSD_COOLDOWN) != 0;
    }

    public boolean isLowFuel(long flags) {
        return (flags & LOW_FUEL) != 0;
    }

    public boolean isOverheating(long flags) {
        return (flags & OVERHEATING) != 0;
    }

    public boolean hasLatLong(long flags) {
        return (flags & HAS_LAT_LONG) != 0;
    }

    public boolean isInDanger(long flags) {
        return (flags & IN_DANGER) != 0;
    }

    public boolean isBeingInterdicted(long flags) {
        return (flags & BEING_INTERDICTED) != 0;
    }

    public boolean isInMainShip(long flags) {
        return (flags & IN_MAIN_SHIP) != 0;
    }

    public boolean isInFighter(long flags) {
        return (flags & IN_FIGHTER) != 0;
    }

    public boolean isInSrv(long flags) {
        return (flags & IN_SRV) != 0 || flags == SRV_SPECIFIC_FLAGS_1 || flags == SRV_SPECIFIC_FLAGS_2;
    }

    public boolean isHudAnalysisMode(long flags) {
        return (flags & HUD_ANALYSIS_MODE) != 0;
    }

    public boolean isNightVision(long flags) {
        return (flags & NIGHT_VISION) != 0;
    }

    public boolean isAltFromAvgRadius(long flags) {
        return (flags & ALT_FROM_AVG_RADIUS) != 0;
    }

    public boolean isFsdJump(long flags) {
        return (flags & FSD_JUMP) != 0;
    }

    public boolean isSrvHighBeam(long flags) {
        return (flags & SRV_HIGH_BEAM) != 0;
    }

    // --- Flags2 bitmask helpers ---

    public boolean isOnFoot(long flags2) {
        return (flags2 & ON_FOOT) != 0;
    }

    public boolean isInTaxi(long flags2) {
        return (flags2 & IN_TAXI) != 0;
    }

    public boolean isInMulticrew(long flags2) {
        return (flags2 & IN_MULTICREW) != 0;
    }

    public boolean isOnFootInStation(long flags2) {
        return (flags2 & ON_FOOT_IN_STATION) != 0;
    }

    public boolean isOnFootOnPlanet(long flags2) {
        return (flags2 & ON_FOOT_ON_PLANET) != 0;
    }

    public boolean isAimingDownSights(long flags2) {
        return (flags2 & AIMING_DOWN_SIGHTS) != 0;
    }

    public boolean isLowOxygenF2(long flags2) {
        return (flags2 & LOW_OXYGEN_F2) != 0;
    }

    public boolean isLowHealthF2(long flags2) {
        return (flags2 & LOW_HEALTH_F2) != 0;
    }

    public boolean isCold(long flags2) {
        return (flags2 & COLD) != 0;
    }

    public boolean isHot(long flags2) {
        return (flags2 & HOT) != 0;
    }

    public boolean isVeryCold(long flags2) {
        return (flags2 & VERY_COLD) != 0;
    }

    public boolean isVeryHot(long flags2) {
        return (flags2 & VERY_HOT) != 0;
    }

    public boolean isGlideModeF2(long flags2) {
        return (flags2 & GLIDE_MODE_F2) != 0;
    }

    public boolean isOnFootInHangar(long flags2) {
        return (flags2 & ON_FOOT_IN_HANGAR) != 0;
    }

    public boolean isOnFootSocialSpace(long flags2) {
        return (flags2 & ON_FOOT_SOCIAL_SPACE) != 0;
    }

    public boolean isOnFootExterior(long flags2) {
        return (flags2 & ON_FOOT_EXTERIOR) != 0;
    }

    public boolean isBreathableAtmosphere(long flags2) {
        return (flags2 & BREATHABLE_ATMOSPHERE) != 0;
    }

    public boolean isTelepresenceMulticrew(long flags2) {
        return (flags2 & TELEPRESENCE_MULTICREW) != 0;
    }

    public boolean isPhysicalMulticrew(long flags2) {
        return (flags2 & PHYSICAL_MULTICREW) != 0;
    }

    public boolean isFsdHyperdriveCharging(long flags2) {
        return (flags2 & FSD_HYPERDRIVE_CHARGING) != 0;
    }

    /**
     * The inverse of {@link Status#getSituation}: a minimal {@code {flags, flags2}} pair that classifies as the
     * given situation. Lets a caller evaluate context-gated action visibility for a what-if situation by feeding
     * a {@link Status#detached} status to the existing {@code isVisibleForLLM} checks, instead of duplicating
     * each action's rule. Only the bits the classifier
     * and the visibility predicates read are set; {@code IN_SHIP_RING} is location-derived (not a flag) so it is
     * approximated as in-ship normal space, and {@code UNKNOWN} maps to all-zero flags.
     */
    public static long[] flagsForSituation(PlayerSituation situation) {
        long flags = 0L;
        long flags2 = 0L;
        switch (situation) {
            case ON_FOOT_STATION -> flags2 = ON_FOOT | ON_FOOT_IN_STATION;
            case ON_FOOT_HANGAR -> flags2 = ON_FOOT | ON_FOOT_IN_HANGAR;
            case ON_FOOT_SOCIAL -> flags2 = ON_FOOT | ON_FOOT_SOCIAL_SPACE;
            case ON_FOOT_PLANET -> flags2 = ON_FOOT | ON_FOOT_ON_PLANET;
            case ON_FOOT -> flags2 = ON_FOOT;
            case IN_TAXI -> flags2 = IN_TAXI;
            case IN_SRV -> flags = IN_SRV;
            case IN_FIGHTER -> flags = IN_FIGHTER;
            case IN_SHIP_DOCKED -> flags = IN_MAIN_SHIP | DOCKED;
            case IN_SHIP_LANDED -> flags = IN_MAIN_SHIP | LANDED;
            case IN_SHIP_GLIDE -> { flags = IN_MAIN_SHIP; flags2 = GLIDE_MODE_F2; }
            case IN_SHIP_SUPERCRUISE -> flags = IN_MAIN_SHIP | SUPERCRUISE;
            case IN_SHIP_ORBIT -> flags = IN_MAIN_SHIP | HAS_LAT_LONG;
            case IN_SHIP_RING, IN_SHIP_DEEP_SPACE -> flags = IN_MAIN_SHIP;
            case UNKNOWN -> { /* no flags set: nothing is known about the context */ }
        }
        return new long[]{flags, flags2};
    }
}