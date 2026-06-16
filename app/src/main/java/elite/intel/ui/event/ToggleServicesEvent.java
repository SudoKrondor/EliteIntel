package elite.intel.ui.event;

public class ToggleServicesEvent {

    private boolean startService;

    public ToggleServicesEvent(boolean startService) {
        this.startService = startService;
    }

    public boolean isStartService() {
        return startService;
    }

}
