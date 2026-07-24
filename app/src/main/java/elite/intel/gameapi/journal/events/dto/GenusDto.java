package elite.intel.gameapi.journal.events.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;

public class GenusDto extends BaseJsonDto {

    private String planetName;
    /**
     * Localized genus display name (for speech/LLM), NOT a species. Historically mis-stored under the JSON key
     * "species"; {@code alternate} keeps existing persisted location blobs readable after the rename.
     */
    @SerializedName(value = "genusLocalised", alternate = {"species"})
    private String genusLocalised;
    /**
     * Language-independent FDev genus symbol stem (e.g. "Tussocks"); used for joins, not {@link #getGenusLocalised()}.
     */
    private String genusSymbol;
    private String variant;
    private long rewardInCredits;
    private long bonusCreditsForFirstDiscovery;


    /**
     * Localized genus display name (for speech/LLM). Despite the historical field name, this is a genus, not a species.
     */
    public String getGenusLocalised() {
        return genusLocalised;
    }

    public void setGenusLocalised(String genusLocalised) {
        this.genusLocalised = genusLocalised;
    }

    public String getVarient() {
        return variant;
    }

    public void setVarient(String varient) {
        this.variant = varient;
    }

    public long getRewardInCredits() {
        return rewardInCredits;
    }

    public void setRewardInCredits(long rewardInCredits) {
        this.rewardInCredits = rewardInCredits;
    }

    public long getBonusCreditsForFirstDiscovery() {
        return bonusCreditsForFirstDiscovery;
    }

    public void setBonusCreditsForFirstDiscovery(long bonusCreditsForFirstDiscovery) {
        this.bonusCreditsForFirstDiscovery = bonusCreditsForFirstDiscovery;
    }

    /**
     * FDev genus symbol stem (language-independent). Use this for joins, not {@link #getGenusLocalised()}.
     */
    public String getGenusSymbol() {
        return genusSymbol;
    }

    public void setGenusSymbol(String genusSymbol) {
        this.genusSymbol = genusSymbol;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        this.planetName = planetName;
    }
}
