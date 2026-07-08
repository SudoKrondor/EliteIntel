package elite.intel.session;

/**
 * The commander's current physical context (where they are and in what vehicle/on foot), derived from the
 * game Status flags plus the current location. This is a stable, display-agnostic classification; the UI
 * maps each constant to a localized label via {@link #i18nKey()}. The classification itself is owned by
 * {@link Status#getSituation}, which decodes the flags.
 */
public enum PlayerSituation {

    ON_FOOT_STATION("location.situation.onFootStation"),
    ON_FOOT_HANGAR("location.situation.onFootHangar"),
    ON_FOOT_SOCIAL("location.situation.onFootSocial"),
    ON_FOOT_PLANET("location.situation.onFootPlanet"),
    ON_FOOT("location.situation.onFoot"),
    IN_SRV("location.situation.inSrv"),
    IN_FIGHTER("location.situation.inFighter"),
    IN_TAXI("location.situation.inTaxi"),
    IN_SHIP_DOCKED("location.situation.inShipDocked"),
    IN_SHIP_LANDED("location.situation.inShipLanded"),
    IN_SHIP_GLIDE("location.situation.inShipGlide"),
    IN_SHIP_SUPERCRUISE("location.situation.inShipSupercruise"),
    IN_SHIP_RING("location.situation.inShipRing"),
    IN_SHIP_ORBIT("location.situation.inShipOrbit"),
    IN_SHIP_DEEP_SPACE("location.situation.inShipDeepSpace"),
    UNKNOWN("location.situation.unknown");

    private final String i18nKey;

    PlayerSituation(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    /** Resource-bundle key for this situation's localized label. */
    public String i18nKey() {
        return i18nKey;
    }
}
