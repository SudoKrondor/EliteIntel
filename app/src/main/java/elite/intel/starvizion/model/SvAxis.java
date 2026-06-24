package elite.intel.starvizion.model;

public record SvAxis(int index, String name) {

    @Override
    public String toString() {
        return name;
    }
}
