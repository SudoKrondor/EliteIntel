package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.dao.CodexEntryDao;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.data.BioForms;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.NavigationUtils;

import static elite.intel.util.StringUtls.capitalizeWords;

public class CodexEntryEventSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Subscribe
    public void onCodexEntryEvent(CodexEntryEvent event) {
        Thread.ofVirtual().start(() -> {
            final Status status = Status.getInstance();

            LocationDto currentLocation = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            playerSession.setCurrentLocationId(event.getBodyID(), event.getSystemAddress());
            StringBuilder sb = new StringBuilder();

            String nameLocalised = event.getNameLocalised();
            if (nameLocalised == null) nameLocalised = event.getName();
            if (nameLocalised == null) nameLocalised = "Unknown";

            String[] nameParts = nameLocalised.split(" ");
            String firstWordOfEntryName = nameParts[0];
            String secondWordOfEntryName = nameParts.length > 1 ? nameParts[1] : null;
            int bioSampleDistance = BioForms.getDistance(firstWordOfEntryName);
            BioForms.ProjectedPayment projectedPayment = (secondWordOfEntryName != null)
                    ? BioForms.getProjectedPayment(capitalizeWords(firstWordOfEntryName), capitalizeWords(secondWordOfEntryName))
                    : BioForms.getAverageProjectedPayment(capitalizeWords(firstWordOfEntryName));
            String genus = bioSampleDistance > 0 ? capitalizeWords(firstWordOfEntryName) : null;

            boolean alreadyHaveThisEntry = codexEntryManager.checkIfExist(currentLocation.getStarName(), currentLocation.getBodyId(), nameLocalised);


            if (!alreadyHaveThisEntry && event.isNewEntry()) {
                sb.append(" New Codex Entry: ");
            } else {
                sb.append(" Codex Entry: ");
            }
            sb.append(" Name: ");
            String[] split = nameLocalised.split(" - ", 2);
            if (split.length == 2) {
                sb.append(split[0]).append(", variant ").append(split[1]).append(", ");
            } else {
                sb.append(nameLocalised).append(", ");
            }
            sb.append(" Category: ");
            sb.append(event.getSubCategoryLocalised()).append(". ");

            if (bioSampleDistance > 0 && !alreadyHaveThisEntry) {
                sb.append(" Minimum distance between samples for collection: ").append(bioSampleDistance).append(" meters. ");
            }


            if (!alreadyHaveThisEntry) {
                sb.append(", ");
                if (event.getVoucherAmount() > 0) {
                    sb.append("Voucher Amount: ");
                    sb.append(event.getVoucherAmount());
                    sb.append(" credits. ");
                }
                Boolean isAnnounced = playerSession.paymentHasBeenAnnounced(genus);

                if (projectedPayment != null && projectedPayment.payment() != null && !isAnnounced) {
                    sb.append("Vista Genomics Payment: ").append(projectedPayment.payment()).append(" credits. For a complete set of three samples. ");
                    if (projectedPayment.firstDiscoveryBonus() != null && currentLocation.isOurDiscovery()) {
                        sb.append(projectedPayment.firstDiscoveryBonus());
                        sb.append(" credits bonus for first discovery.");
                    }
                    sb.append(".");
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
                        sb.append(" WARNING: Codex entry too proximate to previous sample of same genus- insufficient separation for sample diversity!");
                        break;
                    }
                }
            }

            if (playerSession.isDiscoveryAnnouncementOn()) {
                String instructions = """
                                Database updated. 
                                Provide essential summary.
                        - Only facts, no speculation.
                                - IF entry is about organics: List Genus, payment and distance if provided.
                                - IF there is a warning announce it, else do not mention that there are no warnings.
                        - Do not append any extra data.
                        - Express credit amounts using K (thousands) or M (millions) shorthand as appropriate (e.g. 50K, 1.2M).
                        """;
                EventBusManager.publish(new SensorDataEvent(sb.toString(), instructions));
            }
            if ("$Codex_SubCategory_Organic_Structures;".equalsIgnoreCase(event.getSubCategory())) {
                codexEntryManager.save(event);
            }
            locationManager.save(currentLocation);
        });
    }
}
