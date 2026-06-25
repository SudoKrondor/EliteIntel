package elite.intel.ui.widget;

import javax.swing.*;
import java.awt.*;

/**
 * A persistent, themed hover callout for the bindings editor.
 * <p>
 * A standard Swing tooltip auto-dismisses after a fraction of a second, which makes it useless for
 * reading a conflict explanation. This wraps a {@link Popup} (a real, dismiss-on-demand window) so
 * the callout stays visible the whole time the pointer rests on a conflicting row, and is hidden
 * explicitly when the pointer leaves it.
 * <p>
 * A single instance is shared across all the editor's group tables, so only one callout is ever
 * visible at a time.
 */
public final class BindingConflictPopup {

    private Popup popup;
    /**
     * Identity of the content currently shown, so re-hovering the same row does not flicker/reposition.
     */
    private Object shownKey;

    /**
     * Shows {@code content} at screen coordinates ({@code x},{@code y}), anchored to {@code owner}.
     * If the same {@code key} is already showing, this is a no-op so the callout stays put while the
     * pointer drifts within the same row.
     */
    public void show(Component owner, int x, int y, Object key, JComponent content) {
        if (popup != null && key != null && key.equals(shownKey)) {
            return;
        }
        hide();
        popup = PopupFactory.getSharedInstance().getPopup(owner, content, x, y);
        shownKey = key;
        popup.show();
    }

    /**
     * Hides any currently visible callout. Safe to call when nothing is showing.
     */
    public void hide() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
        shownKey = null;
    }
}
