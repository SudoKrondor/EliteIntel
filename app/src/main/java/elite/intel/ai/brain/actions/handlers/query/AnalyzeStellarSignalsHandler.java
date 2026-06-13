package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.*;

public class AnalyzeStellarSignalsHandler extends BaseQueryAnalyzer implements QueryHandler {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        String instructions = """
                You are the ship's AI giving the commander a system briefing.
                Structure the answer in this order, skipping any section with no data:
                
                1. SYSTEM OVERVIEW  one sentence covering allegiance, security level, and population.
                   If controllingPower is set, add who holds powerplay control.
                   Mention government type and primary economy.
                
                2. STATIONS & OUTPOSTS  list each entry from the stations field.
                   Include its type and controlling faction. If stations is empty, say none are on record.
                
                3. POINTS OF INTEREST  from discoveredSignals:
                   - Resource Extraction Sites (Low, Medium, High, Hazardous) are bounty hunting grounds,
                     not materials sites. Describe them as such.
                   - Conflict Zones are active combat areas for mercenary work.
                   - Other named POIs (megaships, nav beacons, tourist beacons) get a brief mention.
                   - Fleet carriers: state the count only, individual names are not useful.
                
                4. EXPLORATION DATA  from stellarObjectSignals:
                   - Biological signals: name the bodies and counts.
                   - Geological signals: name the bodies and counts.
                   - Mining hotspots: name the ring and hotspot types with counts.
                
                Data fields:
                - systemInfo:
                  - starSystem: system name
                  - allegiance: political allegiance (Federation / Empire / Alliance / Independent)
                  - government: government type
                  - security: security level
                  - economy: primary economy
                  - population: inhabitants (0 = uninhabited)
                  - controllingPower: powerplay faction holding this system (null if none)
                - stations: docking locations with known details (commander visited or started here)
                  - name: station or outpost name
                  - type: station type (Orbis, Coriolis, Outpost, PlanetaryPort, MegaShip, etc.)
                  - controllingFaction: local faction that controls this station (null if unknown)
                  - economy: station economy type (null if unknown)
                - discoveredSignals: scanned points of interest (FSS / nav scan)
                  - locationName: body where detected (null = system-wide or unknown body)
                  - signalName: name or label
                  - signalType: type classification
                - stellarObjectSignals: surface and ring signal counts per body
                  - bodyName: planet or moon (null for rings)
                  - ringName: ring name (null for planets/moons)
                  - signalType: signal category
                  - hotspotsAndSignals: map of signal subtype to count
                """;

        Collection<LocationDto> locations = locationManager.findAllBySystemAddress(
                playerSession.getLocationData().getSystemAddress());

        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                extractSystemInfo(locations),
                                extractStations(locations),
                                aggregateSignals(locations),
                                toDiscoveredSignals(locations)
                        )
                ),
                originalUserInput
        );
    }

    private SystemInfoDto extractSystemInfo(Collection<LocationDto> locations) {
        LocationDto best = null;
        for (LocationDto loc : locations) {
            if (loc.getLocationType() == LocationDto.LocationType.PRIMARY_STAR) {
                best = loc;
                break;
            }
        }
        if (best == null) {
            for (LocationDto loc : locations) {
                if (loc.getAllegiance() != null || loc.getGovernment() != null) {
                    best = loc;
                    break;
                }
            }
        }
        if (best == null) return null;
        return new SystemInfoDto(
                best.getStarName(),
                best.getAllegiance(),
                best.getGovernment(),
                best.getSecurity(),
                best.getEconomy(),
                best.getPopulation(),
                best.getControllingPower()
        );
    }

    private List<ToYamlConvertable> extractStations(Collection<LocationDto> locations) {
        List<ToYamlConvertable> result = new ArrayList<>();
        for (LocationDto loc : locations) {
            String name = loc.getStationName();
            if (name == null || name.isBlank()) continue;
            result.add(new StationEntry(
                    name,
                    loc.getStationType(),
                    loc.getStationFaction(),
                    loc.getStationEconomy()
            ));
        }
        return result;
    }

    private List<ToYamlConvertable> aggregateSignals(Collection<LocationDto> locations) {
        List<ToYamlConvertable> result = new ArrayList<>();
        // body → ringId → type → count
        Map<String, Map<String, Map<String, Integer>>> grouping = new LinkedHashMap<>();
        for (LocationDto location : locations) {
            String body = location.getPlanetShortName();
            if (body == null || body.isBlank()) continue;

            String ringName = location.getPlanetShortName();
            Map<String, Map<String, Integer>> bodyMap = grouping.computeIfAbsent(body, k -> new LinkedHashMap<>());
            Map<String, Integer> hotspots = bodyMap.computeIfAbsent(ringName, k -> new LinkedHashMap<>());

            List<SAASignalsFoundEvent.Signal> saa = location.getSaaSignals();
            if (saa != null) {
                for (SAASignalsFoundEvent.Signal s : saa) {
                    String type = StringUtls.humanizeBindingName(s.getType());
                    if (!type.isBlank()) hotspots.merge(type, s.getCount(), Integer::sum);
                }
            }

            List<FSSBodySignalsEvent.Signal> fss = location.getFssSignals();
            if (fss != null) {
                for (FSSBodySignalsEvent.Signal s : fss) {
                    String type = s.getTypeLocalised();
                    if (type != null && !type.isBlank()) hotspots.merge(type, s.getCount(), Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Map<String, Map<String, Integer>>> bodyEntry : grouping.entrySet()) {
            String body = bodyEntry.getKey();
            for (Map.Entry<String, Map<String, Integer>> entry : bodyEntry.getValue().entrySet()) {
                Map<String, Integer> signals = entry.getValue();
                if (signals.isEmpty()) continue;
                String ringName = body.contains("Ring") ? body : null;
                String planetName = body.contains("Ring") ? null : body;
                result.add(new SignalEntry(planetName, entry.getKey(), ringName, signals));
            }
        }
        return result;
    }

    private List<ToYamlConvertable> toDiscoveredSignals(Collection<LocationDto> locations) {
        List<ToYamlConvertable> named = new ArrayList<>();
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Set<String> gameCodeSeen = new LinkedHashSet<>();

        for (LocationDto location : locations) {
            for (FssSignalDto signal : location.getDetectedSignals()) {
                String type = signal.getSignalType();
                String rawName = signal.getSignalName();
                String localised = signal.getSignalNameLocalised();
                String displayName = (localised != null && !localised.isBlank()) ? localised : rawName;

                boolean isCarrier = "FleetCarrier".equalsIgnoreCase(type) || "SquadronCarrier".equalsIgnoreCase(type);
                boolean isGameCode = rawName != null && rawName.startsWith("$");

                if (isCarrier) {
                    typeCounts.merge(type, 1, Integer::sum);
                } else if (isGameCode) {
                    if (displayName != null && !displayName.isBlank()) gameCodeSeen.add(displayName);
                } else if (displayName != null && !displayName.isBlank()) {
                    named.add(new DiscoveredSignal(location.getPlanetName(), displayName, type));
                }
            }
        }

        gameCodeSeen.forEach(label -> named.add(new DiscoveredSignal(null, label, label)));
        typeCounts.forEach((label, count) -> named.add(new DiscoveredSignal(null, count + " " + label, label)));
        return named;
    }

    private record SystemInfoDto(
            String starSystem,
            String allegiance,
            String government,
            String security,
            String economy,
            long population,
            String controllingPower
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private record StationEntry(
            String name,
            String type,
            String controllingFaction,
            String economy
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private record DiscoveredSignal(
            String locationName,
            String signalName,
            String signalType
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private record SignalEntry(
            String bodyName,
            String signalType,
            String ringName,
            Map<String, Integer> hotspotsAndSignals
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private record DataDto(
            SystemInfoDto systemInfo,
            List<ToYamlConvertable> stations,
            List<ToYamlConvertable> stellarObjectSignals,
            List<ToYamlConvertable> discoveredSignals
    ) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
