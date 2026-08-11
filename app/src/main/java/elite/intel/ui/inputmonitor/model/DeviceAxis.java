package elite.intel.ui.inputmonitor.model;

public record DeviceAxis(int index, String name) {

    @Override
    public String toString() {
        return name;
    }
}
