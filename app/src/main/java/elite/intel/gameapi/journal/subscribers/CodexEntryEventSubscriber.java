package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.dao.CodexEntryDao;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;

import static elite.intel.util.StringUtls.localizedEvent;

public class CodexEntryEventSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onCodexEntryEvent(CodexEntryEvent event) {
        Thread.ofVirtual().start(() -> {
            final Status status = Status.getInstance();

            if (event.getBodyID() == null) return;
            LocationDto currentLocation = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            currentLocation.setStarName(locationManager.findBySystemAddress(event.getSystemAddress()).getStarName());
            playerSession.setCurrentLocationId(event.getBodyID(), event.getSystemAddress());
            StringBuilder sb = new StringBuilder();

            String nameLocalised = event.getNameLocalised();
            if (nameLocalised == null) nameLocalised = event.getName();
            if (nameLocalised == null) nameLocalised = "Unknown";

            // Resolve genus/species from the language-independent FDev symbol (e.g.
            // "$Codex_Ent_Shrubs_01_F_Name;"), not the localized display name - so this works
            // on every game language. Non-organic entries resolve to null and skip bio output.
            String genusSymbol = BioForms.genusStemForSpecies(event.getName());
            String speciesSymbol = BioForms.normalizeSpecies(event.getName());
            int bioSampleDistance = genusSymbol != null ? BioForms.getDistance(genusSymbol) : 0;
            BioForms.ProjectedPayment projectedPayment = null;
            if (genusSymbol != null) {
                projectedPayment = BioForms.getProjectedPayment(speciesSymbol);
                if (projectedPayment == null) {
                    projectedPayment = BioForms.getAverageProjectedPayment(genusSymbol);
                }
            }
            String genus = bioSampleDistance > 0 ? genusSymbol : null;

            boolean alreadyHaveThisEntry = codexEntryManager.checkIfExist(currentLocation.getStarName(), currentLocation.getBodyId(), nameLocalised);


            if (!alreadyHaveThisEntry && event.isNewEntry()) {
                sb.append(" ").append(localizedEvent("event.codex.newEntry")).append(" ");
            } else {
                sb.append(" ").append(localizedEvent("event.codex.entry")).append(" ");
            }
            sb.append(localizedEvent("event.codex.name")).append(" ");
            String[] split = nameLocalised.split(" - ", 2);
            if (split.length == 2) {
                sb.append(split[0]).append(", ").append(localizedEvent("event.codex.variant")).append(" ").append(split[1]).append(", ");
            } else {
                sb.append(nameLocalised).append(", ");
            }
            sb.append(localizedEvent("event.codex.category")).append(" ");
            sb.append(event.getSubCategoryLocalised()).append(". ");

            if (bioSampleDistance > 0 && !alreadyHaveThisEntry) {
                sb.append(" ").append(localizedEvent("event.codex.sampleDistance", bioSampleDistance));
            }


            if (!alreadyHaveThisEntry) {
                sb.append(", ");
                if (event.getVoucherAmount() > 0) {
                    sb.append(localizedEvent("event.codex.voucher", event.getVoucherAmount()));
                }
                Boolean isAnnounced = playerSession.paymentHasBeenAnnounced(genus);

                if (projectedPayment != null && projectedPayment.payment() != null && !isAnnounced) {
                    sb.append(" ").append(localizedEvent("event.codex.vistaPayment", projectedPayment.payment()));
                    if (projectedPayment.firstDiscoveryBonus() != null && currentLocation.isOurDiscovery()) {
                        sb.append(" ").append(localizedEvent("event.codex.firstDiscoveryBonus", projectedPayment.firstDiscoveryBonus()));
                    }
                    playerSession.addAnnouncedGenusPayment(genus);
                }
            } else {
                for (CodexEntryDao.CodexEntry entry : codexEntryManager.getForPlanet(currentLocation.getStarName(), currentLocation.getBodyId())) {
                    boolean isNameMatched = entry.getEntryName().equals(nameLocalised);
                    double distanceFromPreviousSample = NavigationUtils.calculateSurfaceDistance(
                            status.getStatus().getLatitude(),
                            status.getStatus().getLongitude(),
                            entry.getLatitude(),
                            entry.getLongitude(),
                            status.getStatus().getPlanetRadius(),
                            0
                    );
                    if (genus != null && isNameMatched && distanceFromPreviousSample < bioSampleDistance) {
                        sb.append(" ").append(localizedEvent("event.codex.tooProximate"));
                        break;
                    }
                }
            }

            boolean isOrganic = "$Codex_SubCategory_Organic_Structures;".equalsIgnoreCase(event.getSubCategory());

            if (playerSession.isDiscoveryAnnouncementOn()) {
                String organicGuidance = isOrganic
                        ? "- List Genus, projected payment for collecting 3 samples, and minimum distance between samples ONLY if those values appear in the data above; otherwise omit them."
                        : "- This is NOT a biological/organic entry. Do NOT mention genus, species, sample counts, sample payments, or minimum sample distances - those concepts do not apply here.";
                String instructions = """
                                Database updated.
                                Provide essential summary.
                        - Only facts, no speculation. Do not invent, estimate, or default any value that is not present in the data above.
                        %s
                                - IF there is a warning announce it, else do not mention that there are no warnings.
                        - Do not append any extra data.
                        - Express credit amounts using K (thousands) or M (millions) shorthand as appropriate (e.g. 50K, 1.2M).
                        """.formatted(organicGuidance);
                CompanionRuntime.narrator().narrate(sb.toString(), instructions);
            }
            if (isOrganic) {
                codexEntryManager.save(event);
            }
            locationManager.save(currentLocation);
        });
    }
}
