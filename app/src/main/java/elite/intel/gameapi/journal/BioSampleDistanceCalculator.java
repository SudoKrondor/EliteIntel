package elite.intel.gameapi.journal;

import elite.intel.gameapi.data.BioForms;

import static elite.intel.util.NavigationUtils.calculateSurfaceDistance;

public class BioSampleDistanceCalculator {


    /**
     * @param genus   FDev genus symbol (raw journal symbol, stem, or English name - all accepted)
     * @param species FDev species symbol (raw journal symbol, stem, or English name - all accepted)
     */
    public static boolean isFarEnoughFromSample(String genus, String species, double scanLat, double scanLong, double positionLat, double positionLong, double planetRadius) {
        BioForms.BioDetails details = BioForms.getDetails(species);
        // Species range when known, else the genus range; 0 (unknown) means no minimum, so allow the sample.
        double requiredDistance = details != null && details.colonyRange() != null
                ? details.colonyRange()
                : BioForms.getDistance(genus);
        double distanceFromSample = calculateSurfaceDistance(scanLat, scanLong, positionLat, positionLong, planetRadius, 0);
        return distanceFromSample > requiredDistance;
    }
}
