package elite.intel.ai.hands;

public final class BindingGroupClassifier {

    private BindingGroupClassifier() {
    }

    public static BindingGroup classify(String bindingId) {
        if (bindingId == null || bindingId.isBlank()) return BindingGroup.MISCELLANEOUS;

        // Elite Dangerous .binds files do not provide categories; context-specific suffixes are most useful first.
        if (containsAny(bindingId, "_Humanoid", "Humanoid")) return BindingGroup.ON_FOOT;
        if (containsAny(bindingId, "_Buggy", "Buggy")) return BindingGroup.SRV;
        if (containsAny(bindingId, "GalaxyMap", "SystemMap")) return BindingGroup.MAPS;
        if (containsAny(bindingId, "Exploration", "FSS", "SAA")) return BindingGroup.EXPLORATION;
        if (containsAny(bindingId, "Cam", "Camera")) return BindingGroup.CAMERA;
        if (containsAny(bindingId, "Panel", "UI_", "CycleNextPanel", "CyclePreviousPanel", "CycleNextPage", "CyclePreviousPage")) {
            return BindingGroup.UI_PANELS;
        }
        if (containsAny(bindingId, "Fire", "Hardpoint", "Target", "HeatSink", "Shield", "Chaff", "Subsystem")) {
            return BindingGroup.COMBAT;
        }
        if (containsAny(bindingId, "ForwardKey", "BackwardKey", "Yaw", "Pitch", "Roll", "Thrust", "Supercruise",
                "Hyperspace", "LandingGear", "CargoScoop", "PowerDistribution", "Speed")) {
            return BindingGroup.SHIP_FLIGHT;
        }

        return BindingGroup.MISCELLANEOUS;
    }

    private static boolean containsAny(String value, String... patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) return true;
        }
        return false;
    }
}
