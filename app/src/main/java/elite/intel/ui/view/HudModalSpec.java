package elite.intel.ui.view;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Spec for HudModalScaffold (§7.2). The scaffold assembles the
 * frame + header + body + footer; this spec carries data only.
 * Buttons are passed READY-MADE (the caller creates them and attaches
 * listeners). The default button (getRootPane().setDefaultButton) is set
 * by the caller after setContentPane.
 */
public final class HudModalSpec {

    /** PRIMARY - main action (SAVE/RUN/...), right side, outermost.
     *  DISMISS - close without action (BACK), left side.
     *  EXTRA   - secondary actions (edit/delete), grouped with primary on the
     *            right, placed to the left of primary. */
    public enum Role { PRIMARY, DISMISS, EXTRA }

    public record FooterButton(AbstractButton button, Role role) {}

    private final String title;            // nullable -> no HudDialogHeader
    private final Runnable onClose;         // header close glyph (used when title != null)
    private final JComponent body;
    private final boolean scrollBody;
    private final List<FooterButton> footerButtons;

    private HudModalSpec(Builder b) {
        this.title = b.title;
        this.onClose = b.onClose;
        this.body = b.body;
        this.scrollBody = b.scrollBody;
        this.footerButtons = List.copyOf(b.footerButtons);
    }

    public String title() { return title; }
    public Runnable onClose() { return onClose; }
    public JComponent body() { return body; }
    public boolean scrollBody() { return scrollBody; }
    public List<FooterButton> footerButtons() { return footerButtons; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String title;
        private Runnable onClose;
        private JComponent body;
        private boolean scrollBody;
        private final List<FooterButton> footerButtons = new ArrayList<>();

        public Builder title(String title) { this.title = title; return this; }
        public Builder onClose(Runnable onClose) { this.onClose = onClose; return this; }
        public Builder body(JComponent body) { this.body = body; return this; }
        public Builder scrollBody(boolean scrollBody) { this.scrollBody = scrollBody; return this; }
        public Builder primary(AbstractButton b) { footerButtons.add(new FooterButton(b, Role.PRIMARY)); return this; }
        public Builder dismiss(AbstractButton b) { footerButtons.add(new FooterButton(b, Role.DISMISS)); return this; }
        public Builder extra(AbstractButton b)   { footerButtons.add(new FooterButton(b, Role.EXTRA));   return this; }

        public HudModalSpec build() {
            if (body == null) throw new IllegalStateException("HudModalSpec: body is required");
            return new HudModalSpec(this);
        }
    }
}
