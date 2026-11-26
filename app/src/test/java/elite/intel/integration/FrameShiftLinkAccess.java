package elite.intel.integration;

import java.time.Duration;

/** Gives tests in other packages a fresh link, never the app's singleton. */
public final class FrameShiftLinkAccess {

    private FrameShiftLinkAccess() {
    }

    public static FrameShiftLink newLink() {
        return new FrameShiftLink(Duration.ofMillis(200), text -> { }, System::nanoTime);
    }
}
