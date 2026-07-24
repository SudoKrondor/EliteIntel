package elite.intel.util;

import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.events.dto.GenusDto;
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
