package elite.intel.util;

import elite.intel.gameapi.SignalName;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;

public class ExoBio {

    public static List<DataDto> completedScansForPlanet(List<BioSampleDto> allBioSamples, String planetName) {
        ArrayList<DataDto> result = new ArrayList<>();
        for (BioSampleDto bioSample : allBioSamples) {
            if (bioSample.getPlanetName().equalsIgnoreCase(planetName)) {
                result.add(new DataDto(planetName, bioSample.getGenus(), bioSample.getGenusSymbol(), bioSample.getSpecies(), bioSample.getScanXof3(), 3 == bioSample.getScanXof3()));
            }
        }

        return result;
    }

    public static List<GenusDto> calculateGenusNotYetScanned(List<ExoBio.DataDto> completedSamples, List<GenusDto> genusListForCurrentLocation) {
        ArrayList<GenusDto> result = new ArrayList<>();
        for (GenusDto genus : genusListForCurrentLocation) {
            boolean found = false;
            for (ExoBio.DataDto sample : completedSamples) {
                if (genusMatches(sample, genus)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(genus);
            }
        }
        return result;
    }

    /**
     * Bio signals known for a body, taking the higher of the two independent sources: the FSS signal list
     * and the body's own {@code bioSignals} count (set by DSS/SAA). Either can be absent - a body sampled
     * on foot without an FSS pass has no signal list at all - and a zero from an absent source must not be
     * read as "no organics here".
     */
    public static int bioSignalsDetected(LocationDto location) {
        if (location == null) return 0;
        int fromFssSignals = 0;
        List<FSSBodySignalsEvent.Signal> fssSignals = location.getFssSignals();
        if (fssSignals != null) {
            for (FSSBodySignalsEvent.Signal signal : fssSignals) {
                String type = SignalName.display(signal.getTypeLocalised(), signal.getType());
                if (type != null && type.toLowerCase().contains("bio")) {
                    fromFssSignals += signal.getCount();
                }
            }
        }
        return Math.max(fromFssSignals, location.getBioSignals());
    }

    /**
     * Whether every genus a surface scan found on this body has been sampled to completion.
     *
     * <p>An empty detected list is not a finished survey but an unknown one - a body sampled on foot
     * without a DSS has no genus list at all - so it answers {@code false}. Reading it the other way
     * declared the survey complete after the first organism on any body that was never mapped.
     */
    public static boolean isSurveyComplete(List<GenusDto> detectedGenus, List<BioSampleDto> completedSamples, String planetName) {
        if (detectedGenus == null || detectedGenus.isEmpty() || planetName == null) return false;
        List<DataDto> completed = completedScansForPlanet(
                completedSamples == null ? List.of() : completedSamples, planetName);
        return calculateGenusNotYetScanned(completed, detectedGenus).isEmpty();
    }

    /**
     * Match a completed scan to a detected genus. Prefers the language-independent FDev genus symbol;
     * falls back to the localized display name for data recorded before symbols were captured.
     */
    private static boolean genusMatches(DataDto sample, GenusDto genus) {
        if (sample.genusSymbol() != null && genus.getGenusSymbol() != null) {
            return sample.genusSymbol().equals(genus.getGenusSymbol());
        }
        return sample.genus() != null && sample.genus().equalsIgnoreCase(genus.getGenusLocalised());
    }


    public record DataDto(String planetName, String genus, String genusSymbol, String species, Integer scanXof3,
                          boolean completed) implements ToYamlConvertable {

        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}
