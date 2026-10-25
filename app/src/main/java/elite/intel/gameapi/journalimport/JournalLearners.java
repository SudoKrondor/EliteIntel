package elite.intel.gameapi.journalimport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.db.managers.CommodityCatalogue;
import elite.intel.db.managers.ExoMasteryManager;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.HuntingGroundTally;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.ScanOrganicEvent;
import elite.intel.gameapi.journal.subscribers.ArchivedJourneyRecord;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import elite.intel.i18n.Language;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the import learns from one commander's journals, fed one event at a time, oldest first.
 * <p>
 * Only what an old journal can still teach without contradicting the present is taken:
 * <ul>
 *   <li>hunting grounds and pirate massacre contracts ({@link HuntingGroundTally});</li>
 *   <li>the species the commander sampled out, which the game will not let them sample again - so the
 *       Exo-Mastery catalogue never sends them back for it;</li>
 *   <li>systems and bodies they scanned, and which of them they discovered or mapped first
 *       ({@link ArchivedJourneyRecord});</li>
 *   <li>materials and commodities the catalogues do not know, named in the language of the client that wrote the
 *       journal.</li>
 * </ul>
 * Nothing that describes the commander's present is read at all: position, ship, cargo, credits, missions in
 * progress, materials held. A journal from 2019 knows none of that any more.
 * <p>
 * Journals whose commander could not be told get only what belongs to everyone - resource sites and names. A
 * contract, a sample or a discovery has to be filed under the commander who made it.
 */
final class JournalLearners {

    private static final String FINAL_ORGANIC_SCAN = "Analyse";

    private static final Set<String> SHARED_EVENTS = Set.of(
            "Materials", "MaterialCollected",
            "Cargo", "MiningRefined", "ProspectedAsteroid", "MarketBuy", "MarketSell", "CollectCargo", "EjectCargo");
    private static final Set<String> COMMANDER_EVENTS = Set.of("FSDJump", "Scan", "ScanOrganic");

    private final boolean commanderKnown;
    private final HuntingGroundTally huntingGrounds;
    private final ExoMasteryManager exoMastery;
    private final MaterialManager materials;
    private final CommodityCatalogue commodities;

    /**
     * Names already filed this run, per client language: a commander who traded gold four thousand times needs it
     * learned once.
     */
    private final Set<String> namesSeen = new HashSet<>();

    private int bioSamples;
    private int bodiesScanned;

    /**
     * @param commanderKnown whether these journals' commander is known, and so whether their own records may be
     *                       written - into the file the caller has opened for that commander
     */
    JournalLearners(boolean commanderKnown) {
        this.commanderKnown = commanderKnown;
        this.huntingGrounds = new HuntingGroundTally(HuntingGroundManager.getInstance(), commanderKnown);
        this.exoMastery = ExoMasteryManager.getInstance();
        this.materials = MaterialManager.getInstance();
        this.commodities = CommodityCatalogue.getInstance();
    }

    boolean wants(String eventName) {
        return huntingGrounds.wants(eventName)
                || SHARED_EVENTS.contains(eventName)
                || (commanderKnown && COMMANDER_EVENTS.contains(eventName));
    }

    /**
     * @param clientLanguage the language of the client that wrote this journal, from its header
     */
    void accept(String eventName, JsonObject json, Optional<Language> clientLanguage) {
        if (huntingGrounds.wants(eventName)) huntingGrounds.accept(eventName, json);
        switch (eventName) {
            case "Materials" -> {
                for (String category : List.of("Raw", "Manufactured", "Encoded")) {
                    for (JsonObject material : objects(json, category)) {
                        learnMaterial(text(material, "Name"), MaterialsType.fromJournalCategory(category),
                                text(material, "Name_Localised"), clientLanguage);
                    }
                }
            }
            case "MaterialCollected" -> learnMaterial(text(json, "Name"),
                    MaterialsType.fromJournalCategory(text(json, "Category")), text(json, "Name_Localised"), clientLanguage);
            case "Cargo" -> {
                for (JsonObject item : objects(json, "Inventory")) {
                    learnCommodity(text(item, "Name"), text(item, "Name_Localised"), clientLanguage);
                }
            }
            case "ProspectedAsteroid" -> {
                for (JsonObject item : objects(json, "Materials")) {
                    learnCommodity(text(item, "Name"), text(item, "Name_Localised"), clientLanguage);
                }
            }
            case "MiningRefined" -> learnCommodity(text(json, "Type"), text(json, "Type_Localised"), clientLanguage);
            case "MarketBuy", "MarketSell", "CollectCargo", "EjectCargo" -> {
                // Only with the client's own word for it. Journals older than the localised fields carry the bare
                // symbol here, and "Lowtemperaturediamond" is not a name to teach the catalogue.
                String localised = text(json, "Type_Localised");
                if (localised != null) learnCommodity(text(json, "Type"), localised, clientLanguage);
            }
            case "FSDJump" -> {
                if (commanderKnown) ArchivedJourneyRecord.arrival(new FSDJumpEvent(json));
            }
            case "Scan" -> {
                if (commanderKnown) {
                    ArchivedJourneyRecord.scan(new ScanEvent(json));
                    bodiesScanned++;
                }
            }
            case "ScanOrganic" -> {
                if (commanderKnown) recordSample(new ScanOrganicEvent(json));
            }
            default -> {
            }
        }
    }

    /**
     * Writes what was held back until the commander's last journal was read.
     */
    void finish() {
        huntingGrounds.persistResourceSites();
    }

    int bioSamples() {
        return bioSamples;
    }

    int bodiesScanned() {
        return bodiesScanned;
    }

    private void recordSample(ScanOrganicEvent event) {
        if (!FINAL_ORGANIC_SCAN.equalsIgnoreCase(event.getScanType()) || event.getBody() == null) return;
        String species = BioForms.normalizeSpecies(event.getSpecies());
        if (species == null || species.isBlank()) return;
        exoMastery.recordSampled(event.getSystemAddress(), event.getBody(), species);
        bioSamples++;
    }

    private void learnMaterial(String symbol, MaterialsType type, String localised, Optional<Language> language) {
        if (symbol == null || !firstSighting("m:", symbol, language)) return;
        materials.learnName(symbol, type, localised, language);
    }

    private void learnCommodity(String symbol, String localised, Optional<Language> language) {
        if (symbol == null || !firstSighting("c:", symbol, language)) return;
        commodities.learnAll(List.of(new CommodityCatalogue.Sighting(symbol, localised)), language);
    }

    private boolean firstSighting(String kind, String symbol, Optional<Language> language) {
        String normalized = JournalSymbol.normalize(symbol);
        if (normalized == null) return false;
        return namesSeen.add(kind + language.map(Enum::name).orElse("?") + ":" + normalized);
    }

    private static List<JsonObject> objects(JsonObject json, String field) {
        if (!json.has(field) || !json.get(field).isJsonArray()) return List.of();
        JsonArray array = json.getAsJsonArray(field);
        return array.asList().stream().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).toList();
    }

    private static String text(JsonObject json, String field) {
        JsonElement value = json.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
