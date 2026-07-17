package elite.intel.session;

/**
 * The commander's current physical context (where they are and in what vehicle/on foot), derived from the
 * game Status flags plus the current location. This is a stable, display-agnostic classification; consumers
 * map each constant to a localized label via {@link #i18nKey()}. The classification itself is owned by
 * {@link Status#getSituation}, which decodes the flags.
 */
public enum PlayerSituation {

    ON_FOOT_STATION("game.situation.onFootStation"),
    ON_FOOT_HANGAR("game.situation.onFootHangar"),
    ON_FOOT_SOCIAL("game.situation.onFootSocial"),
    ON_FOOT_PLANET("game.situation.onFootPlanet"),
    ON_FOOT("game.situation.onFoot"),
    IN_SRV("game.situation.inSrv"),
    IN_FIGHTER("game.situation.inFighter"),
    IN_TAXI("game.situation.inTaxi"),
    IN_SHIP_DOCKED("game.situation.inShipDocked"),
    IN_SHIP_LANDED("game.situation.inShipLanded"),
    IN_SHIP_GLIDE("game.situation.inShipGlide"),
    IN_SHIP_SUPERCRUISE("game.situation.inShipSupercruise"),
    IN_SHIP_RING("game.situation.inShipRing"),
    IN_SHIP_ORBIT("game.situation.inShipOrbit"),
    IN_SHIP_DEEP_SPACE("game.situation.inShipDeepSpace"),
    UNKNOWN("game.situation.unknown");

    private final String i18nKey;

    PlayerSituation(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    /** Resource-bundle key for this situation's localized label. */
    public String i18nKey() {
        return i18nKey;
    }
}
