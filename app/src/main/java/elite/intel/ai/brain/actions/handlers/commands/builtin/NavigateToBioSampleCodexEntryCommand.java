package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.dao.CodexEntryDao;
import elite.intel.db.managers.BioSamplesManager;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.*;

import static elite.intel.util.NavigationUtils.calculateSurfaceDistance;

/**
 * Self-describing "navigate to bio sample codex entry" command.
 * Owns its own execution: body migrated 1:1 from the legacy NavigateToNextCodexEntry,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateToBioSampleCodexEntryCommand implements IntelCommand {
    public static final String ID = "navigate_to_bio_sample_codex_entry";

    @Override
    public String llmDescription() {
        return "Plot surface navigation to the nearest saved biological-sample / codex location on the current planet (guides to the next organism to scan).";
    }


    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final BioSamplesManager bioSamplesManager = BioSamplesManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /**
     * Navigates to a biological-sample codex entry the app already holds in the DB - it infers no parameters from
     * the commander, so it is offered in any control mode (ship, SRV, fighter, on foot). When no codex entry is
     * known for the current planet, {@link #execute} says so rather than the command being hidden.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv() || status.isInFighter() || status.isOnFoot();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());

        if (currentLocation == null || status.getStatus() == null) {
            return StringUtls.localizedLlm("handler.navigate.noLocation");
        }

        List<CodexEntryDao.CodexEntry> codexEntries = getCodexEntries(currentLocation);
        if (codexEntries.isEmpty()) {
            return StringUtls.localizedLlm("handler.codex.notFound");
        }

        double planetRadius = status.getStatus().getPlanetRadius();
        double playerLat = status.getStatus().getLatitude();
        double playerLon = status.getStatus().getLongitude();

        Tuple<CodexEntryDao.CodexEntry, String> target = findBestBioTarget(codexEntries, currentLocation.getPartialBioSamples(), playerLat, playerLon, planetRadius);

        if (target.getSample() == null) {
            return target.getNote();
        }

        TargetLocation nav = new TargetLocation();
        nav.setLatitude(target.getSample().getLatitude());
        nav.setLongitude(target.getSample().getLongitude());
        nav.setEnabled(true);
        nav.setRequestedTime(System.currentTimeMillis());
        playerSession.setTracking(nav);
        playerSession.setNavigationAnnouncementOn(true);

        return StringUtls.localizedLlm("handler.codex.heading", target.getSample().getEntryName());
    }

    private List<CodexEntryDao.CodexEntry> getCodexEntries(LocationDto currentLocation) {
        List<BioSampleDto> completedBioSamples = bioSamplesManager.findByPlanetName(currentLocation.getStarName(), currentLocation.getPlanetName());
        List<CodexEntryDao.CodexEntry> codexEntries = codexEntryManager.getForPlanet(currentLocation.getStarName(), currentLocation.getBodyId());
        if (codexEntries == null) {
            return new ArrayList<>();
        }
        List<BioSampleDto> partialBioSamples = currentLocation.getPartialBioSamples();

        if (!partialBioSamples.isEmpty()) {
            List<CodexEntryDao.CodexEntry> filteredResult = new ArrayList<>();
            for (CodexEntryDao.CodexEntry entry : codexEntries) {
                if (completedBioSamples != null && completedBioSamples.stream()
                        .anyMatch(c -> entryMatchesSample(entry, c))) {
                    continue;
                }
                for (BioSampleDto partial : partialBioSamples) {
                    if (entryMatchesSample(entry, partial)) {
                        filteredResult.add(entry);
                        break;
                    }
                }
            }
            return filteredResult;
        }

        if (completedBioSamples == null || completedBioSamples.isEmpty()) return codexEntries;

        List<CodexEntryDao.CodexEntry> filteredResult = new ArrayList<>();
        for (CodexEntryDao.CodexEntry entry : codexEntries) {
            boolean isCompleted = completedBioSamples.stream().anyMatch(c -> entryMatchesSample(entry, c));
            if (!isCompleted) filteredResult.add(entry);
        }
        return filteredResult;
    }

    /**
     * Genus symbol stem of a codex entry, or null for legacy rows with no stored symbol.
     */
    private static String genusSymbolOf(CodexEntryDao.CodexEntry entry) {
        String sym = entry.getEntrySymbol();
        return (sym != null && !sym.isBlank()) ? BioForms.genusStemForSpecies(sym) : null;
    }

    /**
     * True when a codex entry belongs to the same genus as a bio sample. Prefers the language-independent
     * FDev symbol on both sides; falls back to a localized display-name substring match for legacy rows.
     */
    private static boolean entryMatchesSample(CodexEntryDao.CodexEntry entry, BioSampleDto sample) {
        String es = genusSymbolOf(entry);
        if (es != null && sample.getGenusSymbol() != null) {
            return es.equals(sample.getGenusSymbol());
        }
        return sample.getGenus() != null && entry.getEntryName() != null
                && entry.getEntryName().toLowerCase(Locale.ROOT).contains(sample.getGenus().toLowerCase(Locale.ROOT));
    }

    /**
     * True when a codex entry belongs to the given genus symbol. Prefers the entry's stored symbol;
     * falls back to matching the English genus name inside the (localized) entry name for legacy rows.
     */
    private static boolean entryMatchesGenusSymbol(CodexEntryDao.CodexEntry entry, String genusSymbol) {
        if (genusSymbol == null) return false;
        String es = genusSymbolOf(entry);
        if (es != null) return genusSymbol.equals(es);
        String english = BioForms.englishGenusName(genusSymbol);
        return english != null && entry.getEntryName() != null
                && entry.getEntryName().toLowerCase(Locale.ROOT).contains(english.toLowerCase(Locale.ROOT));
    }

    /**
     * Stable per-genus grouping key: symbol stem when known, else the (legacy) first word of the entry name.
     */
    private static String genusKeyOf(CodexEntryDao.CodexEntry entry) {
        String es = genusSymbolOf(entry);
        if (es != null) return es;
        String name = entry.getEntryName();
        return name != null ? name.split(" ")[0] : "";
    }

    private Tuple<CodexEntryDao.CodexEntry, String> findBestBioTarget(List<CodexEntryDao.CodexEntry> codexEntries, List<BioSampleDto> partials, double playerLat, double playerLon, double planetRadius) {
        String partialGenus = playerSession.getCurrentPartial();
        boolean hasPartials = partialGenus != null && !partials.isEmpty();
        if (hasPartials) {
            return findPartialTarget(codexEntries, partials, partialGenus, playerLat, playerLon, planetRadius);
        } else {
            return findFreshTarget(codexEntries, playerLat, playerLon, planetRadius);
        }
    }

    /**
     * Has a partial scan in progress: find the nearest codex entry for the tracked genus
     * that is far enough from all existing partial scan locations.
     */
    private Tuple<CodexEntryDao.CodexEntry, String> findPartialTarget(List<CodexEntryDao.CodexEntry> codexEntries, List<BioSampleDto> partials, String partialGenus, double playerLat, double playerLon, double planetRadius) {
        CodexEntryDao.CodexEntry best = null;
        double bestDist = Double.MAX_VALUE;

        for (CodexEntryDao.CodexEntry entry : codexEntries) {
            if (entry.getLatitude() == 0 && entry.getLongitude() == 0) continue;
            // partialGenus is the FDev genus symbol stem set by ScanOrganicSubscriber.
            if (!entryMatchesGenusSymbol(entry, partialGenus)) continue;
            if (isTooCloseToAnyPartialOfSameGenus(entry, partialGenus, partials, planetRadius)) continue;

            double dist = calculateSurfaceDistance(playerLat, playerLon, entry.getLatitude(), entry.getLongitude(), planetRadius, 0);
            if (dist < bestDist) {
                bestDist = dist;
                best = entry;
            }
        }

        if (best == null) return new Tuple<>(null, StringUtls.localizedLlm("handler.codex.noPartialTarget"));
        return new Tuple<>(best, "");
    }

    /**
     * No partial scan: find the genus with the most codex entries that are all at least
     * minRange apart from each other (greedy). Prefers genera with 3 viable entries,
     * then 2, then 1. Ties broken by distance to the nearest entry.
     */
    private Tuple<CodexEntryDao.CodexEntry, String> findFreshTarget(List<CodexEntryDao.CodexEntry> codexEntries, double playerLat, double playerLon, double planetRadius) {
        Map<String, List<CodexEntryDao.CodexEntry>> byGenus = new LinkedHashMap<>();
        for (CodexEntryDao.CodexEntry entry : codexEntries) {
            if (entry.getLatitude() == 0 && entry.getLongitude() == 0) continue;
            byGenus.computeIfAbsent(genusKeyOf(entry), k -> new ArrayList<>()).add(entry);
        }

        int bestCount = 0;
        CodexEntryDao.CodexEntry bestEntry = null;
        double bestDist = Double.MAX_VALUE;

        for (Map.Entry<String, List<CodexEntryDao.CodexEntry>> genusGroup : byGenus.entrySet()) {
            String genus = genusGroup.getKey();
            List<CodexEntryDao.CodexEntry> entries = genusGroup.getValue();
            double minRange = BioForms.getDistance(genus);

            // Sort nearest-first so the greedy pick yields the most player-convenient set
            entries.sort((a, b) -> Double.compare(
                    calculateSurfaceDistance(playerLat, playerLon, a.getLatitude(), a.getLongitude(), planetRadius, 0),
                    calculateSurfaceDistance(playerLat, playerLon, b.getLatitude(), b.getLongitude(), planetRadius, 0)));

            // Greedy independent set: pick entries >= minRange from all already-picked
            List<CodexEntryDao.CodexEntry> feasible = new ArrayList<>();
            for (CodexEntryDao.CodexEntry candidate : entries) {
                boolean tooClose = minRange > 0 && feasible.stream().anyMatch(picked ->
                        calculateSurfaceDistance(
                                candidate.getLatitude(), candidate.getLongitude(),
                                picked.getLatitude(), picked.getLongitude(), planetRadius, 0) < minRange);
                if (!tooClose) feasible.add(candidate);
            }

            if (feasible.isEmpty()) continue;
            double nearestDist = calculateSurfaceDistance(playerLat, playerLon,
                    feasible.get(0).getLatitude(), feasible.get(0).getLongitude(), planetRadius, 0);

            if (feasible.size() > bestCount || (feasible.size() == bestCount && nearestDist < bestDist)) {
                bestCount = feasible.size();
                bestEntry = feasible.get(0);
                bestDist = nearestDist;
            }
        }

        if (bestEntry == null) return new Tuple<>(null, StringUtls.localizedLlm("handler.codex.notFound"));
        return new Tuple<>(bestEntry, "");
    }

    private boolean isTooCloseToAnyPartialOfSameGenus(CodexEntryDao.CodexEntry entry, String genusSymbol, List<BioSampleDto> partials, double planetRadius) {
        double minAllowed = BioForms.getDistance(genusSymbol);
        if (minAllowed <= 0) return false;

        for (BioSampleDto partial : partials) {
            boolean sameGenus = partial.getGenusSymbol() != null
                    ? genusSymbol.equals(partial.getGenusSymbol())
                    : genusSymbol.equalsIgnoreCase(partial.getGenus());
            if (!sameGenus) continue;
            double dist = calculateSurfaceDistance(
                    partial.getScanLatitude(), partial.getScanLongitude(),
                    entry.getLatitude(), entry.getLongitude(), planetRadius, 0);
            if (dist <= minAllowed) return true;
        }
        return false;
    }

    class Tuple<S, N> {
        private final S sample;
        private final N note;

        Tuple(S sample, N note) {
            this.sample = sample;
            this.note = note;
        }

        public S getSample() {
            return sample;
        }

        public N getNote() {
            return note;
        }
    }
}
