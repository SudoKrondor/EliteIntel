package elite.intel.ui.starvizion.model;

public record SvAxis(int index, String name) {

    @Override
    public String toString() {
        return name;
    }
}
