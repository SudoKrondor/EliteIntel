package elite.intel.ui.event;

/**
 * UI-layer signal that the commander's personal credit balance changed.
 * Published on UiBus by {@code FinanceSubscriber} (which owns the credit math);
 * carries the resulting balance so the UI can render directly without calling
 * back into the session on the EDT.
 */
public class CreditsUpdatedEvent {

    private final long newBalance;

    public CreditsUpdatedEvent(long newBalance) {
        this.newBalance = newBalance;
    }

    public long getNewBalance() {
        return newBalance;
    }
}
