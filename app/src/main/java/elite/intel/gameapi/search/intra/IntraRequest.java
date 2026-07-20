package elite.intel.gameapi.search.intra;

import com.google.gson.annotations.SerializedName;

public class IntraRequest {
    @SerializedName("ver") private final int version = 1;
    @SerializedName("lim") private int limit = 20;
    @SerializedName("ref") private ReferenceCoordinates referenceCoordinates;
    @SerializedName("shop") private MissionProvider missionProvider;
    @SerializedName("arena") private BattleGround arena;

    public IntraRequest withResultLimit(int l) {
        limit = l;
        return this;
    }

    public IntraRequest withOurLocation(double x, double y, double z, int max) {
        this.referenceCoordinates = new ReferenceCoordinates(x, y, z, max);
        return this;
    }

    public IntraRequest withMissionProviderMinFactions(int numFactions) {
        if (missionProvider == null) missionProvider = new MissionProvider();
        missionProvider.faction.all = numFactions;
        return this;
    }

    public IntraRequest withMissionProviderHiResDistanceFromEntry(int maxLs) {
        if (missionProvider == null) missionProvider = new MissionProvider();
        missionProvider.distance.highResourceSite = maxLs;
        return this;
    }

    public IntraRequest withArenaHazResDistanceFromEntryLs(int maxLs) {
        if (arena == null) arena = new BattleGround();
        arena.distance.hazardousResourceSite = maxLs;
        return this;
    }

    static class ReferenceCoordinates {
        double x, y, z;
        int max;

        ReferenceCoordinates(double x, double y, double z, int m) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.max = m;
        }
    }

    static class MissionProvider {
        @SerializedName("fac")
        Faction faction = new Faction();
        @SerializedName("dist")
        Distance distance = new Distance();
    }

    static class BattleGround {
        @SerializedName("dist")
        Distance distance = new Distance();
    }

    static class Faction {
        @SerializedName("all") int all = 0;
    }

    static class Distance {
        @SerializedName("hi") int highResourceSite = 0;
        @SerializedName("haz") int hazardousResourceSite = 0;
    }
}