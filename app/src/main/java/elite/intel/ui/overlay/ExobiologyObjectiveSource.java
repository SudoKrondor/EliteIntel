package elite.intel.ui.overlay;

import elite.intel.db.managers.BioSamplesManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.util.ExoBio;

import java.util.*;

/**
 * Projects the genuses still to sample in the current system into a HUD
 * objective.
 * <p>
 * This is a self-set objective, not a contract: a DSS turns up biological
 * signals and the sampling list is the commander's own to-do list. It is
 * therefore shown only when discovery announcements are on - the same switch
 * that decides whether the commander wants to hear about discoveries at all -
 * and it ranks below every committed task, so an accepted mission or a plotted
 * route always keeps the card. There is only room for one.
 * <p>
 * Every poll re-derives the card from stored state - the genuses a DSS wrote to
 * the body, less the samples the scans recorded - so the card is the same after a
 * restart as it was before one. The DSS itself needs no wiring: the poll that
 * follows it finds the body because the row now has genuses on it. The one thing
 * the derivation cannot recover is a survey that has been finished and sold, since
 * selling clears the samples; the body carries a completion flag for that case and
 * a flagged body never gets a card.
 * <p>
 * The card follows the commander rather than the scan: the body being stood on
 * wins, then a body with samples already started, then the rest in body order.
 * Everything else falls out of the same derivation - a sampled genus drops off
 * the card by itself, and the card disappears when the system's last genus is
 * sampled or when the commander jumps away.
 */
public class ExobiologyObjectiveSource implements HudObjectiveSource {

    /**
     * Three samples make one organism; the game has no other value here.
     */
    private static final int SAMPLES_PER_GENUS = 3;

    /**
     * Genus rows the card will list. One header row plus this plus the overflow
     * row is the overlay's whole row budget, so raising it silently drops rows
     * at the far end of the pipe instead of showing more.
     */
    static final int MAX_GENUS_ROWS = 6;

    private final PlayerSession playerSession;
    private final LocationManager locationManager;
    private final BioSamplesManager bioSamplesManager;

    public ExobiologyObjectiveSource() {
        this(PlayerSession.getInstance(), LocationManager.getInstance(), BioSamplesManager.getInstance());
    }

    /**
     * Seam for tests.
     */
    ExobiologyObjectiveSource(PlayerSession playerSession, LocationManager locationManager,
                              BioSamplesManager bioSamplesManager) {
        this.playerSession = playerSession;
        this.locationManager = locationManager;
        this.bioSamplesManager = bioSamplesManager;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        if (!Boolean.TRUE.equals(playerSession.isDiscoveryAnnouncementOn())) return Optional.empty();

        LocationData<Long, Long> here = playerSession.getLocationData();
        Long systemAddress = here == null ? null : here.getSystemAddress();
        if (systemAddress == null || systemAddress == 0) return Optional.empty();

        // Scoped to the current system, which is also what clears the card: bodies
        // in the system just left are not read at all.
        List<LocationDto> bioBodies = locationManager.findBioSignalBodies(systemAddress);
        if (bioBodies == null || bioBodies.isEmpty()) return Optional.empty();

        return bioBodies.stream()
                .sorted(byRelevance(here.getInGameId()))
                .map(body -> card(body, completedSamplesOn(body), "exobiology:" + systemAddress + ":" + body.getBodyId()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Which body's list to show when a system holds several. The body the
     * commander is at wins - that is the one being worked - then one with samples
     * already started, then body order so the choice is stable while nothing
     * changes.
     */
    private static Comparator<LocationDto> byRelevance(Long currentBodyId) {
        return Comparator
                .comparing((LocationDto body) -> !isCurrentBody(body, currentBodyId))
                .thenComparing(body -> body.getPartialBioSamples() == null || body.getPartialBioSamples().isEmpty())
                .thenComparingLong(LocationDto::getBodyId);
    }

    private static boolean isCurrentBody(LocationDto body, Long currentBodyId) {
        return currentBodyId != null && currentBodyId == body.getBodyId();
    }

    /**
     * Finished samples recorded on this body. Scoped to the body rather than
     * reading every sample the commander has ever taken, which is a career-long
     * list and would be deserialised in full on each poll.
     */
    private List<BioSampleDto> completedSamplesOn(LocationDto body) {
        String primaryStar = body.getStarName() == null || body.getStarName().isBlank()
                ? playerSession.getPrimaryStarName()
                : body.getStarName();
        if (primaryStar == null || primaryStar.isBlank()) return List.of();
        return bioSamplesManager.findByPlanetName(primaryStar, body.getPlanetName());
    }

    /**
     * The card for one body, or empty when there is nothing left to sample there.
     * <p>
     * Pure so the row shapes can be tested without a database: everything it
     * needs is on the body or in the completed-sample list.
     */
    static Optional<HudObjective> card(LocationDto body, List<BioSampleDto> completedSamples, String id) {
        if (body == null || body.getPlanetName() == null || body.getPlanetName().isBlank()) {
            return Optional.empty();
        }
        // A body sampled out stays sampled out, and the stored flag is the only thing that still says
        // so after a sale: selling clears the completed samples the remainder below is derived from,
        // which brought the whole genus list back onto the card as work to do.
        if (body.isBioScansCompleted()) return Optional.empty();
        List<GenusDto> detected = body.getGenus();
        if (detected == null || detected.isEmpty()) return Optional.empty();

        List<ExoBio.DataDto> completed = ExoBio.completedScansForPlanet(
                completedSamples == null ? List.of() : completedSamples, body.getPlanetName());
        List<GenusDto> remaining = ExoBio.calculateGenusNotYetScanned(completed, detected);
        if (remaining.isEmpty()) return Optional.empty();

        Map<String, Integer> partials = partialsByGenus(body.getPartialBioSamples());

        List<HudRow> rows = new ArrayList<>();
        rows.add(HudRow.progress(HudText.get("overlay.card.row.genus"),
                detected.size() - remaining.size(), detected.size()));

        int shown = Math.min(remaining.size(), MAX_GENUS_ROWS);
        for (GenusDto genus : remaining.subList(0, shown)) {
            String label = displayName(genus);
            Integer sampled = sampledStage(partials, genus);
            if (sampled != null && sampled > 0 && sampled < SAMPLES_PER_GENUS) {
                // The one being worked on right now: the bar says how many more
                // colonies to walk to, which is the only number that matters then.
                rows.add(HudRow.progress(label, sampled, SAMPLES_PER_GENUS, HudRow.State.WARN));
            } else {
                rows.add(HudRow.of(label, payout(genus, body.isOurDiscovery())));
            }
        }
        if (remaining.size() > shown) {
            rows.add(HudRow.of(HudText.get("overlay.card.row.moreGenus"), "+" + (remaining.size() - shown)));
        }

        return Optional.of(new HudObjective(
                id,
                HudText.get("overlay.card.title.exobiology"),
                bodyLabel(body),
                rows,
                HudObjective.PRIORITY_AMBIENT));
    }

    /**
     * Samples taken but not finished, keyed by genus, keeping the furthest scan
     * reached. Partials accumulate as separate rows per scan stage, and the third
     * one is deleted on completion, so the highest surviving stage is the state.
     * <p>
     * Indexed under both the FDev symbol and the localised name, because samples
     * recorded before symbols were captured carry only the name - a partial that
     * fails to match simply shows the genus without its bar, which reads as "not
     * started" on a genus that is two thirds done.
     */
    private static Map<String, Integer> partialsByGenus(List<BioSampleDto> partials) {
        Map<String, Integer> result = new HashMap<>();
        if (partials == null) return result;
        for (BioSampleDto sample : partials) {
            if (sample == null || sample.getScanXof3() == null) continue;
            for (String key : genusKeys(sample.getGenusSymbol(), sample.getGenus())) {
                result.merge(key, sample.getScanXof3(), Math::max);
            }
        }
        return result;
    }

    /**
     * The furthest sample stage reached on a genus, under either of its keys.
     */
    private static Integer sampledStage(Map<String, Integer> partials, GenusDto genus) {
        Integer furthest = null;
        for (String key : genusKeys(genus.getGenusSymbol(), genus.getGenusLocalised())) {
            Integer stage = partials.get(key);
            if (stage != null && (furthest == null || stage > furthest)) furthest = stage;
        }
        return furthest;
    }

    /**
     * The language-independent FDev symbol and the localised name, when held.
     */
    private static List<String> genusKeys(String genusSymbol, String genusLocalised) {
        List<String> keys = new ArrayList<>(2);
        if (genusSymbol != null && !genusSymbol.isBlank()) keys.add(genusSymbol);
        if (genusLocalised != null && !genusLocalised.isBlank()) {
            keys.add(genusLocalised.toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private static String displayName(GenusDto genus) {
        String name = genus.getGenusLocalised();
        if (name == null || name.isBlank()) name = genus.getGenusSymbol();
        return name == null ? HudText.get("overlay.card.row.unknownGenus") : name.toUpperCase(Locale.ROOT);
    }

    /**
     * Blank rather than "0 cr" when we hold no payout figure for the genus.
     * <p>
     * The first-discovery bonus is counted only on a body we charted ourselves. Elsewhere it is a
     * maybe - the journal never says whether another commander has already sampled here - and a row
     * with one number on it has no room to say "maybe", so the certain figure is the one shown.
     */
    private static String payout(GenusDto genus, boolean ourDiscovery) {
        long reward = genus.getRewardInCredits() + (ourDiscovery ? genus.getBonusCreditsForFirstDiscovery() : 0);
        return reward > 0 ? HudText.credits(reward) : "";
    }

    private static String bodyLabel(LocationDto body) {
        String shortName = body.getPlanetShortName();
        String name = shortName == null || shortName.isBlank() ? body.getPlanetName() : shortName;
        return name.toUpperCase(Locale.ROOT);
    }
}
