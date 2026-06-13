package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.CodexEntryDao.CodexEntry;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.ScanOrganicEvent;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.BioScanDistances;
import elite.intel.util.ExoBio;

import java.util.List;
import java.util.Locale;

import static elite.intel.util.ExoBio.calculateGenusNotYetScanned;
import static elite.intel.util.ExoBio.completedScansForPlanet;
import static elite.intel.util.NavigationUtils.calculateSurfaceDistance;
import static elite.intel.util.StringUtls.localizedEvent;
import static elite.intel.util.StringUtls.subtractString;

public class ScanOrganicSubscriber {

    private final String scan1 = "Log";
    private final String scan2 = "Sample";
    private final String scan3 = "Analyse";
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final Status status = Status.getInstance();

    private static void announce(String sb) {
        if (PlayerSession.getInstance().isDiscoveryAnnouncementOn()) {
            EventBusManager.publish(new SensorDataEvent(sb, """
                        Use ONLY facts from sensorData-no invention, external knowledge, or additions like planet/system/payment/bonus/vehicles/scan stages.
                        Rephrase to natural, immersive speech:
                            - Key elements ONLY: genus/species logged, distance/completion if stated.
                            - Use "we", "You" - NEVER "ship", "SRV", or "vehicle".
                            - If 'Remaining genus' listed, provide the exact and detailed list of genus, do not provide a vague reference. 
                            Output EXACTLY:
                                {"text_to_speech_response": "your natural rephrase"}
                    """));
        }
    }

    @Subscribe
    public void onScanOrganicEvent(ScanOrganicEvent event) {
        Thread.ofVirtual().start(() -> {
            playerSession.setTracking(new TargetLocation()); // turn off tracking
            StringBuilder sb = new StringBuilder();
            String scanType = event.getScanType();
            String genus = event.getGenusLocalised();
            playerSession.setCurrentPartial(genus);
            String species = subtractString(event.getSpeciesLocalised(), genus);
            LocationDto currentLocation = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
            playerSession.setCurrentLocationId(event.getBody(), event.getSystemAddress());

            boolean isOurDiscovery = currentLocation.isOurDiscovery();
            BioForms.ProjectedPayment paymentData = BioForms.getProjectedPayment(genus, species);

            long payment = paymentData == null ? 0 : paymentData.payment();
            long firstDiscoveryBonus = paymentData == null || !isOurDiscovery ? 0 : paymentData.firstDiscoveryBonus();

            BioForms.BioDetails bioDetails = BioForms.getDetails(genus, species);
            Integer distance = BioScanDistances.GENUS_TO_CCR.get(genus);

            Integer range = (bioDetails != null) ? (int) bioDetails.colonyRange() : distance;

            if (scan1.equalsIgnoreCase(scanType)) {
                sb.append(" ").append(localizedEvent("event.organic.detected"));
                sb.append(" \"").append(genus).append("\"");
                sb.append(" ").append(localizedEvent("event.organic.species")).append(" ");
                sb.append(species);
                sb.append(" ").append(localizedEvent("event.organic.firstSample"));
                if (range != null) {
                    sb.append(" ").append(localizedEvent("event.organic.requiredDistance", range));
                }

                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, isOurDiscovery);
                bioSampleDto.setScanXof3(1);
                currentLocation.addBioScan(bioSampleDto);
                deleteScannedCodexEntry(genus, currentLocation);
                locationManager.save(currentLocation);
                announce(sb.toString());

            } else if (scan2.equalsIgnoreCase(scanType)) {
                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, isOurDiscovery);
                bioSampleDto.setScanXof3(2);
                currentLocation.addBioScan(bioSampleDto);
                deleteScannedCodexEntry(genus, currentLocation);
                locationManager.save(currentLocation);
                announce(localizedEvent("event.organic.sampleLogged", genus));
            } else if (scan3.equalsIgnoreCase(scanType)) {
                sb = new StringBuilder();
                sb.append(localizedEvent("event.organic.finalSample", genus)).append(" ");
                sb.append(localizedEvent("event.organic.collectionComplete")).append(" ");

                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, isOurDiscovery);

                bioSampleDto.setPayout(payment);
                bioSampleDto.setFistDiscoveryBonus(firstDiscoveryBonus);
                bioSampleDto.setScanXof3(3);
                bioSampleDto.setBioSampleCompleted(true);
                bioSampleDto.setOurDiscovery(currentLocation.isOurDiscovery());
                deleteScannedCodexEntry(genus, currentLocation);
                playerSession.addBioSample(bioSampleDto);
                playerSession.setCurrentPartial(null);
                currentLocation.deletePartialBioSamples();
                playerSession.clearGenusPaymentAnnounced();
                locationManager.save(currentLocation);

                List<GenusDto> allSpecies = currentLocation.getGenus();
                List<ExoBio.DataDto> completedSpecies = completedScansForPlanet(playerSession.getBioCompletedSamples(), currentLocation.getPlanetName());
                List<GenusDto> remainingSpecies = calculateGenusNotYetScanned(completedSpecies, allSpecies);
                if (remainingSpecies.isEmpty()) {
                    sb.append(" ").append(localizedEvent("event.organic.surveyComplete"));
                } else {
                    sb.append(" ").append(localizedEvent("event.organic.remainingGenus")).append(" ");
                    for (GenusDto entry : remainingSpecies) {
                        sb.append(entry.getSpecies()).append(", ");
                    }
                }

                announce(sb.toString());
            }
        });
    }

    private BioSampleDto createBioSampleDto(String genus, String species, boolean isOurDiscovery) {

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        BioSampleDto bioSampleDto = new BioSampleDto();
        bioSampleDto.setPrimaryStar(playerSession.getPrimaryStarName());
        bioSampleDto.setPlanetName(currentLocation.getPlanetName());
        bioSampleDto.setPlanetShortName(currentLocation.getPlanetShortName());
        bioSampleDto.setScanLatitude(status.getStatus().getLatitude());
        bioSampleDto.setScanLongitude(status.getStatus().getLongitude());
        bioSampleDto.setGenus(genus);
        bioSampleDto.setSpecies(species);
        bioSampleDto.setOurDiscovery(isOurDiscovery);
        bioSampleDto.setBodyId(currentLocation.getBodyId());
        bioSampleDto.setDistanceToNextSample(distanceToNextSample(genus, species));
        return bioSampleDto;
    }

    /**
     * Deletes the codex entry nearest to the current scan position whose genus matches.
     * The scanned colony will be within minimum colony range of the matching codex entry.
     */
    private void deleteScannedCodexEntry(String genus, LocationDto currentLocation) {
        double lat = status.getStatus().getLatitude();
        double lon = status.getStatus().getLongitude();
        double planetRadius = status.getStatus().getPlanetRadius();
        double minRange = BioForms.getDistance(genus);
        if (minRange <= 0) return;

        List<CodexEntry> entries =
                codexEntryManager.getForPlanet(currentLocation.getStarName(), currentLocation.getBodyId());
        if (entries == null || entries.isEmpty()) return;

        CodexEntry nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (CodexEntry entry : entries) {
            if (!entry.getEntryName().toLowerCase(Locale.ROOT).contains(genus.toLowerCase(Locale.ROOT))) continue;
            double dist = calculateSurfaceDistance(lat, lon, entry.getLatitude(), entry.getLongitude(), planetRadius, 0);
            if (dist < minRange && dist < nearestDist) {
                nearest = entry;
                nearestDist = dist;
            }
        }

        if (nearest != null) {
            TargetLocation tl = new TargetLocation();
            tl.setLatitude(nearest.getLatitude());
            tl.setLongitude(nearest.getLongitude());
            codexEntryManager.deleteTrackedEntry(tl);
        }
    }

    private double distanceToNextSample(String genus, String species) {
        BioForms.BioDetails details = BioForms.getDetails(genus, species);
        return details == null ? BioScanDistances.GENUS_TO_CCR.get(genus) : details.colonyRange();
    }
}
