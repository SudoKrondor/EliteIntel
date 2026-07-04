package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class LoadGameEvent extends BaseEvent {
    @SerializedName("FID")
    private String fid;

    @SerializedName("Commander")
    private String commander;

    @SerializedName("Horizons")
    private boolean horizons;

    @SerializedName("Odyssey")
    private boolean odyssey;

    @SerializedName("Ship")
    private String ship;

    @SerializedName("Ship_Localised")
    private String shipLocalised;

    @SerializedName("ShipID")
    private int shipID;

    @SerializedName("ShipName")
    private String shipName;

    @SerializedName("ShipIdent")
    private String shipIdent;

    @SerializedName("FuelLevel")
    private double fuelLevel;

    @SerializedName("FuelCapacity")
    private double fuelCapacity;

    @SerializedName("GameMode")
    private String gameMode;

    @SerializedName("Credits")
    private long credits;

    @SerializedName("Loan")
    private long loan;

    @SerializedName("language")
    private String language;

    @SerializedName("gameversion")
    private String gameversion;

    @SerializedName("build")
    private String build;

    public LoadGameEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "LoadGame");
        LoadGameEvent event = GsonFactory.getGson().fromJson(json, LoadGameEvent.class);
        this.fid = event.fid;
        this.commander = event.commander;
        this.horizons = event.horizons;
        this.odyssey = event.odyssey;
        this.ship = event.ship;
        this.shipLocalised = event.shipLocalised;
        this.shipID = event.shipID;
        this.shipName = event.shipName;
        this.shipIdent = event.shipIdent;
        this.fuelLevel = event.fuelLevel;
        this.fuelCapacity = event.fuelCapacity;
        this.gameMode = event.gameMode;
        this.credits = event.credits;
        this.loan = event.loan;
        this.language = event.language;
        this.gameversion = event.gameversion;
        this.build = event.build;
    }

    @Override
    public String getEventType() {
        return "LoadGame";
    }

    /** Session load snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Game session loaded; carries the commander, current ship, and credit balance. Background, fires at startup.";
    }

    @Override
    public String memorySummary() {
        String ship = shipLocalised != null && !shipLocalised.isBlank() ? shipLocalised : this.ship;
        return ship == null || ship.isBlank() ? "" : "started the session flying the " + ship;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFID() {
        return fid;
    }

    public void setFID(String fid) {
        this.fid = fid;
    }

    public String getCommander() {
        return commander;
    }

    public void setCommander(String commander) {
        this.commander = commander;
    }

    public boolean isHorizons() {
        return horizons;
    }

    public void setHorizons(boolean horizons) {
        this.horizons = horizons;
    }

    public boolean isOdyssey() {
        return odyssey;
    }

    public void setOdyssey(boolean odyssey) {
        this.odyssey = odyssey;
    }

    public String getShip() {
        return ship;
    }

    public void setShip(String ship) {
        this.ship = ship;
    }

    public String getShipLocalised() {
        return shipLocalised;
    }

    public void setShipLocalised(String shipLocalised) {
        this.shipLocalised = shipLocalised;
    }

    public int getShipID() {
        return shipID;
    }

    public void setShipID(int shipID) {
        this.shipID = shipID;
    }

    public String getShipName() {
        return shipName;
    }

    public void setShipName(String shipName) {
        this.shipName = shipName;
    }

    public String getShipIdent() {
        return shipIdent;
    }

    public void setShipIdent(String shipIdent) {
        this.shipIdent = shipIdent;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(double fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public double getFuelCapacity() {
        return fuelCapacity;
    }

    public void setFuelCapacity(double fuelCapacity) {
        this.fuelCapacity = fuelCapacity;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public long getCredits() {
        return credits;
    }

    public void setCredits(long credits) {
        this.credits = credits;
    }

    public long getLoan() {
        return loan;
    }

    public void setLoan(long loan) {
        this.loan = loan;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGameversion() {
        return gameversion;
    }

    public void setGameversion(String gameversion) {
        this.gameversion = gameversion;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadGameEvent that = (LoadGameEvent) o;
        return horizons == that.horizons &&
                odyssey == that.odyssey &&
                shipID == that.shipID &&
                Double.compare(that.fuelLevel, fuelLevel) == 0 &&
                Double.compare(that.fuelCapacity, fuelCapacity) == 0 &&
                credits == that.credits &&
                loan == that.loan &&
                Objects.equals(fid, that.fid) &&
                Objects.equals(commander, that.commander) &&
                Objects.equals(ship, that.ship) &&
                Objects.equals(shipLocalised, that.shipLocalised) &&
                Objects.equals(shipName, that.shipName) &&
                Objects.equals(shipIdent, that.shipIdent) &&
                Objects.equals(gameMode, that.gameMode) &&
                Objects.equals(language, that.language) &&
                Objects.equals(gameversion, that.gameversion) &&
                Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fid, commander, horizons, odyssey, ship, shipLocalised, shipID, shipName, shipIdent,
                fuelLevel, fuelCapacity, gameMode, credits, loan, language, gameversion, build);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoadGameEvent.class.getSimpleName() + "[", "]")
                .add("FID='" + fid + "'")
                .add("Commander='" + commander + "'")
                .add("Horizons=" + horizons)
                .add("Odyssey=" + odyssey)
                .add("Ship='" + ship + "'")
                .add("shipLocalised='" + shipLocalised + "'")
                .add("ShipID=" + shipID)
                .add("ShipName='" + shipName + "'")
                .add("ShipIdent='" + shipIdent + "'")
                .add("FuelLevel=" + fuelLevel)
                .add("FuelCapacity=" + fuelCapacity)
                .add("GameMode='" + gameMode + "'")
                .add("Credits=" + credits)
                .add("Loan=" + loan)
                .add("language='" + language + "'")
                .add("gameversion='" + gameversion + "'")
                .add("build='" + build + "'")
                .toString();
    }
}
