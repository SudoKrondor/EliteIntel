package elite.intel.gameapi.journal.events.dto;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.Md5Utils;
import elite.intel.util.json.ToJsonConvertible;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class BioSampleDto extends BaseJsonDto implements ToJsonConvertible, ToYamlConvertable {

    long payout;
    long fistDiscoveryBonus;
    boolean ourDiscovery;
    private String planetName;
    private String planetShortName;
    private String primaryStar;
    private long bodyId;
    private String genus;
    /**
     * Language-independent FDev genus symbol stem (e.g. "Tussocks"); used for all lookups/joins.
     */
    private String genusSymbol;
    /**
     * Language-independent FDev species symbol stem (e.g. "Tussocks_02"); used for all lookups/joins.
     */
    private String speciesSymbol;
    private double scanLatitude;
    private double scanLongitude;
    private double distanceToNextSample;
    private boolean playerFarEnough;
    private boolean bioSampleCompleted;
    private String species;
    private Integer scanXof3;

    public long getBodyId() {
        return bodyId;
    }

    public void setBodyId(long bodyId) {
        this.bodyId = bodyId;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    /**
     * FDev genus symbol stem (language-independent). Use this for lookups/joins, not {@link #getGenus()}.
     */
    public String getGenusSymbol() {
        return genusSymbol;
    }

    public void setGenusSymbol(String genusSymbol) {
        this.genusSymbol = genusSymbol;
    }

    /**
     * FDev species symbol stem (language-independent). Use this for lookups/joins, not {@link #getSpecies()}.
     */
    public String getSpeciesSymbol() {
        return speciesSymbol;
    }

    public void setSpeciesSymbol(String speciesSymbol) {
        this.speciesSymbol = speciesSymbol;
    }


    public double getScanLatitude() {
        return scanLatitude;
    }

    public void setScanLatitude(double scanLatitude) {
        this.scanLatitude = scanLatitude;
    }

    public double getScanLongitude() {
        return scanLongitude;
    }

    public void setScanLongitude(double scanLongitude) {
        this.scanLongitude = scanLongitude;
    }

    public double getDistanceToNextSample() {
        return distanceToNextSample;
    }

    public void setDistanceToNextSample(double distanceToNextSample) {
        this.distanceToNextSample = distanceToNextSample;
    }

    public boolean isPlayerFarEnough() {
        return playerFarEnough;
    }

    public void setPlayerFarEnough(boolean playerFarEnough) {
        this.playerFarEnough = playerFarEnough;
    }

    public boolean isBioSampleCompleted() {
        return bioSampleCompleted;
    }

    public void setBioSampleCompleted(boolean bioSampleCompleted) {
        this.bioSampleCompleted = bioSampleCompleted;
    }

    public void setSpecies(String variant) {
        this.species = variant;
    }

    public String getSpecies() {
        return species;
    }

    public long getPayout() {
        return payout;
    }

    public void setPayout(long payout) {
        this.payout = payout;
    }

    public Integer getScanXof3() {
        return scanXof3;
    }

    public void setScanXof3(Integer scanXof3) {
        this.scanXof3 = scanXof3;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        this.planetName = planetName;
    }

    public long getFistDiscoveryBonus() {
        return fistDiscoveryBonus;
    }

    public void setFistDiscoveryBonus(long fistDiscoveryBonus) {
        this.fistDiscoveryBonus = fistDiscoveryBonus;
    }

    public boolean isOurDiscovery() {
        return ourDiscovery;
    }

    public void setOurDiscovery(boolean ourDiscovery) {
        this.ourDiscovery = ourDiscovery;
    }

    public String getPlanetShortName() {
        return planetShortName;
    }

    public void setPlanetShortName(String planetShortName) {
        this.planetShortName = planetShortName;
    }

    public String getPrimaryStar() {
        return primaryStar;
    }

    public void setPrimaryStar(String primaryStar) {
        this.primaryStar = primaryStar;
    }

    public String getKey() {
        // WHY: identity is the language-independent symbol when we have it, so the same sample keys
        // the same on any client language. Legacy samples (no symbol) fall back to the localized names,
        // which preserves their original stored key so existing rows are not orphaned or duplicated.
        String g = genusSymbol != null ? genusSymbol : genus;
        String s = speciesSymbol != null ? speciesSymbol : species;
        return Md5Utils.generateMd5(bodyId + planetName + g + s);
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
