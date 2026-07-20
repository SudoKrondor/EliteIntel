package elite.intel.gameapi.search.spansh.station.traderandbroker;

public enum TraderType {

    ENCODED("Encoded"), RAW("Raw"), MANUFACTURED("Manufactured");

    private String type;

    TraderType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
