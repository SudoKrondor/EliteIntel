package elite.intel.gameapi.search.edsm.dto;

public enum MaterialsType {
    EDMS_MATERIAL("materials"), EDMS_ENCODED("data"),
    GAME_RAW("Raw"),
    GAME_MANUFACTURED("Manufactured"),
    GAME_ENCODED("Encoded"),
    GAME_UNKNOWN("Unknown")
    ;

    private String type;

    MaterialsType(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }
}
