package elite.intel.ai.brain;

/**
 * This enum defines different cadences that an AI system can adopt.
 * Cadence specifies linguistic preferences, focusing on regional variations such as British or American English.
 */
public enum ShipCadence {
    IMPERIAL(" IF language is English: Use British Cadence. Avoid American slang like dude. ELSE use cadence of language requested"),
    FEDERATION(" IF language is English:Use American Cadence. Avoid British slang like bloody.  ELSE use cadence of language requested"),
    ALLIANCE(" IF language is English: Mix British and American cadence.  ELSE use cadence of language requested");

    private final String cadenceClause;

    ShipCadence(String cadenceClause) {
        this.cadenceClause = cadenceClause;
    }

    public String getCadenceClause() {
        return cadenceClause;
    }
}