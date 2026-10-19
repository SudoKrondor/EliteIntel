package elite.intel.ui.event;

import java.util.concurrent.CompletableFuture;

public class RestartMouthEvent {
    private final CompletableFuture<Boolean> completion = new CompletableFuture<>();

    /** True after the new mouth started; false when services are stopped. Failures complete exceptionally. */
    public CompletableFuture<Boolean> completion() {
        return completion;
    }
}
