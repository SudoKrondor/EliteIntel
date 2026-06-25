package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * The EnglishInputNormalizerRules class provides synonym substitution rules and noise word patterns
 * for normalizing user input in English. It serves as a language-specific implementation of the
 * InputNormalizerProvider interface.
 *
 * The primary purpose of this class is to preprocess and normalize English input text by replacing
 * synonyms with their canonical forms and stripping noise words where applicable. The transformations
 * are applied using ordered mappings that ensure specific phrases are substituted before shorter phrases
 * that are substrings of them.
 *
 * Methods in this class define specific synonym mappings for various lexical and contextual categories,
 * such as HUD modes, navigation commands, hyperspace actions, and more. The implementation also allows
 * for phonetic-based substitutions to correct specific acoustic confusions that other modules cannot resolve.
 *
 * Superclasses:
 * - java.lang.Object
 * - elite.intel.ai.brain.i18n.InputNormalizerProvider
 *
 * Methods:
 *
 * buildSynonymMap:
 * - Builds the ordered synonym map that matches input phrases to their canonical forms. This map is used
 *   to normalize text inputs by replacing synonyms based on the specified rules.
 * - Returns a LinkedHashMap of synonyms and their transformations.
 *
 * noiseWordPattern:
 * - Returns a regex pattern for identifying and stripping noise words from normalized input after synonym
 *   substitutions. Returns null if no noise word removal is required.
 *
 * loadHudModes:
 * - Defines synonym mappings related to HUD mode commands, such as switching between combat and analysis modes.
 * - Maps phrases to their normalized forms for consistency in command recognition.
 *
 * loadNavigation:
 * - Defines synonym mappings for navigation-related commands such as finding locations, setting routes,
 *   and cancelling navigation.
 * - Includes mappings for various verb structures and synonyms.
 *
 * loadHyperspace:
 * - Handles synonym mappings specific to hyperspace-related actions, enabling consistent interpretation of
 *   commands for transitioning between systems.
 *
 * loadLandingAndBubble:
 * - Provides synonym substitutions for commands related to landing, station interactions, and local operations
 *   ('bubble' refers to a localized play area in the game).
 *
 * loadCombatAndVehicles:
 * - Maps synonyms for combat-related commands and vehicle operations. Includes substitutions for targeting,
 *   threat prioritization, and vehicle deployment.
 *
 * loadShipSystems:
 * - Normalizes commands relevant to ship system management, such as power distribution and system activation.
 *
 * loadRouteAndShipQueries:
 * - Covers synonym mappings for route-related queries and ship-related commands directed at navigation or
 *   utility explanations.
 *
 * loadExploration:
 * - Focuses on exploration-related commands, such as planetary data retrieval, resource searching, and
 *   general exploratory actions.
 *
 * loadCarrierAndTrade:
 * - Defines mappings for carrier commands and trading-related actions, assisting in economic and fleet
 *   management scenarios.
 *
 * loadGameWorldQueries:
 * - Normalizes input phrases used to inquire about world state, missions, and environmental interactions.
 *
 * loadPhonetics:
 * - Provides phonetic-based substitution mappings for acoustic confusions that are outside the scope of
 *   existing correction mechanisms. Designed to handle phonetic output discrepancies from speech-to-text systems.
 */
public class EnglishInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        loadHudModes(m);
        loadNavigation(m);
        loadHyperspace(m);           // self-contained critical ordering block
        loadLandingAndBubble(m);
        loadCombatAndVehicles(m);    // retract/raise verbs must precede loadShipSystems
        loadShipSystems(m);
        loadRouteAndShipQueries(m);
        loadExploration(m);
        loadCarrierAndTrade(m);
        loadGameWorldQueries(m);
        loadPhonetics(m);
        return m;
    }

    @Override
    public String noiseWordPattern() {
        return "(?i)\\bwhere\\b";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD modes / priority target
    // Multi-word "activate X mode" entries must precede single-word "activate"
    // in loadShipSystems().
    // ─────────────────────────────────────────────────────────────────────────

    private void loadHudModes(LinkedHashMap<String, String> m) {
        m.put("activate combat mode", "switch to combat mode");
        m.put("activate analysis mode", "switch to analysis mode");
        m.put("hud combat mode", "switch to combat mode");
        m.put("hud analysis mode", "switch to analysis mode");
        m.put("combat mode", "switch to combat mode");
        m.put("analysis mode", "switch to analysis mode");
        m.put("enter combat mode", "switch to combat mode");
        m.put("combat hud", "switch to combat mode");
        m.put("analysis hud", "switch to analysis mode");
        m.put("explorer mode", "switch to analysis mode");
        m.put("next enemy", "priority target");
        m.put("select enemy", "priority target");
        m.put("high threat", "priority target");
        m.put("highest threat", "priority target");
        m.put("priority target", "priority target");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation - find, go, cancel, target, dismiss, calculate
    // ─────────────────────────────────────────────────────────────────────────

    private void loadNavigation(LinkedHashMap<String, String> m) {
        // find / locate / search
        m.put("search for", "find");
        m.put("search", "find");
        m.put("locate", "find");
        m.put("look for", "find");
        m.put("where is", "find");
        m.put("where can i", "find");
        m.put("where can we", "find");
        m.put("find me", "find");
        m.put("location of", "find");
        m.put("brain tree", "find brain trees");
        m.put("brain trees", "find brain trees");

        // navigate / go / head / travel
        m.put("head over to", "navigate to");
        m.put("head to", "navigate to");
        m.put("go to next trade stop", "navigate to next trade stop");
        m.put("go to", "navigate to");
        m.put("take us to", "navigate to");
        m.put("take me to", "navigate to");
        m.put("get me to", "navigate to");
        m.put("get us to", "navigate to");
        m.put("travel to", "navigate to");
        m.put("route to", "navigate to");
        m.put("set course to", "navigate to");
        m.put("set course for", "navigate to");
        m.put("guide me to", "navigate to");
        m.put("guide us to", "navigate to");
        m.put("plot course to", "navigate to");
        m.put("plot a course", "navigate");
        m.put("fly to", "navigate to");
        m.put("set destination", "navigate to");
        m.put("plot route to", "navigate to");
        m.put("select destination", "target destination");

        // calculate / compute
        m.put("compute", "calculate");
        m.put("work out", "calculate");
        m.put("figure out", "calculate");
        m.put("calculate neutron route", "calculate neutron star route");

        // cancel / abort / stop
        m.put("abort", "cancel");
        m.put("clear route", "cancel navigation");
        m.put("stop navigation", "cancel navigation");
        m.put("plot route", "navigate to");

        // target
        m.put("aim at", "target");
        m.put("focus on", "target");
        m.put("lock on", "target");
        m.put("lock onto", "target");
        m.put("acquire target", "target");
        m.put("select target", "target");

        // dismiss ship
        m.put("go play", "dismiss ship");
        m.put("dismiss ship", "dismiss ship");
        m.put("dismissed", "dismiss ship");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperspace / FTL / supercruise
    //
    // ORDERING WITHIN THIS METHOD IS CRITICAL:
    //   "how many jumps on carrier route" → "how many jumps" → single "jump"
    //   "drop ftl" → "ftl"
    // Do not reorder entries within this method.
    // ─────────────────────────────────────────────────────────────────────────

    private void loadHyperspace(LinkedHashMap<String, String> m) {
        // Multi-word jump expressions - must precede single "jump" below
        m.put("get out of here", "jump to hyperspace");
        m.put("next star system", "jump to hyperspace");
        m.put("let us go", "jump to hyperspace");
        m.put("let's get moving", "jump to hyperspace");
        m.put("proceed to", "jump to hyperspace");
        m.put("next way point", "jump to hyperspace");
        m.put("punch it", "jump to hyperspace");
        m.put("engage fsd", "jump to hyperspace");
        m.put("engage hyperspace", "jump to hyperspace");

        // FSD query - must precede "jump"
        m.put("info on next jump", "FSD target");

        // Carrier route jump counts - most specific first
        m.put("how many jumps on the carrier route", "carrier route");
        m.put("how many jump on the carrier route", "carrier route");
        m.put("how many jumps on carrier", "carrier route");
        m.put("how many jump on carrier", "carrier route");
        m.put("jumps left on carrier", "carrier route");
        m.put("jumps remaining on carrier", "carrier route");

        // Ship route jump counts - more specific before "how many jumps"
        m.put("how many jumps to destination", "plotted route");
        m.put("how many jump to destination", "plotted route");
        // "jumps remaining/left/to destination" must precede single "jump" to prevent substring corruption
        // ("jump" would match inside "jumps remaining" and produce "jump to hyperspaces remaining")
        m.put("jumps remaining", "plotted route");
        m.put("jumps left", "plotted route");
        m.put("jumps to destination", "plotted route");
        m.put("how many jumps", "plotted route");

        // Single "jump" - must follow all multi-word jump phrases above
        m.put("jump", "jump to hyperspace");
        m.put("hyperspace jump", "jump to hyperspace");
        m.put("make the jump", "jump to hyperspace");
        m.put("let's jump", "jump to hyperspace");

        m.put("engage drive", "jump to hyperspace");
        m.put("get us out", "jump to hyperspace");
        m.put("let's bounce", "jump to hyperspace");

        // FTL / supercruise - normalize to "supercruise" (not "enter supercruise") so the
        // word "enter" does not pull JUMP_TO_HYPERSPACE into the Reducer set via "enter hyperspace".
        m.put("drop ftl", "drop from supercruise");   // must precede "ftl" below
        m.put("light speed", "supercruise");
        m.put("lightspeed", "supercruise");
        m.put("ftl", "supercruise");
        m.put("f t l", "supercruise");
        m.put("super cruise", "supercruise");
        m.put("leave supercruise", "drop out");
        m.put("drop here", "drop out");
        m.put("disengage", "drop out");
        m.put("disengage supercruise", "drop out");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Landing gear / docking clearance / distance to bubble
    // ─────────────────────────────────────────────────────────────────────────

    private void loadLandingAndBubble(LinkedHashMap<String, String> m) {
        m.put("auto docking", "auto land");
        m.put("auto dock", "auto land");

        m.put("lower landing gear", "gear down");
        m.put("extend landing gear", "gear down");

        // Docking / landing clearance
        // "request landing clearance" before "landing clearance" before "request landing"
        m.put("request landing clearance", "request docking");
        m.put("landing clearance", "request docking");
        m.put("clear me to land", "request docking");
        m.put("permission to land", "request docking");
        m.put("request landing", "request docking");

        // Distance to the bubble - all Sol / Earth / inhabited-space / civilization variants
        m.put("how far am i from civilization", "distance to bubble");
        m.put("how far are we from civilization", "distance to bubble");
        m.put("how far from civilization", "distance to bubble");
        m.put("distance from civilization", "distance to bubble");
        m.put("distance to civilization", "distance to bubble");
        m.put("how far to civilization", "distance to bubble");
        m.put("how far am i from the bubble", "distance to bubble");
        m.put("how far are we from the bubble", "distance to bubble");
        m.put("how far from the bubble", "distance to bubble");
        m.put("how far to the bubble", "distance to bubble");
        m.put("how far am i from inhabited space", "distance to bubble");
        m.put("how far are we from inhabited space", "distance to bubble");
        m.put("how far from inhabited space", "distance to bubble");
        m.put("distance from inhabited space", "distance to bubble");
        m.put("how far from human space", "distance to bubble");
        m.put("how far am i from human space", "distance to bubble");
        m.put("how far are we from human space", "distance to bubble");
        m.put("distance from human space", "distance to bubble");
        m.put("how far am i from the earth", "distance to bubble");
        m.put("how far are we from the earth", "distance to bubble");
        m.put("how far from the earth", "distance to bubble");
        m.put("how far am i from earth", "distance to bubble");
        m.put("how far are we from earth", "distance to bubble");
        m.put("how far from earth", "distance to bubble");
        m.put("how far is earth", "distance to bubble");
        m.put("how far is sol", "distance to bubble");
        m.put("distance to earth", "distance to bubble");
        m.put("distance from earth", "distance to bubble");
        m.put("how far am i from sol", "distance to bubble");
        m.put("how far are we from sol", "distance to bubble");
        m.put("distance to sol", "distance to bubble");
        m.put("distance from sol", "distance to bubble");
        m.put("how far from sol", "distance to bubble");
        m.put("how far to sol", "distance to bubble");
        m.put("how far to earth", "distance to bubble");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Combat - hardpoints, generic deploy/retract verbs, SRV, boarding
    //
    // retract/raise/stow single-word entries must precede "retract cargo scoop"
    // in loadShipSystems() so the raise-cargo-scoop cascade works correctly.
    // ─────────────────────────────────────────────────────────────────────────

    private void loadCombatAndVehicles(LinkedHashMap<String, String> m) {
        // Retract hardpoints - multi-word phrases before single "retract"/"stow" below
        m.put("weapons cold", "retract hardpoints");
        m.put("store weapons", "retract hardpoints");
        m.put("holster weapons", "retract hardpoints");
        m.put("weapons away", "retract hardpoints");
        m.put("retract weapons", "retract hardpoints");
        m.put("stow hardpoints", "retract hardpoints");   // before single "stow" below
        m.put("put weapons away", "retract hardpoints");
        m.put("hardpoints in", "retract hardpoints");

        // Deploy hardpoints
        m.put("weapons hot", "deploy hardpoints");
        m.put("weapons free", "deploy hardpoints");
        m.put("weapons out", "deploy hardpoints");
        m.put("weapons ready", "deploy hardpoints");
        m.put("arm weapons", "deploy hardpoints");
        m.put("deploy hardpoints", "deploy hardpoints");
        m.put("deploy weapons", "deploy hardpoints");
        m.put("hardpoints out", "deploy hardpoints");
        m.put("arm up", "deploy hardpoints");
        m.put("ready weapons", "deploy hardpoints");
        m.put("power cell", "shield cell");

        // Generic deploy verbs
        m.put("extend", "deploy");
        m.put("release", "deploy");
        m.put("lower", "deploy");
        m.put("put out", "deploy");
        m.put("launch", "deploy");

        // SRV
        m.put("launch srv", "deploy srv");
        m.put("deploy vehicle", "deploy srv");

        // Board ship / SRV recovery
        m.put("board ship", "recover SRV");
        m.put("back a board this ship", "recover SRV");
        m.put("requesting extraction", "recover SRV");
        m.put("extract car", "recover SRV");
        m.put("extract vehicle", "recover SRV");
        m.put("recover car", "recover SRV");
        m.put("recover vehicle", "recover SRV");
        m.put("recover buggy", "recover SRV");
        m.put("recover srv", "recover SRV");
        m.put("extract srv", "recover SRV");

        // Retract / raise single-word verbs
        // MUST precede "retract cargo scoop" → "close cargo scoop" in loadShipSystems()
        m.put("raise", "retract");
        m.put("stow", "retract");
        m.put("retract", "retract");
        m.put("pull in", "retract");
        m.put("hide", "retract");
        m.put("recall ship", "return to surface");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ship systems - scan, power, activate, radio, cargo
    // ─────────────────────────────────────────────────────────────────────────

    private void loadShipSystems(LinkedHashMap<String, String> m) {
        // Scanner panel
        m.put("exit scanner", "exit close panel");
        m.put("close scanner", "exit close panel");
        m.put("close the scanner", "exit close panel");

        // System scan — "honk" always maps to the honk action. Any phrase containing "honk" is normalized
        // to "honk" so it bypasses the LLM and hits the alias directly.
        m.put("honk the system", "honk");
        m.put("full scan", "scan the system");
        m.put("fss scan", "scan the system");
        m.put("open fss and scan", "scan the system");
        m.put("open central panel", "open or display commander panel");
        m.put("open role panel", "open or display commander panel");
        m.put("system scan", "scan the system");
        m.put("scan system", "scan the system");
        m.put("run a scan", "scan the system");
        m.put("perform system scan", "scan the system");
        m.put("discovery scan", "scan the system");

        // Carrier balance must be locked in before the bare "balance" → "equalize power" entry below
        m.put("squadron carrier balance", "squadron carrier finances");
        m.put("carrier balance", "carrier finances");

        // Equalize / balance / reset power
        m.put("balance power", "equalize power");
        m.put("reset power", "equalize power");
        m.put("equalize", "equalize power");
        m.put("equalize power", "equalize power");
        m.put("balance", "equalize power");
        m.put("reset systems", "equalize power");
        m.put("even out power", "equalize power");

        // Activate - single-word forms; MUST follow "activate combat mode" in loadHudModes()
        m.put("select", "activate");
        m.put("activate", "activate");
        m.put("activate controls", "activate");

        // Stop listening
        m.put("slip", "slip");
        m.put("stop listening", "ignore me");
        m.put("do not monitor", "ignore me");

        // Radio / announcements / speed
        m.put("radio traffic", "toggle radio");
        m.put("radio traffic on", "toggle radio on");
        m.put("radio traffic off", "toggle radio off");
        m.put("radio transmission", "toggle radio");
        m.put("radio transmission on", "toggle radio on");
        m.put("radio transmission off", "toggle radio off");
        m.put("shut up", "interrupt");
        m.put("display email", "open or display email inbox panel");
        m.put("email", "open or display email inbox panel");
        m.put("inbox", "open or display email inbox panel");
        m.put("optimize", "set optimal speed");

        // Cargo / commodities
        m.put("commodity inventory", "cargo contents");
        m.put("what commodities do we have", "cargo contents");
        m.put("cargo manifest", "cargo hold");
        m.put("what are we hauling", "cargo hold");
        m.put("what's in the hold", "cargo hold");
        m.put("hold contents", "cargo hold");
        m.put("deploy cargo scoop", "cargo scoop");
        m.put("open cargo scoop", "cargo scoop");
        m.put("open cargo bay", "cargo scoop");
        m.put("retract cargo scoop", "cargo scoop");
        m.put("close cargo scoop", "cargo scoop");
        m.put("close cargo bay", "cargo scoop");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Route / plotted route, ship fuel, FSD target, loadout, current location
    // ─────────────────────────────────────────────────────────────────────────

    private void loadRouteAndShipQueries(LinkedHashMap<String, String> m) {
        // Plotted route / jump counts
        m.put("how many jumps remaining", "plotted route");
        m.put("how many jumps left", "plotted route");
        m.put("is next star scoopable", "fuel at next stop");
        m.put("can we scoop next star", "fuel at next stop");
        m.put("scoopable at next stop", "fuel at next stop");
        m.put("fuel at next star", "fuel at next stop");
        m.put("next jump fuel", "fuel at next stop");
        m.put("fuel stop on route", "fuel at next stop");

        // Ship fuel status
        m.put("do we have enough fuel", "fuel status");
        m.put("fuel check", "fuel status");
        m.put("fuel gauge", "fuel status");
        m.put("running low on fuel", "fuel status");
        m.put("tank level", "fuel status");
        m.put("fuel reading", "fuel status");

        // FSD jump target
        m.put("next jump destination", "jump destination");
        m.put("what star are we targeting", "FSD target");
        m.put("analyze jump target", "analyze destination");

        // Ship loadout
        m.put("check loadout", "ship loadout");
        m.put("access loadout", "ship loadout");
        m.put("loadout", "ship loadout");
        m.put("ship configuration", "ship loadout");
        m.put("what modules do we have", "ship loadout");
        m.put("module list", "ship loadout");
        m.put("our build", "ship loadout");
        m.put("ready for a hunt", "ship loadout combat readiness summary");

        // Current location
        m.put("where are we", "what is our current location");
        m.put("where am i", "what is our current location");
        m.put("our coordinates", "current location");
        m.put("what planet are we at", "current location");
        m.put("current system", "current location");
        m.put("our current position", "current location");

        // Stellar objects
        m.put("how many planets", "planets in system");
        m.put("bodies in system", "stellar objects");
        m.put("system bodies", "bodies in system");
        m.put("what bodies are here", "bodies in system");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exploration - bio signals, exobiology, materials, geological, planet surface
    // ─────────────────────────────────────────────────────────────────────────

    private void loadExploration(LinkedHashMap<String, String> m) {
        // Exploration profits
        m.put("discovery profit", "exploration profits");
        m.put("profits from discovery", "exploration profits");
        m.put("profit from discovery", "exploration profits");
        m.put("exobiology profit", "exploration profits");
        m.put("profit from exobiology", "exploration profits");
        m.put("profits from exobiology", "exploration profits");
        m.put("mapping profit", "exploration profits");
        m.put("scan earnings", "exploration profits");
        m.put("worth scanning", "exploration profits");
        m.put("exobiology value", "exploration profits");
        m.put("what are scans worth", "exploration profits");

        // Bio signals - system-wide
        // Map to "which planets have bio signals" to avoid cascading into bio-scan-progress
        m.put("which planets still need bio or organic scans", "which planets have bio signals");
        m.put("which planets still need bio", "which planets have bio signals");
        m.put("which planets still need organic", "which planets have bio signals");
        m.put("which planets need bio scans", "which planets have bio signals");
        m.put("which planets need organic scans", "which planets have bio signals");
        m.put("which planets have unscanned bio", "which planets have bio signals");
        m.put("bio signals in system", "bio scan progress");
        m.put("biological signals in system", "bio scan progress");
        m.put("biosignals in system", "bio scan progress");
        m.put("organics in system", "bio scan progress");
        m.put("how many bio signals", "bio scan progress");

        // Exobiology samples - planet surface
        m.put("bio scans have we completed", "exobiology samples");
        m.put("bio scans completed", "exobiology samples");
        m.put("what organisms remain", "exobiology samples");
        m.put("remaining organisms", "exobiology samples");
        m.put("what's left to scan", "exobiology samples");
        m.put("samples left", "exobiology samples");
        m.put("exobiology remaining", "exobiology samples");
        m.put("how many samples left", "exobiology samples");
        m.put("scan remaining", "exobiology samples");

        // Engineering materials
        m.put("engineering materials", "material inventory");
        m.put("raw materials inventory", "material inventory");
        m.put("manufactured materials", "material inventory");
        m.put("encoded materials", "material inventory");
        m.put("crafting materials", "material inventory");

        // Geological signals
        m.put("geological activity", "geological signals");
        m.put("volcanic activity", "volcanic signals");
        m.put("geology in system", "geo signals");

        // Planet surface queries
        m.put("surface materials", "planet materials");
        m.put("materials on the surface", "planet materials");
        m.put("what materials are here", "planet materials");
        m.put("planetary biome", "planet biome");
        m.put("atmosphere analysis", "biome analysis");
        m.put("what life is here", "planet biome");

        // Last scan
        m.put("most recent scan", "last scan");
        m.put("latest scan", "last scan");
        m.put("what did i last scan", "last scanned object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carrier / station / trade
    // ─────────────────────────────────────────────────────────────────────────

    private void loadCarrierAndTrade(LinkedHashMap<String, String> m) {
        // Carrier status / fuel / ETA / destination
        m.put("carrier balance", "carrier finances");
        m.put("when does carrier arrive", "carrier ETA");
        m.put("carrier arrival time", "carrier ETA");
        m.put("carrier arrival", "carrier ETA");
        m.put("when does carrier jump", "carrier ETA");
        m.put("carrier jump time", "carrier ETA");
        m.put("squadron carrier tritium", "squadron carrier fuel status");
        m.put("squadron carrier fuel level", "squadron carrier fuel status");
        m.put("carrier fuel level", "fleet carrier fuel status");
        m.put("carrier tritium", "fleet carrier fuel status");
        m.put("tritium level", "fleet carrier fuel status");
        m.put("tritium reserve", "fleet carrier fuel status");
        m.put("where is carrier headed", "carrier destination");
        m.put("carrier final destination", "carrier destination");
        m.put("carrier heading", "carrier destination");
        m.put("where is the squadron carrier going", "squadron carrier final destination");
        m.put("where is our squadron carrier going", "squadron carrier final destination");
        m.put("where is squadron carrier going", "squadron carrier final destination");
        // "where is" → "find" fires earlier in the map; also cover the post-substitution form:
        m.put("find the squadron carrier going", "squadron carrier final destination");
        m.put("find our squadron carrier going", "squadron carrier final destination");
        m.put("find squadron carrier going", "squadron carrier final destination");
        m.put("when does the squadron carrier arrive", "squadron carrier ETA");
        m.put("when does our squadron carrier arrive", "squadron carrier ETA");
        m.put("how long until the squadron carrier arrives", "squadron carrier ETA");
        m.put("how long until our squadron carrier arrives", "squadron carrier ETA");

        // Station / outfitting / shipyard
        m.put("station info", "station details");
        m.put("station facilities", "station details");
        m.put("available modules", "modules available");
        m.put("buy modules", "outfitting");
        m.put("ship parts", "outfitting");
        m.put("station equipment", "outfitting");
        m.put("available ships", "ships for sale");
        m.put("buy a ship", "shipyard");
        m.put("new ship options", "shipyard");

        // Trade
        m.put("what are we trading", "trade route");
        m.put("trading schedule", "trade route");
        m.put("our trading plan", "trade route");
        m.put("trade configuration", "trade profile");
        m.put("trading criteria", "trade profile");
        m.put("our trade settings", "trade profile");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game world queries - security, player profile, missions, distance, time
    // ─────────────────────────────────────────────────────────────────────────

    private void loadGameWorldQueries(LinkedHashMap<String, String> m) {
        // System security / politics
        m.put("who owns this system", "faction control");
        m.put("who controls this system", "faction control");
        m.put("controlling power", "faction control");
        m.put("dominant faction", "faction control");
        m.put("security level", "system security");

        // Pirate / massacre missions
        m.put("kills remaining", "kill count");
        m.put("how many pirates left", "kill count");
        m.put("massacre progress", "massacre mission progress");
        m.put("pirates remaining", "massacre mission progress");
        m.put("pirate kills remaining", "massacre mission progress");

        // Player profile - normalize before any "profile" substring can match trade_profile entries
        m.put("player profile", "player profile analysis");

        // Reminder - collapse variations to canonical alias phrase
        m.put("any reminders", "reminder");
        m.put("recall reminder", "reminder");
        m.put("what was the reminder", "reminder");

        // Distance queries
        m.put("range to carrier", "distance to carrier");
        m.put("how far is the carrier", "distance to carrier");
        m.put("where is our carrier", "distance to carrier");
        m.put("range to bio sample", "distance to last bio sample");
        m.put("how far to previous organism", "distance to last bio sample");
        m.put("range from human space", "distance to bubble");
        m.put("range to planet", "distance to stellar object");
        m.put("how far to moon", "distance to stellar object");
        m.put("how far to station", "distance to stellar object");

        // Missions
        m.put("mission status", "active missions");
        m.put("what are our missions", "current missions");
        m.put("ongoing missions", "active missions");
        m.put("markets at outposts in system", "query markets");

        // Bounties
        m.put("bounty earnings", "total bounties");
        m.put("credits from bounties", "total bounties");

        // Time
        m.put("galactic time", "current time");
        m.put("utc time", "current time");
        m.put("earth time", "time on earth");

        // Key bindings - normalize to interrogative so COMMAND/QUERY classifier routes correctly
        m.put("check key bindings", "what key bindings are missing");
        m.put("missing key bindings", "what key bindings are missing");
        m.put("unbound keys", "what key bindings are missing");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phonetic corrections
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Acoustic confusions that SttCorrector cannot catch  cases where Parakeet's
     * phonetic output is structurally unlike the intended word.
     * Do NOT add entries that SttCorrector already handles: vocabulary words of
     * 7+ characters within edit-distance 2 are corrected automatically.
     */
    private void loadPhonetics(LinkedHashMap<String, String> m) {
        m.put(" of", " off");
        m.put("manax", "max");
        m.put("hard points", "hardpoints");
        m.put("scott", "scan");
        m.put("scale", "scan");
        m.put("mining spots", "mining hot spots");
        m.put("net vision", "toggle night vision");
        m.put("division", "toggle night vision");
        m.put("her style", "hostile");
        m.put("hair style", "hostile");
        m.put("did", "deploy");
        m.put("did ploy", "deploy");
        m.put("do they play", "deploy");
        m.put("perimeter", "enter");
        m.put("exit this window", "exit");
        m.put("spectrum scan", "scan system");
        m.put("full spectrum scan", "FSS");
        m.put("full-spectrum scan", "FSS");
        m.put("when wake up", "wake up");
        m.put("nicolai has", "equalize");
        m.put("mitigation", "navigation");
        m.put("codec", "codex");
        m.put("kodak", "codex");
        m.put("they make me", "take me");
        m.put("products", "codex");
        m.put("sleep carrier", "fleet carrier");
        m.put("navigate zip", "exit");
        m.put("first to", "what is");
        m.put("repair", "radar");
        m.put("scam", "scan");
        m.put("rfss", "fss");
        m.put("displayed", "display");
        m.put("i think it's it", "exit");
        m.put("are two", "power to");
        m.put("motor car of", "recover");
        m.put("allocation", "location");
        m.put("distance", "distance");
        m.put("fields", "shields");
        m.put("power two", "power to");
        m.put("what are the systems", "power to systems");
        m.put("continuation", "connection");
        m.put("and is it", "exit");
        m.put("think that it", "exit");
        m.put("I am going to cover", "recover");
        m.put("we recall the", "recover");
        m.put("carcass too", "cargo scoop");
        m.put("pergascope", "cargo scoop");
        m.put("flint", "fleet");
        m.put("fleet crater", "fleet carrier");
        m.put("litigation", "navigation");
        m.put("survey", "SRV");
        m.put("product center", "codex entry");
        m.put("council", "cancel");
        m.put("scalar", "scanner");
        m.put("lensing", "landing");
        m.put("team", "tin");
        m.put("karga", "cargo");
        m.put("skoop", "scoop");
        m.put("cmm commodities", "cmm composites");
        m.put("alex sounds right", "alexandrite");
        m.put("next zip", "exit");
        m.put("recovered", "recover");
        m.put("break over", "recover");
        m.put("from seoul", "from sol");
        m.put("seoul", "sol");
        m.put("roll", "role");
        m.put("career", "carrier");
        m.put("sip", "ship");
        m.put("aligns", "launch");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trash phrases — filler / noise utterances the STT emits as standalone output
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> trashPhrases() {
        return List.of(
                "--", "mm-hmm", "uh-huh", "hmm", "mm", "uh", "um", "ah", "oh", "huh", "eh",

                "yeah", "yep", "yup", "nope", "it", "an", "cool", "the",
                "okay", "ok", "got it", "alright", "alrighty", "sure", "right",
                "hello", "hi", "hey", "bye", "goodbye",
                "so", "well", "now", "anyway", "actually", "basically", "literally",
                "thanks", "thank you", "i'm sorry", "sorry", "excuse me", "pardon",
                "you know", "i see", "i mean", "of course", "no problem",
                "i got it", "don't i", "a ", "or ", "she can", "he can", "you can",
                "like they", "did you", "wh", "i'll", "like", "got a",
                "blow", "fuck", "shit", "just", "i "
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stop words — function words excluded from Reducer word-overlap scoring
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Set<String> stopWords() {
        return Set.of(
                // George Carlin list
                "blow", "fuck", "shit", "piss", "cunt", "cock", "cocksucker", "motherfucker",

                // Function words
                "a", "an", "the", "to", "of", "in", "on", "at", "by", "for",
                "with", "and", "or", "is", "are", "am", "be", "do", "does",
                "what", "where", "how", "which", "any", "our", "my", "me",
                "we", "us", "i", "you", "it", "this", "that", "get", "have",
                "has", "can", "could", "would", "should", "not", "no", "up",
                "here", "there", "some", "much", "many"
        );
    }
}
