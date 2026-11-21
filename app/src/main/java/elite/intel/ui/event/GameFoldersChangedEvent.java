package elite.intel.ui.event;

/**
 * The installation being played changed (linux-several-installations.md, rules 2-3): Elite-Intel's bindings and journal
 * folders now point at another installation's. What reads them restarts, and the screens that show them read them
 * again. Published on {@code UiBus}, on whichever thread made the change.
 */
public record GameFoldersChangedEvent() {
}
