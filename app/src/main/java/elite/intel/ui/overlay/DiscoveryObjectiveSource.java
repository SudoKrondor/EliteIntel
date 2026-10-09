package elite.intel.ui.overlay;

import elite.intel.db.managers.FssSurveyManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * The explorer's card for a system nobody had discovered before the commander arrived.
 * <p>
 * It follows the survey as it goes:
 * <ol>
 *   <li>on arrival, before the honk, only the system name under a NEW DISCOVERY banner - nothing has
 *       been counted yet, so there is nothing else to say;</li>
 *   <li>after the honk, the FSS progress: bodies found out of the bodies the honk counted;</li>
 *   <li>as the FSS resolves them, a line per kind of find worth a detour (icy bodies, high metal content
 *       worlds, gas giants, bodies with geological signals), and a green line per body with life showing
 *       how many of its genuses have been sampled.</li>
 * </ol>
 * At a body with sampling to do - in orbit, gliding, landed or on foot - it stands aside for the
 * exobiology card, which says more about that one body; back in supercruise it takes the card again.
 * <p>
 * Shown only when discovery announcements are on, like the exobiology card, and ranked
 * {@link HudObjective#PRIORITY_AMBIENT}: a mission or a colonisation run the commander committed to keeps
 * the card. It is scoped to the system the commander is in, so leaving it clears the card and a plotted
 * route, ranked below it, takes over.
 * <p>
 * Derived on every poll from stored state - the primary star's discovery flag, the body rows the scans
 * wrote, the honk's count and the samples taken - so it reads the same after a restart.
 */
public class DiscoveryObjectiveSource implements HudObjectiveSource {

    /**
     * The overlay's whole row budget; must match {@code MAX_ROWS} in {@code overlay/src/hud.h}.
     */
    static final int MAX_ROWS = 8;

    private static final String ICY_BODY = "icy body";
    private static final String HIGH_METAL_CONTENT = "high metal content body";
    private static final String GAS_GIANT = "gas giant";

    private final PlayerSession playerSession;
    private final LocationManager locationManager;
    private final FssSurveyManager fssSurveys;
    private final ExobiologyObjectiveSource exobiology;
    private final BooleanSupplier atBody;

    public DiscoveryObjectiveSource() {
        this(PlayerSession.getInstance(), LocationManager.getInstance(), FssSurveyManager.getInstance(),
                new ExobiologyObjectiveSource(), () -> Status.getInstance().hasLatLong());
    }

    /**
     * Seam for tests.
     *
     * @param atBody whether the ship is near enough a body to have a latitude and longitude on it
     */
    DiscoveryObjectiveSource(PlayerSession playerSession, LocationManager locationManager,
                             FssSurveyManager fssSurveys, ExobiologyObjectiveSource exobiology,
                             BooleanSupplier atBody) {
        this.playerSession = playerSession;
        this.locationManager = locationManager;
        this.fssSurveys = fssSurveys;
        this.exobiology = exobiology;
        this.atBody = atBody;
    }

    @Override
    public Optional<HudObjective> currentObjective() {
        if (!Boolean.TRUE.equals(playerSession.isDiscoveryAnnouncementOn())) return Optional.empty();

        LocationData<Long, Long> here = playerSession.getLocationData();
        Long systemAddress = here == null ? null : here.getSystemAddress();
        if (systemAddress == null || systemAddress == 0) return Optional.empty();

        // The arrival scan of the primary star is what says nobody had been here. ScanEventSubscriber
        // already refuses the flag from a nav beacon or a populated system, where it cannot be believed.
        LocationDto primaryStar = locationManager.findBySystemAddress(systemAddress);
        if (primaryStar == null || !primaryStar.isOurDiscovery()) return Optional.empty();

        if (atBodyWithSamplingToDo(systemAddress, here.getInGameId())) return Optional.empty();

        List<LocationDto> bodies = locationManager.findBodies(systemAddress);
        Map<Long, List<BioSampleDto>> samplesByBody = new HashMap<>();
        for (LocationDto body : bodies) {
            if (hasLife(body) && body.getGenus() != null && !body.getGenus().isEmpty()) {
                samplesByBody.put(body.getBodyId(), exobiology.completedSamplesOn(body));
            }
        }

        String systemName = primaryStar.getStarName() == null || primaryStar.getStarName().isBlank()
                ? playerSession.getPrimaryStarName()
                : primaryStar.getStarName();
        return Optional.of(card("discovery:" + systemAddress, systemName,
                fssSurveys.find(systemAddress).orElse(null), bodies, samplesByBody));
    }

    /**
     * Whether the exobiology card has a better claim: the ship is at a body, and that body still has a
     * genus to sample.
     */
    private boolean atBodyWithSamplingToDo(long systemAddress, Long bodyId) {
        if (bodyId == null || !atBody.getAsBoolean()) return false;
        LocationDto body = locationManager.findBySystemAddress(systemAddress, bodyId);
        return body != null && exobiology.hasWorkOn(body);
    }

    /**
     * The card, from what is on record. Pure so the stages can be tested without a database.
     *
     * @param survey        the honk's count, or null before the honk
     * @param bodies        the system's stars and planets on record, in body order
     * @param samplesByBody completed samples per BodyID, for the bodies a DSS has surveyed
     */
    static HudObjective card(String id, String systemName, FssSurveyManager.Survey survey,
                             List<LocationDto> bodies, Map<Long, List<BioSampleDto>> samplesByBody) {
        String subtitle = systemName == null ? null : systemName.toUpperCase(Locale.ROOT);
        List<HudRow> rows = new ArrayList<>();
        if (survey != null) {
            rows.add(fssProgress(survey, bodies.size()));

            List<HudRow> life = new ArrayList<>();
            for (LocationDto body : bodies) {
                if (hasLife(body)) life.add(lifeRow(body, samplesByBody.getOrDefault(body.getBodyId(), List.of())));
            }
            List<HudRow> finds = findRows(bodies);

            // Life is the list the commander works through, so it gets the room first; the tallies take
            // what is left, and when even life does not fit, its last line counts the rest.
            int room = MAX_ROWS - rows.size();
            int findsShown = Math.min(finds.size(), Math.max(0, room - life.size()));
            rows.addAll(finds.subList(0, findsShown));
            room -= findsShown;
            if (life.size() <= room) {
                rows.addAll(life);
            } else {
                rows.addAll(life.subList(0, room - 1));
                rows.add(HudRow.of(HudText.get("overlay.card.row.moreLife"), "+" + (life.size() - (room - 1)),
                        HudRow.State.GOOD));
            }
        }
        return new HudObjective(id, HudText.get("overlay.card.title.newDiscovery"), subtitle, rows,
                HudObjective.PRIORITY_AMBIENT);
    }

    /**
     * Bodies found out of bodies counted. FSSAllBodiesFound is exact where the stored rows are not -
     * a body the game resolved but whose scan the app never wrote would leave the bar a step short forever.
     */
    private static HudRow fssProgress(FssSurveyManager.Survey survey, int bodiesOnRecord) {
        int total = survey.bodyCount();
        int found = survey.allBodiesFound() ? total : Math.min(bodiesOnRecord, total);
        return HudRow.progress(HudText.get("overlay.card.row.fss"), found, total,
                found >= total ? HudRow.State.GOOD : HudRow.State.NORMAL);
    }

    private static List<HudRow> findRows(List<LocationDto> bodies) {
        int icy = 0, highMetal = 0, gasGiants = 0, geological = 0;
        for (LocationDto body : bodies) {
            String planetClass = body.getPlanetClass() == null ? "" : body.getPlanetClass().toLowerCase(Locale.ROOT);
            if (planetClass.equals(ICY_BODY)) icy++;
            if (planetClass.equals(HIGH_METAL_CONTENT)) highMetal++;
            if (planetClass.contains(GAS_GIANT)) gasGiants++;
            if (body.getGeoSignals() > 0) geological++;
        }
        List<HudRow> rows = new ArrayList<>();
        addCount(rows, "overlay.card.row.icyBodies", icy);
        addCount(rows, "overlay.card.row.highMetal", highMetal);
        addCount(rows, "overlay.card.row.gasGiants", gasGiants);
        addCount(rows, "overlay.card.row.geoSignals", geological);
        return rows;
    }

    private static void addCount(List<HudRow> rows, String labelKey, int count) {
        if (count > 0) rows.add(HudRow.of(HudText.get(labelKey), HudText.count(count)));
    }

    /**
     * Genuses sampled out of genuses on the body. Before a DSS only the FSS's signal count is known, so
     * that stands in as the total; a body whose survey was finished and sold reads as done.
     */
    private static HudRow lifeRow(LocationDto body, List<BioSampleDto> completedSamples) {
        List<GenusDto> genus = body.getGenus();
        boolean surveyed = genus != null && !genus.isEmpty();
        int total = surveyed ? genus.size() : body.getBioSignals();
        int done;
        if (body.isBioScansCompleted()) {
            done = total;
        } else if (surveyed) {
            done = total - ExobiologyObjectiveSource.remainingGenus(body, completedSamples).size();
        } else {
            done = 0;
        }
        return HudRow.progress(HudText.get("overlay.card.row.lifeOn", bodyLabel(body)), done, total, HudRow.State.GOOD);
    }

    private static boolean hasLife(LocationDto body) {
        return body.getBioSignals() > 0 || (body.getGenus() != null && !body.getGenus().isEmpty());
    }

    private static String bodyLabel(LocationDto body) {
        String shortName = body.getPlanetShortName();
        String name = shortName == null || shortName.isBlank() ? body.getPlanetName() : shortName;
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }
}
