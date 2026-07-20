package elite.intel.gameapi.search.edsm.dto;

import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class EncodedMaterialsDto implements ToJsonConvertible {

    @SerializedName("data")
    private List<EncodedMaterialsDto.EncodedMaterialEntry> encoded;


    public List<EncodedMaterialsDto.EncodedMaterialEntry> getEncoded() {
        return encoded;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }


    public static class EncodedMaterialEntry implements ToJsonConvertible {

        @SerializedName("name")
        private String materialName;

        @SerializedName("qty")
        private int quantity;

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}
