package elite.intel.ui.starvizion.model;

public record SvButton(int index, String name) {

    @Override
    public String toString() {
        return name;
    }
}
