package elite.intel.util;

public class PlayBeepEvent {

    private final String soundFile;

    public PlayBeepEvent(String soundFile) {
        this.soundFile = soundFile;
    }

    public String getSoundFile() {
        return soundFile;
    }
}