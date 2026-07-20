package elite.intel.gameapi.search.spansh.findcarrier;

public enum CarrierAccess {
    ALL("All"), FRIEND("Friends"),NONE("None"), SQUADRON("Squadron"), SQUADRON_FRIENDS("Squadron Friends");

    private String type;

    CarrierAccess(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
