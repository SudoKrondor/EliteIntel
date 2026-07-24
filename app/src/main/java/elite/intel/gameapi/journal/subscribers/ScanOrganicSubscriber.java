package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.dao.CodexEntryDao.CodexEntry;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.ScanOrganicEvent;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
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
            CompanionRuntime.narrator().narrate(sb, """
                        Use ONLY facts from sensorData-no invention, external knowledge, or additions like planet/system/payment/bonus/vehicles/scan stages.
                        Rephrase to natural, immersive speech:
                            - Key elements ONLY: genus/species logged, distance/completion if stated.
                            - Use "we", "You" - NEVER "ship", "SRV", or "vehicle".
                            - If 'Remaining genus' listed, provide the exact and detailed list of genus, do not provide a vague reference. 
                            Output EXACTLY:
                                {"text_to_speech_response": "your natural rephrase"}
                    """);
        }
    }

    @Subscribe
    public void onScanOrganicEvent(ScanOrganicEvent event) {
        Thread.ofVirtual().start(() -> {
            playerSession.setTracking(new TargetLocation()); // turn off tracking
            StringBuilder sb = new StringBuilder();
            String scanType = event.getScanType();
            // Language-independent symbols drive every lookup/join; the localised strings are for speech only.
            String genusSymbol = BioForms.normalizeGenus(event.getGenus());
            String speciesSymbol = BioForms.normalizeSpecies(event.getSpecies());
            String genus = event.getGenusLocalised() != null ? event.getGenusLocalised() : BioForms.englishGenusName(genusSymbol);
            playerSession.setCurrentPartial(genusSymbol);
            String species = subtractString(event.getSpeciesLocalised(), genus);
            if (event.getBody() == null) return;
            LocationDto currentLocation = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBody());
            currentLocation.setStarName(locationManager.findBySystemAddress(event.getSystemAddress()).getStarName());
            playerSession.setCurrentLocationId(event.getBody(), event.getSystemAddress());

            boolean isOurDiscovery = currentLocation.isOurDiscovery();
            BioForms.ProjectedPayment paymentData = BioForms.getProjectedPayment(speciesSymbol);

            long payment = paymentData == null ? 0 : paymentData.payment();
            long firstDiscoveryBonus = paymentData == null || !isOurDiscovery ? 0 : paymentData.firstDiscoveryBonus();

            BioForms.BioDetails bioDetails = BioForms.getDetails(speciesSymbol);
            Integer distance = BioForms.colonyRangeOrNull(genusSymbol);

            Integer range = requiredRange(bioDetails, distance);

            if (scan1.equalsIgnoreCase(scanType)) {
                sb.append(" ").append(localizedEvent("event.organic.detected"));
                sb.append(" \"").append(genus).append("\"");
                sb.append(" ").append(localizedEvent("event.organic.species")).append(" ");
                sb.append(species);
                sb.append(" ").append(localizedEvent("event.organic.firstSample"));
                if (range != null) {
                    sb.append(" ").append(localizedEvent("event.organic.requiredDistance", range));
                }

                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, genusSymbol, speciesSymbol, isOurDiscovery);
                bioSampleDto.setScanXof3(1);
                currentLocation.addBioScan(bioSampleDto);
                deleteScannedCodexEntry(genusSymbol, currentLocation);
                locationManager.save(currentLocation);
                announce(sb.toString());

            } else if (scan2.equalsIgnoreCase(scanType)) {
                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, genusSymbol, speciesSymbol, isOurDiscovery);
                bioSampleDto.setScanXof3(2);
                currentLocation.addBioScan(bioSampleDto);
                deleteScannedCodexEntry(genusSymbol, currentLocation);
                locationManager.save(currentLocation);
                announce(localizedEvent("event.organic.sampleLogged", genus));
            } else if (scan3.equalsIgnoreCase(scanType)) {
                sb = new StringBuilder();
                sb.append(localizedEvent("event.organic.finalSample", genus)).append(" ");
                sb.append(localizedEvent("event.organic.collectionComplete")).append(" ");

                BioSampleDto bioSampleDto = createBioSampleDto(genus, species, genusSymbol, speciesSymbol, isOurDiscovery);

                bioSampleDto.setPayout(payment);
                bioSampleDto.setFistDiscoveryBonus(firstDiscoveryBonus);
                bioSampleDto.setScanXof3(3);
                bioSampleDto.setBioSampleCompleted(true);
                bioSampleDto.setOurDiscovery(currentLocation.isOurDiscovery());
                deleteScannedCodexEntry(genusSymbol, currentLocation);
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
                        sb.append(entry.getGenusLocalised()).append(", ");
                    }
                }

                announce(sb.toString());
            }
        });
    }

    /**
     * Minimum colony distance to state for a scan: the species' own range when we have a table entry for it,
     * otherwise the genus default.
     *
     * <p>Both sources are optional and {@code null} is a normal answer, not an error - it means we hold no
     * distance for this organism and simply say nothing about range. Both lookups are keyed by the genus name
     * as the journal localised it, so they miss together whenever the game client is not running in English,
     * or whenever Frontier adds an organism our tables predate.
     *
     * <p>Kept as a method rather than an inline conditional because the inline form was a live NPE: casting one
     * branch to {@code int} gives the whole conditional type {@code int} (JLS 15.25 binary numeric promotion),
     * which unboxes the other branch. The throw killed the scan handler's virtual thread, so the sample was
     * never recorded and nothing was announced - the log line was the only symptom.
     */
    static Integer requiredRange(BioForms.BioDetails bioDetails, Integer genusDefault) {
        return bioDetails != null ? bioDetails.colonyRange() : genusDefault;
    }

    private BioSampleDto createBioSampleDto(String genus, String species, String genusSymbol, String speciesSymbol, boolean isOurDiscovery) {

        LocationDto currentLocation = locationManager.findByLocationData(playerSession.getLocationData());
        BioSampleDto bioSampleDto = new BioSampleDto();
        bioSampleDto.setPrimaryStar(playerSession.getPrimaryStarName());
        bioSampleDto.setPlanetName(currentLocation.getPlanetName());
        bioSampleDto.setPlanetShortName(currentLocation.getPlanetShortName());
        bioSampleDto.setScanLatitude(status.getStatus().getLatitude());
        bioSampleDto.setScanLongitude(status.getStatus().getLongitude());
        bioSampleDto.setGenus(genus);                 // localised - for speech
        bioSampleDto.setSpecies(species);             // localised - for speech
        bioSampleDto.setGenusSymbol(genusSymbol);     // language-independent - for lookups/joins
        bioSampleDto.setSpeciesSymbol(speciesSymbol); // language-independent - for lookups/joins
        bioSampleDto.setOurDiscovery(isOurDiscovery);
        bioSampleDto.setBodyId(currentLocation.getBodyId());
        bioSampleDto.setDistanceToNextSample(distanceToNextSample(genusSymbol, speciesSymbol));
        return bioSampleDto;
    }

    /**
     * Deletes the codex entry nearest to the current scan position whose genus matches.
     * The scanned colony will be within minimum colony range of the matching codex entry.
     * Matching is on the language-independent genus symbol, resolved from each codex entry's
     * stored FDev symbol - so it works on every game language.
     *
     * @param genusSymbol FDev genus symbol stem of the just-scanned organism
     */
    private void deleteScannedCodexEntry(String genusSymbol, LocationDto currentLocation) {
        double lat = status.getStatus().getLatitude();
        double lon = status.getStatus().getLongitude();
        double planetRadius = status.getStatus().getPlanetRadius();
        double minRange = BioForms.getDistance(genusSymbol);
        if (minRange <= 0) return;

        List<CodexEntry> entries =
                codexEntryManager.getForPlanet(currentLocation.getStarName(), currentLocation.getBodyId());
        if (entries == null || entries.isEmpty()) return;

        CodexEntry nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (CodexEntry entry : entries) {
            if (!genusMatches(entry, genusSymbol)) continue;
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

    /**
     * True when the codex entry belongs to the given genus symbol. Prefers the entry's stored FDev
     * symbol (language-independent); falls back to the legacy localised-name substring match for rows
     * saved before the symbol column existed.
     */
    private boolean genusMatches(CodexEntry entry, String genusSymbol) {
        String entrySymbol = entry.getEntrySymbol();
        if (entrySymbol != null && !entrySymbol.isBlank()) {
            return genusSymbol.equals(BioForms.genusStemForSpecies(entrySymbol));
        }
        String english = BioForms.englishGenusName(genusSymbol);
        return english != null
                && entry.getEntryName() != null
                && entry.getEntryName().toLowerCase(Locale.ROOT).contains(english.toLowerCase(Locale.ROOT));
    }

    private double distanceToNextSample(String genusSymbol, String speciesSymbol) {
        BioForms.BioDetails details = BioForms.getDetails(speciesSymbol);
        if (details != null && details.colonyRange() != null) {
            return details.colonyRange();
        }
        return BioForms.getDistance(genusSymbol); // never null; 0 when unknown
    }
}
