package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

public class LoadGameEvent extends BaseEvent {
    @SerializedName("FID")
    private String FID;

    @SerializedName("Commander")
    private String Commander;

    @SerializedName("Horizons")
    private boolean Horizons;

    @SerializedName("Odyssey")
    private boolean Odyssey;

    @SerializedName("Ship")
    private String Ship;

    @SerializedName("Ship_Localised")
    private String ShipLocalised;

    @SerializedName("ShipID")
    private int ShipID;

    @SerializedName("ShipName")
    private String ShipName;

    @SerializedName("ShipIdent")
    private String ShipIdent;

    @SerializedName("FuelLevel")
    private double FuelLevel;

    @SerializedName("FuelCapacity")
    private double FuelCapacity;

    @SerializedName("GameMode")
    private String GameMode;

    @SerializedName("Credits")
    private long Credits;

    @SerializedName("Loan")
    private long Loan;

    @SerializedName("language")
    private String language;

    @SerializedName("gameversion")
    private String gameversion;

    @SerializedName("build")
    private String build;

    public LoadGameEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "LoadGame");
        LoadGameEvent event = GsonFactory.getGson().fromJson(json, LoadGameEvent.class);
        this.FID = event.FID;
        this.Commander = event.Commander;
        this.Horizons = event.Horizons;
        this.Odyssey = event.Odyssey;
        this.Ship = event.Ship;
        this.ShipLocalised = event.ShipLocalised;
        this.ShipID = event.ShipID;
        this.ShipName = event.ShipName;
        this.ShipIdent = event.ShipIdent;
        this.FuelLevel = event.FuelLevel;
        this.FuelCapacity = event.FuelCapacity;
        this.GameMode = event.GameMode;
        this.Credits = event.Credits;
        this.Loan = event.Loan;
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
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFID() {
        return FID;
    }

    public void setFID(String FID) {
        this.FID = FID;
    }

    public String getCommander() {
        return Commander;
    }

    public void setCommander(String commander) {
        Commander = commander;
    }

    public boolean isHorizons() {
        return Horizons;
    }

    public void setHorizons(boolean horizons) {
        Horizons = horizons;
    }

    public boolean isOdyssey() {
        return Odyssey;
    }

    public void setOdyssey(boolean odyssey) {
        Odyssey = odyssey;
    }

    public String getShip() {
        return Ship;
    }

    public void setShip(String ship) {
        Ship = ship;
    }

    public String getShipLocalised() {
        return ShipLocalised;
    }

    public void setShipLocalised(String shipLocalised) {
        ShipLocalised = shipLocalised;
    }

    public int getShipID() {
        return ShipID;
    }

    public void setShipID(int shipID) {
        ShipID = shipID;
    }

    public String getShipName() {
        return ShipName;
    }

    public void setShipName(String shipName) {
        ShipName = shipName;
    }

    public String getShipIdent() {
        return ShipIdent;
    }

    public void setShipIdent(String shipIdent) {
        ShipIdent = shipIdent;
    }

    public double getFuelLevel() {
        return FuelLevel;
    }

    public void setFuelLevel(double fuelLevel) {
        FuelLevel = fuelLevel;
    }

    public double getFuelCapacity() {
        return FuelCapacity;
    }

    public void setFuelCapacity(double fuelCapacity) {
        FuelCapacity = fuelCapacity;
    }

    public String getGameMode() {
        return GameMode;
    }

    public void setGameMode(String gameMode) {
        GameMode = gameMode;
    }

    public long getCredits() {
        return Credits;
    }

    public void setCredits(long credits) {
        Credits = credits;
    }

    public long getLoan() {
        return Loan;
    }

    public void setLoan(long loan) {
        Loan = loan;
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
        return Horizons == that.Horizons &&
                Odyssey == that.Odyssey &&
                ShipID == that.ShipID &&
                Double.compare(that.FuelLevel, FuelLevel) == 0 &&
                Double.compare(that.FuelCapacity, FuelCapacity) == 0 &&
                Credits == that.Credits &&
                Loan == that.Loan &&
                Objects.equals(FID, that.FID) &&
                Objects.equals(Commander, that.Commander) &&
                Objects.equals(Ship, that.Ship) &&
                Objects.equals(ShipLocalised, that.ShipLocalised) &&
                Objects.equals(ShipName, that.ShipName) &&
                Objects.equals(ShipIdent, that.ShipIdent) &&
                Objects.equals(GameMode, that.GameMode) &&
                Objects.equals(language, that.language) &&
                Objects.equals(gameversion, that.gameversion) &&
                Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FID, Commander, Horizons, Odyssey, Ship, ShipLocalised, ShipID, ShipName, ShipIdent,
                FuelLevel, FuelCapacity, GameMode, Credits, Loan, language, gameversion, build);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoadGameEvent.class.getSimpleName() + "[", "]")
                .add("FID='" + FID + "'")
                .add("Commander='" + Commander + "'")
                .add("Horizons=" + Horizons)
                .add("Odyssey=" + Odyssey)
                .add("Ship='" + Ship + "'")
                .add("ShipLocalised='" + ShipLocalised + "'")
                .add("ShipID=" + ShipID)
                .add("ShipName='" + ShipName + "'")
                .add("ShipIdent='" + ShipIdent + "'")
                .add("FuelLevel=" + FuelLevel)
                .add("FuelCapacity=" + FuelCapacity)
                .add("GameMode='" + GameMode + "'")
                .add("Credits=" + Credits)
                .add("Loan=" + Loan)
                .add("language='" + language + "'")
                .add("gameversion='" + gameversion + "'")
                .add("build='" + build + "'")
                .toString();
    }
}