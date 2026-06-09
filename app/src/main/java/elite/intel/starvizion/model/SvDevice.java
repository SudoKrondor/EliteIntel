package elite.intel.starvizion.model;

public record SvDevice(int id, String name, int axisCount, int buttonCount) {

    @Override
    public String toString() {
        return name;
    }
}
