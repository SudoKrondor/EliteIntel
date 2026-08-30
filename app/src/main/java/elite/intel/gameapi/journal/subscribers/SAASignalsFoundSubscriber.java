package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.SAASignalsFoundEvent;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.MaterialDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.ExoBio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static elite.intel.gameapi.journal.events.dto.LocationDto.LocationType.PLANETARY_RING;
import static elite.intel.util.StringUtls.localizedEvent;

public class SAASignalsFoundSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static void announce(String sb) {
        Status status = Status.getInstance();
        if (status.isInMainShip() && !status.isLanded() && !status.isDocked()) {
            String instructions = """
                        Report the signals detected on this body. List each signal type briefly.
                        If biological signals are present, name each genus and state the average projected payout.
                        State a first-discovery bonus only if the sensor data gives one, and keep any doubt it
                        expresses about it - never present an uncertain bonus as earnings.
                        If the sensor data says the survey here is already complete, say so and list no genus.
                    """;
            CompanionRuntime.narrator().narrate(sb, instructions);
        }
    }

    @Subscribe
    public void onSAASignalsFound(SAASignalsFoundEvent event) {
        Thread.ofVirtual().start(() -> {
            // Read-modify-write under a per-body lock so a concurrent Scan for the same body can't
            // interleave and overwrite these signals/materials/classification (blind whole-JSON upsert).
            locationManager.updateBody(event.getSystemAddress(), event.getBodyID(), location -> {
            StringBuilder sb = new StringBuilder();
            LocationDto primaryStarLocation = locationManager.findBySystemAddress(event.getSystemAddress());
            location.setPlanetName(event.getBodyName());
            location.setBodyId(event.getBodyID());
            location.setStarName(primaryStarLocation.getStarName());
            location.setX(primaryStarLocation.getX());
            location.setY(primaryStarLocation.getY());
            location.setZ(primaryStarLocation.getZ());

            location.addSaaSignals(event.getSignals());

            List<SAASignalsFoundEvent.Signal> signals = event.getSignals();
            int signalsFound = signals != null ? signals.size() : 0;

            if (signalsFound > 0) {
                int liveSignals = event.getGenuses() != null ? event.getGenuses().size() : 0;
                sb.append(" ").append(localizedEvent("event.signals.found")).append(" ");
                for (SAASignalsFoundEvent.Signal signal : signals) {
                    sb.append(" ").append(localizedEvent("event.signals.type", signal.getType()));
                }

                if (liveSignals > 0) {
                    location.setBioSignals(liveSignals);
                    location.setGenus(toGenusDto(event.getGenuses(), location.getPlanetName()));
                    boolean alreadySampledOut = surveyAlreadyComplete(location);

                    if (alreadySampledOut) {
                        // A body yields its organics once. Re-listing the genuses here reads as work to
                        // do, so the commander is told plainly that this one is spent instead.
                        sb.append(" ").append(localizedEvent("event.signals.bioSurveyAlreadyComplete"));
                    } else {
                        sb.append(" ").append(localizedEvent("event.signals.exobio", liveSignals));

                        long averageProjectedPayment = 0;
                        long averageFirstDiscoveryBonus = 0;
                        for (SAASignalsFoundEvent.Genus genus : event.getGenuses()) {
                            BioForms.ProjectedPayment averagePayment = BioForms.getAverageProjectedPayment(genus.getGenus());
                            if (averagePayment != null) {
                                averageProjectedPayment = averageProjectedPayment + averagePayment.payment();
                                averageFirstDiscoveryBonus = averageFirstDiscoveryBonus + averagePayment.firstDiscoveryBonus();
                            }
                            sb.append(" ");
                            sb.append(genus.getGenusLocalised());
                            sb.append(", ");
                        }
                        sb.append(localizedEvent("event.signals.avgPayment", averageProjectedPayment));
                        // The bonus is Vista Genomics' payment for being first to log the organism, which
                        // is not the same question as who charted the body. On a body nobody had found,
                        // nobody can have sampled it either, so the bonus is ours to claim. On a charted
                        // body it is genuinely unknown - the journal never says whether anyone sampled
                        // here - so it is offered as a possibility, never added to a projection.
                        if (averageFirstDiscoveryBonus > 0) {
                            sb.append(" ").append(location.isOurDiscovery()
                                    ? localizedEvent("event.signals.firstDiscoveryBonus", averageFirstDiscoveryBonus)
                                    : localizedEvent("event.signals.firstDiscoveryBonusUncertain", averageFirstDiscoveryBonus));
                        }
                    }

                } else if (event.getBodyName().contains("Ring")) {
                    // Rings are bodies. Classify and enrich the same `location` we save below - do NOT
                    // build a second DTO with the same name, or the trailing save(location) overwrites it.
                    location.setLocationType(PLANETARY_RING);
                    location.setMaterials(toMaterials(event.getSignals()));

                    String parentBodyName = event.getBodyName().replaceAll(" [A-Z] Ring$", "");
                    location.setParentBodyName(parentBodyName);
                    LocationDto parent = locationManager.getLocation(
                            playerSession.getPrimaryStarName(),
                            findParentId(
                                    parentBodyName,
                                    locationManager.findAllBySystemAddress(event.getSystemAddress()
                                    )
                            )
                    );
                    if (parent != null) {
                        parent.setHasRings(true);
                        locationManager.save(parent);
                    }
                }

                if (playerSession.isDiscoveryAnnouncementOn()) {
                    announce(sb.toString());
                }
            } else {
                if (playerSession.isDiscoveryAnnouncementOn()) {
                    announce(localizedEvent("event.signals.none"));
                }
            }
            });
        });
    }


    private long findParentId(String parentBodyName, Collection<LocationDto> allLocationsInStarSystem) {
        for (LocationDto dto : allLocationsInStarSystem) {
            if (dto.getPlanetName().equalsIgnoreCase(parentBodyName)) {
                return dto.getBodyId();
            }
        }
        return 0;
    }

    /**
     * Whether this body is sampled out, recording the answer on the body when it is.
     *
     * <p>The stored flag is the authority, because the derivation behind it does not survive a sale:
     * completed samples are session state that {@code SellOrganicData} clears. Deriving it as well
     * heals bodies finished before the flag existed, and bodies whose last sample was taken while
     * this DSS had not yet written a genus list to count against.
     *
     * <p>What it replaced answered a different question: it said "complete" as soon as <em>one</em>
     * genus on the body had been sampled, which silenced the survey briefing for every body the
     * commander had merely started.
     */
    private boolean surveyAlreadyComplete(LocationDto location) {
        if (location.isBioScansCompleted()) return true;
        if (!ExoBio.isSurveyComplete(location.getGenus(), playerSession.getBioCompletedSamples(), location.getPlanetName())) {
            return false;
        }
        location.markBioScansCompleted();
        return true;
    }

    private List<MaterialDto> toMaterials(List<SAASignalsFoundEvent.Signal> signals) {
        ArrayList<MaterialDto> materialDtos = new ArrayList<>();
        for (SAASignalsFoundEvent.Signal signal : signals) {
            materialDtos.add(new MaterialDto(signal.getType(), 100, true));
        }
        return materialDtos;
    }

    /**
     * WHY the first-discovery bonus is stored unconditionally rather than only when the body is ours:
     * it is the table figure for the organism, not a claim that we will be paid it. Baking the
     * body's discovery state into the number froze whatever that state happened to be at DSS time,
     * so a body wrongly flagged as ours kept an unearned bonus in every later projection even after
     * a real scan corrected the flag. Callers gate the figure on the body's discovery state as they
     * read it; see {@link elite.intel.ai.brain.actions.handlers.queries.AnalyzeExplorationProfitsQuery}.
     */
    private List<GenusDto> toGenusDto(List<SAASignalsFoundEvent.Genus> organics, String planetName) {
        ArrayList<GenusDto> result = new ArrayList<>();
        for (SAASignalsFoundEvent.Genus genus : organics) {
            GenusDto dto = new GenusDto();
            dto.setGenusLocalised(genus.getGenusLocalised());   // localised - for speech/display
            dto.setGenusSymbol(BioForms.normalizeGenus(genus.getGenus())); // language-independent - for joins
            dto.setPlanetName(planetName);
            BioForms.ProjectedPayment projectedPayment = BioForms.getAverageProjectedPayment(genus.getGenus());
            if (projectedPayment != null && projectedPayment.payment() != null) {
                dto.setRewardInCredits(projectedPayment.payment());
                dto.setBonusCreditsForFirstDiscovery(projectedPayment.firstDiscoveryBonus());
            }
            result.add(dto);
        }
        return result;
    }
}
