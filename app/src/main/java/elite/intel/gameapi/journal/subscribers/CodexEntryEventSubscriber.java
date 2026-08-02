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
            // The star name is the only thing this handler contributes to the body, so it is written
            // here, at once, under the per-body lock. The DTO below is read-only from this point on.
            //
            // WHY: this used to hold a DTO loaded now and save it whole at the end - after a blocking
            // narration call. A codex entry fires in the same instant as the first ScanOrganic of the
            // species, so that trailing save landed seconds later on top of the sample the scan
            // handler had recorded, and the first sample of every new species vanished.
            LocationDto currentLocation = locationManager.findBySystemAddress(event.getSystemAddress(), event.getBodyID());
            String starName = locationManager.findBySystemAddress(event.getSystemAddress()).getStarName();
            currentLocation.setStarName(starName); // for the codex lookups below, persisted or not
            // A codex entry can name a body we hold no row for. That row is keyed on the body name,
            // which this event does not carry, so there is nothing to persist - and persisting anyway
            // means an insert with a null key, which would now fail ahead of the narration instead of
            // after it.
            if (currentLocation.getPlanetName() != null) {
                locationManager.updateBody(event.getSystemAddress(), event.getBodyID(),
                        location -> location.setStarName(starName));
            }
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
                CompanionRuntime.narrator().narrate(sb.toString(), narrationInstructions(isOrganic));
            }
            if (isOrganic) {
                codexEntryManager.save(event);
            }
        });
    }

    /**
     * Phrasing instructions for the codex narration.
     *
     * <p>WHY the money rules are this explicit: a codex payload can carry up to three unrelated credit
     * figures - the one-off report voucher from the journal event, the Vista Genomics payment for a
     * complete set of three samples, and the first-discovery bonus. The earlier organic guidance
     * whitelisted only the set-of-three payment and the sample distance, so the voucher was a labelled
     * number in the data with no sanctioned slot in the instructions. A small model resolves that
     * mismatch by inventing a slot: a 2500-credit voucher on a Tussock Propagito codex entry came out as
     * "2.5K per sample. 1M for three" - a per-sample rate that does not exist in the game or the payload.
     * Every figure the payload can carry must therefore be named here, and relabelling/deriving forbidden.
     *
     * <p>The voucher reaches both branches: it is appended for any entry we do not already hold, and 12 of 77
     * real codex events carry one (2500 or 50000) - including a geology entry, where it is the only credit
     * figure in the payload because a non-organic name resolves to a null genus and so yields no Vista payment.
     */
    public static String narrationInstructions(boolean isOrganic) {
        String organicGuidance = isOrganic
                ? """
                - Of the bio figures, state only those actually present above: the codex report voucher, the Vista Genomics payment, the first-discovery bonus, and the minimum distance between samples.
                - The Vista Genomics payment is for a complete set of THREE samples. There is no per-sample rate; never state one."""
                : """
                - This is NOT a biological/organic entry. Do NOT mention genus, species, sample counts, sample payments, or minimum sample distances - those concepts do not apply here.
                - The report voucher is the only credit figure such an entry can carry. State it as the voucher it is; do not turn it into a sample or survey payment.""";
        return """
                        Database updated.
                        Provide essential summary.
                - Only facts, no speculation. Do not invent, estimate, or default any value that is not present in the data above.
                - Every figure above arrives with its own label. Keep each figure with the label it came with; never relabel one, merge two, or derive a further figure (a rate, a total, a remainder) from them.
                %s
                        - IF there is a warning announce it, else do not mention that there are no warnings.
                - Do not append any extra data.
                - Express credit amounts using K (thousands) or M (millions) shorthand as appropriate (e.g. 50K, 1.2M).
                """.formatted(organicGuidance);
    }
}
