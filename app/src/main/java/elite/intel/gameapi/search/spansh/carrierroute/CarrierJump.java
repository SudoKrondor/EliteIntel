package elite.intel.gameapi.search.spansh.carrierroute;

import elite.intel.gameapi.gamestate.dtos.BaseJsonDto;
import elite.intel.util.json.ToJsonConvertible; /**
 * DTO for a carrier jump, extending BaseJsonDto for JSON persistence.
 */
public class CarrierJump extends BaseJsonDto implements ToJsonConvertible {
    private int leg;
    private String systemName;
    private double distance;
    private int fuelUsed;
    private int remainingFuel;
    private boolean hasIcyRing;
    private boolean isPristine;
    private double x;
    private double y;
    private double z;

    // Getters and setters
    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getFuelUsed() {
        return fuelUsed;
    }

    public void setFuelUsed(int fuelUsed) {
        this.fuelUsed = fuelUsed;
    }

    public int getRemainingFuel() {
        return remainingFuel;
    }

    public void setRemainingFuel(int remainingFuel) {
        this.remainingFuel = remainingFuel;
    }

    public boolean getHasIcyRing() {
        return hasIcyRing;
    }

    public void setHasIcyRing(boolean hasIcyRing) {
        this.hasIcyRing = hasIcyRing;
    }

    public boolean isPristine() {
        return isPristine;
    }

    public void setPristine(boolean isPristine) {
        this.isPristine = isPristine;
    }

    public int getLeg() {
        return leg;
    }

    public void setLeg(int leg) {
        this.leg = leg;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
