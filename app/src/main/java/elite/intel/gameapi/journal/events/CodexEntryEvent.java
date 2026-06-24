package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.Objects;

public class CodexEntryEvent extends BaseEvent {
    @SerializedName("EntryID")
    private long entryID;

    @SerializedName("Name")
    private String name;

    @SerializedName("Name_Localised")
    private String nameLocalised;

    @SerializedName("SubCategory")
    private String subCategory;

    @SerializedName("SubCategory_Localised")
    private String subCategoryLocalised;

    @SerializedName("Category")
    private String category;

    @SerializedName("Category_Localised")
    private String categoryLocalised;

    @SerializedName("Region")
    private String region;

    @SerializedName("Region_Localised")
    private String regionLocalised;

    @SerializedName("System")
    private String system;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private Long bodyID;

    @SerializedName("Latitude")
    private double latitude;

    @SerializedName("Longitude")
    private double longitude;

    @SerializedName("IsNewEntry")
    private boolean isNewEntry;

    @SerializedName("VoucherAmount")
    private long voucherAmount;

    public CodexEntryEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CodexEntry");
        CodexEntryEvent event = GsonFactory.getGson().fromJson(json, CodexEntryEvent.class);
        this.entryID = event.entryID;
        this.name = event.name;
        this.nameLocalised = event.nameLocalised;
        this.subCategory = event.subCategory;
        this.subCategoryLocalised = event.subCategoryLocalised;
        this.category = event.category;
        this.categoryLocalised = event.categoryLocalised;
        this.region = event.region;
        this.regionLocalised = event.regionLocalised;
        this.system = event.system;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
        this.latitude = event.latitude;
        this.longitude = event.longitude;
        this.isNewEntry = event.isNewEntry;
        this.voucherAmount = event.voucherAmount;
    }

    @Override
    public String getEventType() {
        return "CodexEntry";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getEntryID() {
        return entryID;
    }

    public String getName() {
        return name;
    }

    public String getNameLocalised() {
        return nameLocalised;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getSubCategoryLocalised() {
        return subCategoryLocalised;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryLocalised() {
        return categoryLocalised;
    }

    public String getRegion() {
        return region;
    }

    public String getRegionLocalised() {
        return regionLocalised;
    }

    public String getSystem() {
        return system;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public Long getBodyID() {
        return bodyID;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isNewEntry() {
        return isNewEntry;
    }

    public long getVoucherAmount() {
        return voucherAmount;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }


    @Override public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CodexEntryEvent that = (CodexEntryEvent) o;
        return getEntryID() == that.getEntryID() && getSystemAddress() == that.getSystemAddress() && getBodyID() == that.getBodyID() && Double.compare(getLatitude(), that.getLatitude()) == 0 && Double.compare(getLongitude(), that.getLongitude()) == 0 && isNewEntry() == that.isNewEntry() && getVoucherAmount() == that.getVoucherAmount() && Objects.equals(getName(), that.getName()) && Objects.equals(getNameLocalised(), that.getNameLocalised()) && Objects.equals(getSubCategory(), that.getSubCategory()) && Objects.equals(getSubCategoryLocalised(), that.getSubCategoryLocalised()) && Objects.equals(getCategory(), that.getCategory()) && Objects.equals(getCategoryLocalised(), that.getCategoryLocalised()) && Objects.equals(getRegion(), that.getRegion()) && Objects.equals(getRegionLocalised(), that.getRegionLocalised()) && Objects.equals(getSystem(), that.getSystem());
    }

    @Override public int hashCode() {
        int result = Long.hashCode(getEntryID());
        result = 31 * result + Objects.hashCode(getName());
        result = 31 * result + Objects.hashCode(getNameLocalised());
        result = 31 * result + Objects.hashCode(getSubCategory());
        result = 31 * result + Objects.hashCode(getSubCategoryLocalised());
        result = 31 * result + Objects.hashCode(getCategory());
        result = 31 * result + Objects.hashCode(getCategoryLocalised());
        result = 31 * result + Objects.hashCode(getRegion());
        result = 31 * result + Objects.hashCode(getRegionLocalised());
        result = 31 * result + Objects.hashCode(getSystem());
        result = 31 * result + Long.hashCode(getSystemAddress());
        return result;
    }
}