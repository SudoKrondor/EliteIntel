package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.managers.Whereabouts;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.population.PopulationEstimate;
import elite.intel.gameapi.population.PortSize;
import elite.intel.gameapi.population.SystemPort;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.*;

/**
 * How many people live where the commander is, for immersion. The star system's figure is the game's own; the
 * game says nothing below that, so a station's and a planet's are shares of it, see {@link PopulationEstimate}.
 */
@RegisterQuery
public class AnalyzePopulationQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_population";

    /**
     * The surface scanner's signal for a human presence on a body: one per settlement or port.
     */
    private static final String HUMAN_SIGNAL = "$SAA_SignalType_Human;";

    private static final String INSTRUCTIONS = """
            Answer the commander's question about how many people live here.
            
            Data fields:
            - place: where the commander is - docked at a station, at a planet, or in the star system
            - starSystem, systemPopulation: the star system and its population as the game reports it
            - stationName, stationPopulationEstimate: the station we are docked at and our estimate of the people
              living there. A null estimate means it has no residents to estimate: a fleet carrier, construction
              site or megaship has a crew, not a population.
            - bodyName, settlementsOnBody, bodyPopulationEstimate: the planet or moon we are at, how many
              settlements we know of on it, and our estimate of the people living in them. A null estimate means
              we know of no settlement there.
            
            Rules:
            - The game reports population for the star system only. Every station and planet figure is our
              estimate: always say it is approximate.
            - Answer about the place in place. Mention systemPopulation only for context or when asked.
            - When the estimate for the place is null, say you do not know, and give the system's population.
            - A systemPopulation of 0 means the system is uninhabited.
            """;

    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public String llmDescription() {
        return "Report how many people live where the commander is: the star system's population, or an estimate for the station they are docked at or the planet they are on.";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        Optional<Whereabouts> here = locationManager.here();
        if (here.isEmpty()) return process(StringUtls.localizedResponse("query.noData"));

        long systemAddress = here.get().position().systemAddress();
        LocationDto system = here.get().system();
        List<SystemPort> ports = portsOf(systemAddress, system);
        List<LocationDto> bodies = locationManager.findBodies(systemAddress);
        PopulationEstimate estimate = new PopulationEstimate(system.getPopulation(), allSizes(ports, bodies));
        DataDto systemOnly = DataDto.system(here.get().starSystem(), system.getPopulation());

        Status status = Status.getInstance();
        DataDto data = systemOnly;
        if (isAtPort(status)) {
            data = atStation(systemOnly, ports, estimate);
        } else if (status.hasLatLong()) {
            data = atBody(systemOnly, ports, estimate, systemAddress, status.getStatus().getBodyName());
        }
        return process(new AiDataStruct(INSTRUCTIONS, data), originalUserInput);
    }

    private static boolean isAtPort(Status status) {
        return status.isDocked() || status.isOnFootInStation() || status.isOnFootInHangar()
                || status.isOnFootSocialSpace();
    }

    private DataDto atStation(DataDto systemOnly, List<SystemPort> ports, PopulationEstimate estimate) {
        LocationDto station = locationManager.findCurrentStation();
        String name = station.getStationName();
        if (name == null) return systemOnly;

        SystemPort port = ports.stream().filter(p -> key(p.name()).equals(key(name))).findFirst()
                .orElseGet(() -> new SystemPort(name, station.getRecordedStationType(), null, station.getParentBodyName()));
        Long population = port.size().map(estimate::of).orElse(null);
        return systemOnly.atStation(name, population);
    }

    /**
     * A planet's people live in its settlements, so the estimate is theirs: the ones we flew to, and as many
     * more as the surface scanner counted. With neither, we do not know of anyone living there.
     */
    private DataDto atBody(DataDto systemOnly, List<SystemPort> ports, PopulationEstimate estimate,
                           long systemAddress, String bodyName) {
        if (bodyName == null || bodyName.isBlank()) return systemOnly;
        LocationDto body = locationManager.findBySystemAddress(systemAddress, bodyName);
        String spokenName = body.getPlanetShortName() == null ? bodyName : body.getPlanetShortName();

        List<PortSize> settlements = settlementsOn(body, bodyName, ports);
        if (settlements.isEmpty()) return systemOnly.atBody(spokenName, null, null);
        return systemOnly.atBody(spokenName, settlements.size(), estimate.ofAll(settlements));
    }

    /**
     * Every port the system is known to have: the ones on record from a docking or an approach, matched by name
     * to what the FSS reported, and the FSS's own that we never visited.
     */
    private List<SystemPort> portsOf(long systemAddress, LocationDto system) {
        Map<String, FssSignalDto> signals = new LinkedHashMap<>();
        for (FssSignalDto signal : system.getDetectedSignals()) {
            String name = StationName.display(signal.getSignalName());
            if (name != null && !name.isBlank()) signals.put(key(name), signal);
        }

        List<SystemPort> ports = new ArrayList<>();
        for (LocationDto station : locationManager.findStationsInCurrentStarSystem(systemAddress)) {
            String name = station.getStationName();
            if (name == null || name.isBlank()) continue;
            FssSignalDto signal = signals.remove(key(name));
            ports.add(new SystemPort(name, station.getRecordedStationType(),
                    signal == null ? null : signal.getSignalType(), station.getParentBodyName()));
        }
        signals.values().forEach(signal ->
                ports.add(new SystemPort(signal.getSignalName(), null, signal.getSignalType(), null)));
        return ports;
    }

    /**
     * The size of every port the system has, the settlements the surface scanner counted and we never saw
     * included: the share of each is taken against all of them.
     */
    private static List<PortSize> allSizes(List<SystemPort> ports, List<LocationDto> bodies) {
        List<PortSize> sizes = new ArrayList<>();
        ports.forEach(port -> port.size().ifPresent(sizes::add));
        for (LocationDto body : bodies) {
            sizes.addAll(unseenSettlements(body, body.getPlanetName(), ports));
        }
        return sizes;
    }

    private static List<PortSize> settlementsOn(LocationDto body, String bodyName, List<SystemPort> ports) {
        List<PortSize> sizes = new ArrayList<>();
        ports.stream().filter(port -> port.isSettlementOn(bodyName))
                .forEach(port -> port.size().ifPresent(sizes::add));
        sizes.addAll(unseenSettlements(body, bodyName, ports));
        return sizes;
    }

    /**
     * Settlements the surface scanner counted on a body beyond those we flew to, as small ones: we know only
     * that they are there.
     */
    private static List<PortSize> unseenSettlements(LocationDto body, String bodyName, List<SystemPort> ports) {
        if (bodyName == null) return List.of();
        long seen = ports.stream().filter(port -> port.isSettlementOn(bodyName)).count();
        int unseen = (int) Math.max(0, humanSignals(body) - seen);
        return Collections.nCopies(unseen, PortSize.SMALL);
    }

    /**
     * The surface scanner's count of human signals on a body. Taken as the largest reported rather than the
     * sum: the record keeps every scan, and a body mapped twice lists its signals twice.
     */
    private static int humanSignals(LocationDto body) {
        List<SAASignalsFoundEvent.Signal> signals = body.getSaaSignals();
        if (signals == null) return 0;
        return signals.stream().filter(signal -> HUMAN_SIGNAL.equals(signal.getType()))
                .mapToInt(SAASignalsFoundEvent.Signal::getCount).max().orElse(0);
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    record DataDto(
            String place,
            String starSystem,
            long systemPopulation,
            String stationName,
            Long stationPopulationEstimate,
            String bodyName,
            Integer settlementsOnBody,
            Long bodyPopulationEstimate
    ) implements ToYamlConvertable {

        static DataDto system(String starSystem, long systemPopulation) {
            return new DataDto("star system", starSystem, systemPopulation, null, null, null, null, null);
        }

        DataDto atStation(String name, Long population) {
            return new DataDto("station", starSystem, systemPopulation, name, population, null, null, null);
        }

        DataDto atBody(String name, Integer settlements, Long population) {
            return new DataDto("planet", starSystem, systemPopulation, null, null, name, settlements, population);
        }

        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
