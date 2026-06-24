package elite.intel.ai.hands;

public enum BindingGroup {
    SHIP_FLIGHT("bindings.group.shipFlight"),
    COMBAT("bindings.group.combat"),
    UI_PANELS("bindings.group.uiPanels"),
    MAPS("bindings.group.maps"),
    EXPLORATION("bindings.group.exploration"),
    CAMERA("bindings.group.camera"),
    SRV("bindings.group.srv"),
    ON_FOOT("bindings.group.onFoot"),
    MISCELLANEOUS("bindings.group.miscellaneous");

    private final String labelKey;

    BindingGroup(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelKey() {
        return labelKey;
    }
}
