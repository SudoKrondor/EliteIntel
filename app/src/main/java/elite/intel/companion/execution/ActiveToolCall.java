package elite.intel.companion.execution;

/**
 * Thread-scoped correlation id for the tool-call currently being settled, so a handler-emitted announcement
 * (an {@code AiVoxResponseEvent}) can be recorded as that call's {@code tool} result rather than free-standing
 * companion speech (see COMPANION_ARCHITECTURE.md §2.8/§2.10).
 * <p>
 * The {@code GameEventBus} is synchronous, so a subscriber (the {@code CompanionAnnouncementBridge}) runs on
 * the same thread that published the event. This holder bridges only that tiny same-thread publish->subscribe
 * hop: the execution gateway sets it around a command handler (whose narration is emitted during
 * {@code handle()} on the lane thread), and {@code CommanderThought} sets it around a query's answer publish.
 * The id is then carried onward explicitly (as a method parameter), never read from a different thread.
 */
public final class ActiveToolCall {

    private static final ThreadLocal<String> ACTIVE = new ThreadLocal<>();

    private ActiveToolCall() {
    }

    /** The tool-call id active on this thread, or {@code null} when no call is being settled here. */
    public static String current() {
        return ACTIVE.get();
    }

    /**
     * Runs {@code body} with {@code toolCallId} active on this thread (restoring any previous value after),
     * so an {@code AiVoxResponseEvent} the body publishes synchronously is attributed to that call. A
     * {@code null} id runs the body with no active call (the common non-tool case).
     */
    public static void runWith(String toolCallId, Runnable body) {
        if (toolCallId == null) {
            body.run();
            return;
        }
        String previous = ACTIVE.get();
        ACTIVE.set(toolCallId);
        try {
            body.run();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }
}
