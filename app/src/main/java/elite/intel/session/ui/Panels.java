package elite.intel.session.ui;

import elite.intel.session.StatusFlags;

public enum Panels {

    NAV_PANEL(StatusFlags.GuiFocus.EXTERNAL_PANEL, RightPanel.class),
    COMMS_PANEL(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.class),
    ROLE_PANEL(StatusFlags.GuiFocus.CENTRAL_PANEL, CenterPanel.class),
    INTERNAL_PANEL(StatusFlags.GuiFocus.INTERNAL_PANEL, LeftPanel.class);

    private final StatusFlags.GuiFocus id;
    private final Class<? extends PanelTab> commsPanelClass;

    Panels(StatusFlags.GuiFocus id, Class<? extends PanelTab> commsPanelClass) {
        this.id = id;
        this.commsPanelClass = commsPanelClass;
    }

    public Class<? extends PanelTab> getCommsPanelClass() {
        return commsPanelClass;
    }

    public StatusFlags.GuiFocus getId() {
        return id;
    }

    public static Panels getPanel(StatusFlags.GuiFocus id) {
        for (Panels panel : Panels.values()) {
            if (panel.getId() == id) {
                return panel;
            }
        }
        return null;
    }
}


