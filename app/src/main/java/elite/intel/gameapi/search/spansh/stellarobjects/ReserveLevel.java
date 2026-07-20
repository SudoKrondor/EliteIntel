package elite.intel.gameapi.search.spansh.stellarobjects;

public enum ReserveLevel {

    PRISTINE("Pristine"), COMMON("Common"), LOW("Low"), MAJOR("Major"), DEPLETED("Depleted");

    private String type;

    ReserveLevel(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
