package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.LocationDto.LocationType;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterQuery
public class AnalyzeStellarObjectsQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_stellar_objects";

    @Override public String llmDescription() { return "Report the stellar bodies in the current system."; }


    @Override public String id() { return ID; }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static final Map<String, String> NATO = Map.ofEntries(
            Map.entry("A", "Alpha"), Map.entry("B", "Bravo"), Map.entry("C", "Charlie"),
            Map.entry("D", "Delta"), Map.entry("E", "Echo"), Map.entry("F", "Foxtrot"),
            Map.entry("G", "Golf"), Map.entry("H", "Hotel"), Map.entry("I", "India"),
            Map.entry("J", "Juliet"), Map.entry("K", "Kilo"), Map.entry("L", "Lima"),
            Map.entry("M", "Mike"), Map.entry("N", "November"), Map.entry("O", "Oscar"),
            Map.entry("P", "Papa"), Map.entry("Q", "Quebec"), Map.entry("R", "Romeo"),
            Map.entry("S", "Sierra"), Map.entry("T", "Tango"), Map.entry("U", "Uniform"),
            Map.entry("V", "Victor"), Map.entry("W", "Whiskey"), Map.entry("X", "X-ray"),
            Map.entry("Y", "Yankee"), Map.entry("Z", "Zulu")
    );

    // "AB 1 B" → "Alpha Bravo 1 Bravo",  "AC 3b" → "Alpha Charlie 3 Bravo"
    static String toPhonetic(String name) {
        if (name == null || name.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String token : name.trim().split("\\s+")) {
            if (!sb.isEmpty()) sb.append(' ');
            Matcher m = Pattern.compile("[A-Za-z]+|\\d+").matcher(token);
            boolean firstSeg = true;
            while (m.find()) {
                String seg = m.group();
                if (!firstSeg) sb.append(' ');
                firstSeg = false;
                if (Character.isDigit(seg.charAt(0))) {
                    sb.append(seg);
                } else {
                    boolean firstChar = true;
                    for (char c : seg.toUpperCase().toCharArray()) {
                        if (!firstChar) sb.append(' ');
                        firstChar = false;
                        sb.append(NATO.getOrDefault(String.valueOf(c), String.valueOf(c)));
                    }
                }
            }
        }
        return sb.toString();
    }

    // Spoken/STT forms that collapse to a body-name token: NATO letters -> letter, number words ->
    // digit, plus common STT confusions ("for" -> "4"). Used to normalise both the commander's
    // utterance and each body's short name to the same compact form for matching.
    private static final Map<String, String> SPOKEN_TO_TOKEN = Map.ofEntries(
            Map.entry("alpha", "a"), Map.entry("bravo", "b"), Map.entry("charlie", "c"),
            Map.entry("charly", "c"), Map.entry("delta", "d"), Map.entry("echo", "e"),
            Map.entry("foxtrot", "f"), Map.entry("golf", "g"), Map.entry("hotel", "h"),
            Map.entry("india", "i"), Map.entry("juliet", "j"), Map.entry("juliett", "j"),
            Map.entry("kilo", "k"), Map.entry("lima", "l"), Map.entry("mike", "m"),
            Map.entry("november", "n"), Map.entry("oscar", "o"), Map.entry("papa", "p"),
            Map.entry("quebec", "q"), Map.entry("romeo", "r"), Map.entry("sierra", "s"),
            Map.entry("tango", "t"), Map.entry("uniform", "u"), Map.entry("victor", "v"),
            Map.entry("whiskey", "w"), Map.entry("xray", "x"), Map.entry("yankee", "y"),
            Map.entry("zulu", "z"),
            Map.entry("zero", "0"), Map.entry("one", "1"), Map.entry("two", "2"),
            Map.entry("three", "3"), Map.entry("four", "4"), Map.entry("for", "4"),
            Map.entry("five", "5"), Map.entry("six", "6"), Map.entry("seven", "7"),
            Map.entry("eight", "8"), Map.entry("nine", "9"), Map.entry("ten", "10"),
            Map.entry("eleven", "11"), Map.entry("twelve", "12")
    );

    /**
     * Break a phrase into atomic body-name tokens - single letters and number strings - preserving word
     * boundaries. "B 1 a" -> [b,1,a]; "is planet b one landable" -> [b,1]; "b1" -> [b,1]. A pure-letter
     * word is only kept when it is a body designator: a NATO word ("bravo"->b) or a lone letter. Plain
     * multi-letter English words ("and", "landable") are dropped so they cannot contribute stray letters
     * to a match. Glued alphanumerics ("b1", "3b", "ab1b") are split into their letter/digit runs.
     */
    static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) return out;
        for (String word : s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.isEmpty()) continue;
            String mapped = SPOKEN_TO_TOKEN.get(word);
            if (mapped != null) {                       // NATO letter or number word
                out.add(mapped);
                continue;
            }
            boolean hasDigit = word.chars().anyMatch(Character::isDigit);
            if (!hasDigit) {                            // pure letters, not NATO
                if (word.length() == 1) out.add(word);  // a lone letter can be a body designator
                continue;                               // multi-letter English word -> noise, drop it
            }
            // pure digits or a glued letter+digit body token: split into letter/digit runs
            Matcher m = Pattern.compile("[a-z]+|[0-9]+").matcher(word);
            while (m.find()) {
                String seg = m.group();
                if (Character.isDigit(seg.charAt(0))) {
                    out.add(seg);
                } else {
                    for (char c : seg.toCharArray()) out.add(String.valueOf(c));
                }
            }
        }
        return out;
    }

    private static boolean containsSubsequence(List<String> haystack, List<String> needle) {
        if (needle.isEmpty() || needle.size() > haystack.size()) return false;
        outer:
        for (int i = 0; i <= haystack.size() - needle.size(); i++) {
            for (int j = 0; j < needle.size(); j++) {
                if (!haystack.get(i + j).equals(needle.get(j))) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * Deterministically resolve the single body the commander named, so a small local model is never
     * asked to find one boolean in a 30-body dump (and never falls back to the whole-system aggregate).
     * Both the utterance and each body's short name are tokenised the same way; a body matches when its
     * token sequence appears contiguously in the utterance's tokens. Only bodies with a digit token are
     * eligible so bare stars A/B/C/D can't match stray letters. Longest token match wins; a tie at the
     * winning length is ambiguous (more than one body named) and returns null so the LLM path handles it.
     */
    static LocationData resolveNamedBody(String userInput, List<LocationData> bodies) {
        List<String> input = tokenize(userInput);
        if (input.isEmpty()) return null;
        LocationData best = null;
        int bestLen = 0;
        boolean tie = false;
        for (LocationData b : bodies) {
            String name = b.stellarObjectName();
            if (name == null || name.isBlank()) continue;
            List<String> key = tokenize(name);
            if (key.stream().noneMatch(t -> Character.isDigit(t.charAt(0)))) continue;
            if (!containsSubsequence(input, key)) continue;
            if (key.size() > bestLen) {
                best = b;
                bestLen = key.size();
                tie = false;
            } else if (key.size() == bestLen && !name.equals(best.stellarObjectName())) {
                tie = true;
            }
        }
        return tie ? null : best;
    }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        //GameEventBus.publish(new AiVoxResponseEvent("Analyzing stelar objects data. Stand by."));

        StellarObjectsData<List<LocationData>, String> data = toLocationList(locationManager.findAllBySystemAddress(playerSession.getLocationData().getSystemAddress()));

        // If the commander named one specific body, resolve it here and hand the model ONLY that body
        // with no whole-system aggregate. Drops the prompt from ~5700 tokens to a few hundred and removes
        // any generic count a weak model could answer with instead of the body it was actually asked about.
        LocationData focus = resolveNamedBody(originalUserInput, data.getObjectList());
        String summaryField = focus != null ? null : data.getSummary();
        List<LocationData> listField = focus != null ? List.of(focus) : data.getObjectList();
        String focusInstruction = focus == null ? "" :
                "The commander asked specifically about the body \"" + focus.stellarObjectName() + "\". "
                + "It is the ONLY entry in detailedStellarObjectList. Answer strictly from its fields; "
                + "for \"is it landable\" answer a plain yes or no from isLandable. Do not mention other bodies or any counts.\n\n";

        String instructions = focusInstruction + """
                Answer ONLY the specific question asked. Do not give an overview or summary unless the user explicitly asks for one.
                The input comes via STT, do not expect exact matches. 'AB 1 B' could be 'ab-1-b'. User may employ NATO alphabet: 'Alpha 2' means 'A 2', 'Alpha Charlie 3 Bravo' means 'AC 3b'. STT can confuse '4' with 'for'.
                Data fields:
                - summary: pre-computed AGGREGATE counts for the WHOLE system (stars, planets, moons, stations, landable, bio signals, scoopable stars). Use ONLY for whole-system "how many / are there any" questions, NEVER to answer about a single named body.
                - detailedStellarObjectList: full list of stellar objects with per-object data. This is the authoritative source for any question about a specific body:
                  - stellarObjectName: short canonical name (e.g. "AB 1 B")
                  - stellarObjectPhonetic: NATO phonetic expansion (e.g. "Alpha Bravo 1 Bravo") - match STT input against this field; accept partial/variant NATO words (e.g. "Charly"/"Charlie")
                  - objectClass: STAR, PLANET, MOON, STATION
                  - objectType: specific type (e.g. Rocky Body, High metal content world, Neutron Star)
                  - starClass: star spectral class (M, K, G, F, A, B, O are fuel-scoopable)
                  - isLandable: whether the surface can be landed on
                  - isTerraformable: terraforming candidate
                  - gravity: surface gravity (zero means no data)
                  - surfaceTemperature: in Celsius
                  - atmosphere: atmosphere type or None
                  - parentPlanetName: parent body if this is a moon
                  - distanceFromStar: distance from primary star in light seconds
                  - bioSignals: number of biological signals detected
                  - ourDiscovery: true if we were first to discover this body
                  - weMappedIt: true if we mapped this body
                  - hasMarkets: true if this location has a market

                Rules:
                - FIRST decide whether the user named a SPECIFIC body (e.g. "B 1", "Alpha 1", "is that moon landable").
                  Bodies are always referred to by SHORT name ("B 1"), never the full system-prefixed name.
                  - If a specific body IS named: this is NOT a count/summary question. Find that one body in
                    detailedStellarObjectList (match stellarObjectPhonetic first, then stellarObjectName; accept
                    fuzzy/variant/partial STT matches, e.g. "Charly"/"Charlie", "for"/"4") and answer ONLY about it.
                    For "is X landable" give a plain yes or no read directly from that body's isLandable field.
                    NEVER answer a specific-body question with an aggregate count from summary. If you cannot find the
                    named body in the list, say so and ask the commander to repeat it - do not fall back to counts.
                  - If NO specific body is named (e.g. "how many landable planets", "are there any bio signals"):
                    use summary directly. Do not recount from the list.
                - IF data shows 0 planets, 0 moons, 0 stations etc., it means you do not have enough data to answer this question and scans may be required.
                - Do not invent data not present in the provided fields.
                """;

        return process(
                new AiDataStruct(
                        instructions,
                        new DataDto(
                                summaryField,
                                listField
                        )
                ),
                originalUserInput
        );
    }

    private StellarObjectsData<List<LocationData>, String> toLocationList(Collection<LocationDto> locations) {

        ArrayList<LocationData> result = new ArrayList<>();
        int numberOfMoons = 0;
        int numberOfPlanets = 0;
        int numberOfStars = 0;
        int numberOfStations = 0;
        for (LocationDto location : locations) {
            boolean isPlanetaryRing = location.getPlanetName().contains("Ring");
            LocationType locationType = location.getLocationType();

            if (LocationType.STAR == locationType || LocationType.PRIMARY_STAR == locationType) {
                numberOfStars++;
            }

            if (LocationType.PLANET == locationType){
                numberOfPlanets++;
            }

            if(LocationType.MOON == locationType){
                numberOfMoons++;
            }

            if(LocationType.STATION == locationType){
                numberOfStations++;
            }

            String shortName = location.getPlanetShortName();
            result.add(new LocationData(
                    shortName,
                    toPhonetic(shortName),
                    "UNKNOWN".equals(locationType.name()) ? "" : locationType.name(),
                    isPlanetaryRing ? "Planetary Ring" : location.getPlanetClass(),
                    location.getStarClass(),
                    location.getStarName(),
                    location.isLandable(),
                    location.isTerraformable(),
                    Math.round(location.getGravity()),
                    Math.round(( location.getSurfaceTemperature() - 273 ) ), // Convert Kelvin to Celsius
                    location.getAtmosphere(),
                    location.getParentBodyName(),
                    Math.round(location.getDistance()),
                    location.getBioSignals(),
                    location.isOurDiscovery(),
                    location.isWeMappedIt(),
                    location.getMarket() != null
            ));
        }

        long landableMoons = result.stream().filter(l -> "MOON".equals(l.objectClass()) && l.isLandable()).count();
        long landablePlanets = result.stream().filter(l -> "PLANET".equals(l.objectClass()) && l.isLandable()).count();
        long bioMoons = result.stream().filter(l -> "MOON".equals(l.objectClass()) && l.bioSignals() > 0).count();
        long bioPlanets = result.stream().filter(l -> "PLANET".equals(l.objectClass()) && l.bioSignals() > 0).count();
        long atmosMoons = result.stream().filter(l -> "MOON".equals(l.objectClass()) && hasAtmosphere(l)).count();
        long fuelStars = result.stream().filter(l -> isScoopable(l)).count();

        String summary = """
                Star System contains: %d stars, %d planets, %d moons, %d stations.
                WHOLE-SYSTEM AGGREGATE COUNTS (use ONLY for "how many / are there any" questions about the whole system; NEVER to answer whether one specific named body is landable/etc - use detailedStellarObjectList for that):
                Landable moons: %d
                Landable planets: %d
                Moons with bio signals: %d
                Planets with bio signals: %d
                Moons with atmosphere: %d
                Scoopable fuel stars: %d
                """.formatted(numberOfStars, numberOfPlanets, numberOfMoons, numberOfStations,
                landableMoons, landablePlanets, bioMoons, bioPlanets, atmosMoons, fuelStars);

        return new StellarObjectsData<>(result, summary);
    }

    record LocationData(String stellarObjectName,
                        String stellarObjectPhonetic,
                        String objectClass,
                        String objectType,
                        String starClass,
                        String starName,
                        boolean isLandable,
                        boolean isTerraformable,
                        double gravity,
                        double surfaceTemperature,
                        String atmosphere,
                        String parentPlanetName,
                        double distanceFromStar,
                        int bioSignals,
                        boolean ourDiscovery,
                        boolean weMappedIt,
                        boolean hasMarkets
    ) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    record DataDto(String summary, List<LocationData> detailedStellarObjectList) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }

    private static class StellarObjectsData<A, B> {
        private final A locationData;
        private final B summary;

        public StellarObjectsData(A list, B string) {
            locationData = list;
            summary = string;
        }

        public A getObjectList() {
            return locationData;
        }

        public B getSummary() {
            return summary;
        }
    }


    private static boolean hasAtmosphere(LocationData l) {
        String atm = l.atmosphere();
        return atm != null && !atm.isBlank() && !"None".equalsIgnoreCase(atm);
    }

    private static boolean isScoopable(LocationData l) {
        String sc = l.starClass();
        return sc != null && List.of("M", "K", "G", "F", "A", "B", "O").contains(sc.trim().toUpperCase());
    }
}