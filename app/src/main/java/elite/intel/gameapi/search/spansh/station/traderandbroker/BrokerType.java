package elite.intel.gameapi.search.spansh.station.traderandbroker;

public enum BrokerType {

    HUMAN("Human"), GUARDIAN("Guardian");
    private String type;

    BrokerType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
