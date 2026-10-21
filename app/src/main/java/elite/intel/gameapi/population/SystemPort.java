package elite.intel.gameapi.population;

import java.util.Optional;

/**
 * One port of a star system as we know it: the type its docking reported and the type the FSS reported, either
 * of which may be missing.
 *
 * @param name       the port's name, which is how the docking and the signal are matched
 * @param dockedType the {@code Docked} event's {@code StationType}, null when we never docked there
 * @param signalType the FSS {@code SignalType}, null when the FSS never reported it
 * @param body       the planet or moon a settlement stands on, null for a port we never approached on a surface
 */
public record SystemPort(String name, String dockedType, String signalType, String body) {

    /**
     * The port's size, empty when it is not a port people live in.
     * <p>
     * The docking's type wins - it tells a settlement from an outpost, which the FSS files under one name -
     * except for a colony-built station, whose docking says only "SurfaceStation": its signal knows the shape.
     * With neither to go by, a colony station counts as large (they offer large pads) and a settlement we only
     * flew past as small.
     */
    public Optional<PortSize> size() {
        boolean colonyStation = PortSize.SURFACE_STATION.equalsIgnoreCase(dockedType);
        if (dockedType != null && !colonyStation) return PortSize.of(dockedType);

        Optional<PortSize> fromSignal = PortSize.of(signalType);
        if (fromSignal.isPresent()) return fromSignal;
        if (colonyStation) return Optional.of(PortSize.LARGE);
        if (isSettlement()) return Optional.of(PortSize.SMALL);
        return Optional.empty();
    }

    public boolean isSettlementOn(String bodyName) {
        return isSettlement() && body.equalsIgnoreCase(bodyName);
    }

    private boolean isSettlement() {
        return body != null && !body.isBlank();
    }
}
