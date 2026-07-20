package elite.intel.gameapi.search.intra;

import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

import java.util.List;

public class IntraResponse {

    @SerializedName("ok") int ok;
    @SerializedName("msg") String message;
    @SerializedName("ver") int version;
    @SerializedName("head") Head head;
    @SerializedName("body") List<Pair> body;

    public int getOk() {
        return ok;
    }

    public void setOk(int ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public List<Pair> getBody() {
        return body;
    }

    public void setBody(List<Pair> body) {
        this.body = body;
    }

    public static class Head {
        @SerializedName("shop") int missionProvider;
        @SerializedName("arena") int battleGround;

        public int getMissionProvider() {
            return missionProvider;
        }

        public void setMissionProvider(int missionProvider) {
            this.missionProvider = missionProvider;
        }

        public int getBattleGround() {
            return battleGround;
        }

        public void setBattleGround(int battleGround) {
            this.battleGround = battleGround;
        }
    }

    public static class Pair extends BaseJsonDto implements ToJsonConvertible {
        @SerializedName("shop") SystemInfo missionProvider;
        @SerializedName("arena") SystemInfo bettleGround;

        public SystemInfo getMissionProvider() {
            return missionProvider;
        }

        public void setMissionProvider(SystemInfo missionProvider) {
            this.missionProvider = missionProvider;
        }

        public SystemInfo getBettleGround() {
            return bettleGround;
        }

        public void setBettleGround(SystemInfo bettleGround) {
            this.bettleGround = bettleGround;
        }

        @Override public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        @Override public String toString() {
            return toJson();
        }
    }

    public static class SystemInfo {
        @SerializedName("name") String starSystemName;
        @SerializedName("addr") long eliteDangerousSystemAddress;
        @SerializedName("pop") long population;
        @SerializedName("fac") IntraRequest.Faction faction;
        @SerializedName("dist") IntraRequest.Distance distance;
        @SerializedName("haz") ResourceSite hazRes;
        @SerializedName("hi") ResourceSite hiRes;

        public String getStarSystemName() {
            return starSystemName;
        }

        public void setStarSystemName(String starSystemName) {
            this.starSystemName = starSystemName;
        }

        public long getEliteDangerousSystemAddress() {
            return eliteDangerousSystemAddress;
        }

        public void setEliteDangerousSystemAddress(long eliteDangerousSystemAddress) {
            this.eliteDangerousSystemAddress = eliteDangerousSystemAddress;
        }

        public long getPopulation() {
            return population;
        }

        public void setPopulation(long population) {
            this.population = population;
        }

        public IntraRequest.Faction getFaction() {
            return faction;
        }

        public void setFaction(IntraRequest.Faction faction) {
            this.faction = faction;
        }

        public IntraRequest.Distance getDistance() {
            return distance;
        }

        public void setDistance(IntraRequest.Distance distance) {
            this.distance = distance;
        }

        public ResourceSite getHazRes() {
            return hazRes;
        }

        public void setHazRes(ResourceSite hazRes) {
            this.hazRes = hazRes;
        }

        public ResourceSite getHiRes() {
            return hiRes;
        }

        public void setHiRes(ResourceSite hiRes) {
            this.hiRes = hiRes;
        }

        public static class ResourceSite {
            @SerializedName("r") int reported;
            @SerializedName("c") int confirmed;
            @SerializedName("d") double distanceLs;
            @SerializedName("b") String body;

            public int getReported() {
                return reported;
            }

            public void setReported(int reported) {
                this.reported = reported;
            }

            public int getConfirmed() {
                return confirmed;
            }

            public void setConfirmed(int confirmed) {
                this.confirmed = confirmed;
            }

            public double getDistanceLs() {
                return distanceLs;
            }

            public void setDistanceLs(double distanceLs) {
                this.distanceLs = distanceLs;
            }

            public String getBody() {
                return body;
            }

            public void setBody(String body) {
                this.body = body;
            }
        }
    }
}
