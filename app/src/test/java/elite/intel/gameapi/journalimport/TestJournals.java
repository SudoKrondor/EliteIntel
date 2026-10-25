package elite.intel.gameapi.journalimport;

import java.util.ArrayList;
import java.util.List;

/**
 * Journal lines for the import tests, shaped as the game writes them.
 */
final class TestJournals {

    private TestJournals() {
    }

    static String journal(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    static String header(String timestamp, String language) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"Fileheader\", \"part\":1, \"language\":\""
                + language + "\", \"Odyssey\":true, \"gameversion\":\"4.0.0.1904\", \"build\":\"r300000/r0 \" }";
    }

    static String commander(String timestamp, String fid, String name) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"Commander\", \"FID\":\"" + fid + "\", \"Name\":\""
                + name + "\" }";
    }

    /**
     * A LoadGame as journals wrote it before 2018: the commander's name, no FID.
     */
    static String legacyLoadGame(String timestamp, String name) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"LoadGame\", \"Commander\":\"" + name
                + "\", \"Ship\":\"CobraMkIII\", \"ShipID\":1 }";
    }

    static String jump(String timestamp, String system, long address, double x, long population) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"FSDJump\", \"StarSystem\":\"" + system
                + "\", \"SystemAddress\":" + address + ", \"StarPos\":[" + x + ",0.0,0.0], \"Body\":\"" + system
                + "\", \"BodyID\":0, \"BodyType\":\"Star\", \"SystemSecurity\":\"$GAlAXY_MAP_INFO_state_anarchy;\", "
                + "\"SystemSecurity_Localised\":\"Anarchy\", \"Population\":" + population
                + ", \"JumpDist\":10.0, \"FuelUsed\":1.0, \"FuelLevel\":30.0 }";
    }

    static String signal(String timestamp, long address, String signalName) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"FSSSignalDiscovered\", \"SystemAddress\":" + address
                + ", \"SignalName\":\"" + signalName + "\", \"SignalName_Localised\":\"Resource Extraction Site\" }";
    }

    static String massacre(String timestamp, long missionId, String faction, String targetSystem) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"MissionAccepted\", \"Faction\":\"" + faction
                + "\", \"Name\":\"Mission_Massacre\", \"LocalisedName\":\"Kill pirates\", \"TargetType\":"
                + "\"$MissionUtil_FactionTag_Pirate;\", \"TargetType_Localised\":\"Pirates\", \"TargetFaction\":"
                + "\"Target Cartel\", \"KillCount\":24, \"DestinationSystem\":\"" + targetSystem + "\", \"Expiry\":"
                + "\"2030-01-01T00:00:00Z\", \"Wing\":false, \"Influence\":\"++\", \"Reputation\":\"++\", \"Reward\":"
                + "5000000, \"MissionID\":" + missionId + " }";
    }

    static String scan(String timestamp, long address, String system, String body, int bodyId, boolean wasDiscovered) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"Scan\", \"ScanType\":\"Detailed\", \"BodyName\":\""
                + body + "\", \"BodyID\":" + bodyId + ", \"StarSystem\":\"" + system + "\", \"SystemAddress\":" + address
                + ", \"DistanceFromArrivalLS\":500.0, \"TidalLock\":false, \"TerraformState\":\"\", \"PlanetClass\":"
                + "\"Icy body\", \"Atmosphere\":\"\", \"Volcanism\":\"\", \"MassEM\":0.1, \"Radius\":2000000.0, "
                + "\"SurfaceGravity\":1.0, \"SurfaceTemperature\":100.0, \"Landable\":true, \"WasDiscovered\":"
                + wasDiscovered + ", \"WasMapped\":false }";
    }

    static String finalOrganicScan(String timestamp, long address, int bodyId, String species) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"ScanOrganic\", \"ScanType\":\"Analyse\", \"Genus\":"
                + "\"$Codex_Ent_Stratum_Genus_Name;\", \"Species\":\"$Codex_Ent_" + species + "_Name;\", \"SystemAddress\":"
                + address + ", \"Body\":" + bodyId + " }";
    }

    static String refined(String timestamp, String symbol, String localised) {
        return "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"MiningRefined\", \"Type\":\"$" + symbol
                + "_name;\", \"Type_Localised\":\"" + localised + "\" }";
    }

    /**
     * Lines that look like journal lines and are not: truncated, edited, not JSON.
     */
    static List<String> garbage() {
        List<String> lines = new ArrayList<>();
        lines.add("{ \"timestamp\":\"2024-01-01T00:00:00Z\", \"event\":\"MissionAccepted\", \"Faction\":");
        lines.add("this line was typed into the journal by hand");
        lines.add("{ \"event\":\"Scan\", \"BodyID\":\"not a number\" }");
        return lines;
    }
}
