package elite.intel.gameapi;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.db.managers.MaterialManager.symbolKey;

/**
 * Reconstructs the engineering-material inventory at startup, for the case where the app is launched
 * mid-session: the live {@code JournalParser} skips everything written before app start
 * ({@code isReplay()}), so every pickup, trade, and spend that happened before launch would otherwise
 * be invisible and the stored counts would still be whatever the last run left behind.
 *
 * <p>Registered only on the {@link JournalPreScanner} private bus. It anchors on the most recent
 * {@code Materials} event — the only one carrying absolute counts — and replays every movement after
 * that anchor. Because the anchor is absolute, the result does not depend on what was in the database
 * beforehand, so restarting the app twice reconstructs the same inventory rather than counting
 * anything twice.
 *
 * <p>Only replay events are applied ({@code timestamp < APP_START}); anything at or after app start
 * belongs to the live subscribers, so app start is the exact handoff boundary — the same rule
 * {@link FinancePreScanAccumulator} uses. If no {@code Materials} anchor is found in the scanned
 * journals, nothing is written and the stored counts stand.
 *
 * <p>Caps are applied here as the game applies them, so a replayed pickup cannot push a material past
 * its storage ceiling.
 */
public class MaterialsPreScanAccumulator {

    private static final Logger log = LogManager.getLogger(MaterialsPreScanAccumulator.class);

    private final Map<String, Tally> tally = new LinkedHashMap<>();
    private Map<String, Integer> caps;
    private boolean anchored = false;

    // ── the anchor ───────────────────────────────────────────────────────────

    /**
     * A full inventory snapshot: absolute truth that discards everything counted so far. The game
     * writes one at every load, which is why an anchor is essentially always available.
     */
    @Subscribe
    public void onMaterials(MaterialsEvent e) {
        if (!e.isReplay()) return;          // a snapshot at/after app start is handled live
        tally.clear();
        put(e.getRaw(), MaterialsType.GAME_RAW);
        put(e.getManufactured(), MaterialsType.GAME_MANUFACTURED);
        put(e.getEncoded(), MaterialsType.GAME_ENCODED);
        anchored = true;
    }

    private void put(List<MaterialsEvent.Material> materials, MaterialsType type) {
        if (materials == null) return;
        for (MaterialsEvent.Material material : materials) {
            String key = symbolKey(material.getName());
            if (key == null) continue;
            tally.put(key, new Tally(type, material.getCount(), material.getLocalisedName()));
        }
    }

    // ── movements after the anchor ───────────────────────────────────────────

    @Subscribe
    public void onMaterialCollected(MaterialCollectedEvent e) {
        add(e, e.getName(), MaterialsType.fromJournalCategory(e.getCategory()), e.getCount(), e.getNameLocalised());
    }

    @Subscribe
    public void onMaterialTrade(MaterialTradeEvent e) {
        MaterialTradeEvent.TradedMaterial paid = e.getPaid();
        if (paid != null) subtract(e, paid.getMaterial(), paid.getQuantity());

        MaterialTradeEvent.TradedMaterial received = e.getReceived();
        if (received != null) {
            add(e, received.getMaterial(), MaterialsType.fromJournalCategory(received.getCategory()),
                    received.getQuantity(), received.getMaterialLocalised());
        }
    }

    @Subscribe
    public void onMaterialDiscarded(MaterialDiscardedEvent e) {
        subtract(e, e.getName(), e.getCount());
    }

    @Subscribe
    public void onEngineerCraft(EngineerCraftEvent e) {
        if (e.getIngredients() == null) return;
        for (EngineerCraftEvent.Ingredient ingredient : e.getIngredients()) {
            subtract(e, ingredient.getName(), ingredient.getCount());
        }
    }

    @Subscribe
    public void onSynthesis(SynthesisEvent e) {
        if (e.getMaterials() == null) return;
        for (SynthesisEvent.Material material : e.getMaterials()) {
            subtract(e, material.getName(), material.getCount());
        }
    }

    @Subscribe
    public void onTechnologyBroker(TechnologyBrokerEvent e) {
        if (e.getMaterials() == null) return;
        for (TechnologyBrokerEvent.Material material : e.getMaterials()) {
            subtract(e, material.getName(), material.getCount());
        }
    }

    @Subscribe
    public void onScientificResearch(ScientificResearchEvent e) {
        subtract(e, e.getName(), e.getCount());
    }

    @Subscribe
    public void onMissionCompleted(MissionCompletedEvent e) {
        if (e.getMaterialsReward() == null) return;
        for (MissionCompletedEvent.MaterialReward reward : e.getMaterialsReward()) {
            add(e, reward.getName(), MaterialsType.fromJournalCategory(reward.getCategory()),
                    reward.getCount(), reward.getNameLocalised());
        }
    }

    // ── the ledger ───────────────────────────────────────────────────────────

    private void add(BaseEvent e, String symbol, MaterialsType type, int count, String displayName) {
        String key = key(e, symbol);
        if (key == null) return;
        Tally current = tally.get(key);
        int held = (current == null ? 0 : current.amount) + count;
        int cap = capFor(key);
        tally.put(key, new Tally(
                current != null && type == MaterialsType.GAME_UNKNOWN ? current.type : type,
                cap > 0 ? Math.min(held, cap) : held,
                displayName != null ? displayName : (current == null ? null : current.displayName)));
    }

    private void subtract(BaseEvent e, String symbol, int count) {
        String key = key(e, symbol);
        if (key == null) return;
        Tally current = tally.get(key);
        if (current == null) return;         // nothing on the books to spend
        tally.put(key, new Tally(current.type, Math.max(current.amount - count, 0), current.displayName));
    }

    /**
     * The key to move, or null when this event must not be counted here at all.
     */
    private String key(BaseEvent e, String symbol) {
        if (!anchored || !e.isReplay()) return null;
        return symbolKey(symbol);
    }

    private int capFor(String symbol) {
        if (caps == null) caps = MaterialManager.getInstance().capsBySymbol();
        return caps.getOrDefault(symbol, 0);
    }

    /**
     * Writes the reconstructed inventory. Call once after the pre-scan has processed all journal files.
     * No-op if no anchor was found.
     */
    public void persist() {
        if (!anchored) {
            log.info("MaterialsPreScan: no Materials anchor found, leaving material counts untouched");
            return;
        }
        List<MaterialManager.Holding> holdings = new ArrayList<>(tally.size());
        tally.forEach((symbol, held) ->
                holdings.add(new MaterialManager.Holding(symbol, held.type, held.amount, held.displayName)));
        MaterialManager.getInstance().replaceAll(holdings);
        log.info("MaterialsPreScan: reconstructed {} materials held", holdings.size());
    }

    private record Tally(MaterialsType type, int amount, String displayName) {
    }
}
