package elite.intel.ui.inputmonitor.model;

public record DeviceButton(int index, String name) {

    @Override
    public String toString() {
        return name;
    }
}
