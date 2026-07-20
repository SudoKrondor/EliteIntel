package elite.intel.gameapi.gamestate.dtos;

import elite.intel.gameapi.search.edsm.dto.DeathsDto;
import elite.intel.gameapi.search.edsm.dto.TrafficDto;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;

public class NavRouteDto extends BaseJsonDto implements ToJsonConvertible {

    double X;
    double Y;
    double Z;
    private int leg;
    private int remainingJumps;
    private String starClass;
    private String name;
    private boolean isScoopable;
    private DeathsDto deathData;
    private TrafficDto traffic;

    public int getLeg() {
        return leg;
    }

    public void setLeg(int leg) {
        this.leg = leg;
    }

    public String getStarClass() {
        return starClass;
    }

    public void setStarClass(String starClass) {
        this.starClass = starClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isScoopable() {
        return isScoopable;
    }

    public void setScoopable(boolean scoopable) {
        isScoopable = scoopable;
    }

    public int getRemainingJumps() {
        return remainingJumps;
    }

    public void setRemainingJumps(int remainingJumps) {
        this.remainingJumps = remainingJumps;
    }

    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    public double getX() {
        return X;
    }

    public void setX(double x) {
        X = x;
    }

    public double getY() {
        return Y;
    }

    public void setY(double y) {
        Y = y;
    }

    public double getZ() {
        return Z;
    }

    public void setZ(double z) {
        Z = z;
    }

    public DeathsDto getDeathData() {
        return deathData;
    }

    public void setDeathData(DeathsDto deathData) {
        this.deathData = deathData;
    }

    public TrafficDto getTraffic() {
        return traffic;
    }

    public void setTraffic(TrafficDto traffic) {
        this.traffic = traffic;
    }
}
