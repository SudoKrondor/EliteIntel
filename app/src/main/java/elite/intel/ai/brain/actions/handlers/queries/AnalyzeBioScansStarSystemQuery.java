package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.ExoBio;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterQuery
public class AnalyzeBioScansStarSystemQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_bio_scans_and_samples_in_star_system";

    @Override
    public String llmDescription() {
        return "Report scanning progress for biological signals across the current star system: which bodies carry "
                + "bio signals and which still need scanning, including bodies the Exo-Mastery catalogue lists that "
                + "have not been scanned here yet. This is an inventory of what has and has not been "
                + "scanned, never an analysis or prediction of the biome itself.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final ExoMasteryManager exoMastery = ExoMasteryManager.getInstance();

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing bio data for star system."));
        List<BioSampleDto> allCompletedBioScans = playerSession.getBioCompletedSamples();
        SystemBioScans systemBioScans = scanStateForCurrentSystem();

        String instructions = """
                Answer only what the user is asking. Do not read back the entire data set.

                Data fields:
                - conclusion: the already-decided answer to "how many bodies have bio signals, which are done, what is left to scan". It is authoritative.
                - planetsRequireBioScans: planets with organics still to scan (planet name + remaining count). A planet whose organics are all sampled is absent.
                - planetsListedByExoMastery: planets the Exo-Mastery catalogue says still carry unsampled organics, which have not been scanned here yet, so no count is known. These ARE outstanding work.
                - planetsUnmapped: planets where organics were sampled but the body was never surface-mapped, so no bio signal count exists and the true total there is unknown. These are NOT outstanding scans.
                - partialSamples: individual genus samples that are in progress (1 or 2 of 3 taken) with how many scans still needed

                Rules:
                - If asked what is left to scan: restate conclusion in your own voice. Do not contradict it, do not add planets to it, do not turn it into a to-do.
                - Use the other fields only for detail the commander explicitly asks for (which planet, how many, partial scans).
                - Never invent a planet or a count. Be concise: names and counts only.
                """;

        DataDto data = new DataDto(systemBioScans.conclusion(), systemBioScans.planetsRequireBioScans(),
                systemBioScans.planetsListedByExoMastery(), systemBioScans.planetsUnmapped(),
                toBioSameplDataList(allCompletedBioScans));
        return process(new AiDataStruct(instructions, data), originalUserInput);
    }

    private List<BioSampleData> toBioSameplDataList(List<BioSampleDto> allCompletedBioScans) {
        LinkedList<BioSampleData> result = new LinkedList<>();
        for (BioSampleDto data : allCompletedBioScans) {
            Integer numScans = data.getScanXof3();
            if (numScans != null && numScans < 3) {
                result.add(new BioSampleData(data.getPlanetShortName(), data.getGenus(), data.getSpecies(), 3 - numScans));
            }
        }
        return result;
    }

    /**
     * Splits the bodies of the current system into what the commander can be told honestly: those with organics
     * provably left to scan, those the Exo-Mastery catalogue lists as unsampled where no scan of our own says
     * otherwise, and those sampled on a body that was never surface-mapped - where the total is unknowable, not
     * zero. A body that is fully sampled against a known signal count appears in none of them.
     */
    private SystemBioScans scanStateForCurrentSystem() {
        long systemAddress = playerSession.currentSystemAddress();
        List<PlanetsToScan> requireScans = new ArrayList<>();
        Map<Long, UnmappedPlanet> unmapped = new LinkedHashMap<>();
        List<String> surveyed = new ArrayList<>();
        // Bodies our own scans have reached a verdict on; the catalogue is consulted only for the rest.
        Set<Long> decided = new HashSet<>();
        Collection<LocationDto> locations = locationManager.findAllBySystemAddress(systemAddress);

        for (LocationDto location : locations) {
            // A body recorded as sampled out is finished for good, and the sample count below cannot
            // show that on its own: selling organic data clears the samples it counts, after which a
            // finished body reads as untouched and comes back as outstanding work.
            if (location.isBioScansCompleted()) {
                surveyed.add(location.getPlanetShortName());
                decided.add(location.getBodyId());
                continue;
            }
            int detected = bioSignalsDetected(location);
            int completed = getCompletedSamples(location.getPlanetName());
            if (detected > 0) {
                int remaining = remainingOrganics(detected, completed);
                if (remaining > 0) {
                    requireScans.add(new PlanetsToScan(location.getPlanetShortName(), remaining));
                } else {
                    surveyed.add(location.getPlanetShortName());
                }
                decided.add(location.getBodyId());
            } else if (completed > 0) {
                unmapped.put(location.getBodyId(), new UnmappedPlanet(location.getPlanetShortName(), completed));
            }
        }

        List<Body> openInCatalogue = exoMastery.isEnabled() ? exoMastery.remainingBodiesIn(systemAddress) : List.of();
        List<Body> listed = cataloguedWithoutVerdict(openInCatalogue, decided);
        // An unmapped body the catalogue still lists open is not "maybe more": the catalogue says there is more.
        listed.forEach(body -> unmapped.remove(body.bodyId()));
        List<String> listedNames = listed.stream().map(ExoMasteryManager::shortBodyName).toList();
        List<UnmappedPlanet> unmappedList = List.copyOf(unmapped.values());

        return new SystemBioScans(conclusion(surveyed, requireScans, listedNames, unmappedList),
                requireScans, listedNames, unmappedList);
    }

    /**
     * The catalogue's open bodies that our own scans say nothing about. A commander routed here by Exo-Mastery
     * usually skips the FSS, so the catalogue is the only record of what the system holds; where we did scan a
     * body, what we measured outranks it.
     */
    static List<Body> cataloguedWithoutVerdict(List<Body> openInCatalogue, Set<Long> decidedBodyIds) {
        return openInCatalogue.stream().filter(body -> !decidedBodyIds.contains(body.bodyId())).toList();
    }

    /**
     * The answer, decided here rather than left to the model: a small local LLM handed three lists will read any
     * non-empty one as outstanding work (it once announced an unmapped body as "we still need to check out two alpha").
     * Outstanding scans lead; an unmapped body is reported as a caveat to a completed survey, never as a to-do.
     */
    static String conclusion(List<String> surveyed, List<PlanetsToScan> requireScans, List<String> listedByExoMastery,
                             List<UnmappedPlanet> unmapped) {
        StringBuilder sb = new StringBuilder();
        // The whole picture first - how many bodies carry biology and which are done - because "how many
        // moons have bio signals" is the question asked most, and a list of what is left does not answer it.
        int withBiology = surveyed.size() + requireScans.size() + listedByExoMastery.size();
        if (withBiology > 0) {
            sb.append(withBiology).append(withBiology == 1 ? " body carries" : " bodies carry").append(" bio signals");
            if (!surveyed.isEmpty()) {
                sb.append("; fully surveyed: ").append(String.join(", ", surveyed));
            }
            sb.append(". ");
        }
        if (requireScans.isEmpty() && listedByExoMastery.isEmpty()) {
            sb.append("Nothing left to scan in this system: every detected bio signal has been sampled.");
        }
        if (!requireScans.isEmpty()) {
            sb.append("Still to scan: ");
            for (int i = 0; i < requireScans.size(); i++) {
                PlanetsToScan planet = requireScans.get(i);
                sb.append(i > 0 ? ", " : "").append(planet.planetName()).append(" (").append(planet.remainingOrganicsToScan()).append(")");
            }
            sb.append(".");
        }
        if (!listedByExoMastery.isEmpty()) {
            sb.append(requireScans.isEmpty() ? "" : " ")
                    .append("The Exo-Mastery catalogue lists organics still to sample on ")
                    .append(String.join(", ", listedByExoMastery))
                    .append(", not scanned here yet.");
        }
        if (!unmapped.isEmpty()) {
            sb.append(" Caveat: ");
            for (int i = 0; i < unmapped.size(); i++) {
                UnmappedPlanet planet = unmapped.get(i);
                sb.append(i > 0 ? ", " : "").append(planet.planetName()).append(" (").append(planet.samplesTaken()).append(" sampled)");
            }
            sb.append(unmapped.size() == 1 ? " was never surface-mapped" : " were never surface-mapped");
            sb.append(", so there is no signal count for it and more organics cannot be ruled out. Mention this only as a caveat, not as a pending scan.");
        }
        return sb.toString();
    }

    /**
     * Bio signals known for this body. Shared with the command that records a survey as already done by
     * hand, which needs the same answer to decide whether a body is one biology applies to at all.
     */
    static int bioSignalsDetected(LocationDto location) {
        return ExoBio.bioSignalsDetected(location);
    }

    /**
     * Organics still to scan on a body: never negative. Completed samples can exceed the detected signal count when the
     * signal count was never recorded (0 means "unknown", not "none"), and a negative remainder rendered into the prompt
     * reads to the model as work left to do.
     */
    static int remainingOrganics(int bioSignalsDetected, int completedSamples) {
        return Math.max(0, bioSignalsDetected - completedSamples);
    }

    private int getCompletedSamples(String planetName) {
        List<BioSampleDto> completedSamples = playerSession.getBioCompletedSamples();
        int result = 0;
        for (BioSampleDto sample : completedSamples) {
            if (sample.getPlanetName().equalsIgnoreCase(planetName)) {
                result++;
            }
        }
        return result;
    }

    record DataDto(String conclusion,
                   List<PlanetsToScan> planetsRequireBioScans,
                   List<String> planetsListedByExoMastery,
                   List<UnmappedPlanet> planetsUnmapped,
                   List<BioSampleData> partialSamples) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    /**
     * The decided answer plus its supporting detail; see {@link #scanStateForCurrentSystem()}.
     */
    record SystemBioScans(String conclusion,
                          List<PlanetsToScan> planetsRequireBioScans,
                          List<String> planetsListedByExoMastery,
                          List<UnmappedPlanet> planetsUnmapped) {
    }

    /**
     * A body sampled without ever being surface-mapped: no signal count exists, so the total there is unknown.
     */
    record UnmappedPlanet(String planetName, int samplesTaken) implements ToYamlConvertable {
        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record BioSampleData(String planetShortName, String genus, String species, Integer samplesRequired) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record PlanetsToScan(String planetName, int remainingOrganicsToScan) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
