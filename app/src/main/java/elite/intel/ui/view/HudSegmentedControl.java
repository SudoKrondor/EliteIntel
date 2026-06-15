package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * HUD-styled segmented selector for a mutually-exclusive choice (radio-group semantics)
 * rendered as a single cockpit bar: equal-width text segments divided by a dark
 * {@code HUD_BG} gap, with the active segment carried by an inverted fill (§0.4/§11).
 * <p>
 * The canonical replacement for round LAF {@code JRadioButton}s: selection is shown by
 * a solid {@code ACCENT} fill with dark {@code SEL_FG} text — never a circular pill.
 * Colours mirror {@link HudCheckBox} (§5.2) so the controls read as one family. No LAF
 * rendering is used; {@code super.paintComponent} is not called.
 * <p>
 * Like an {@code ActionListener}, registered {@link ChangeListener}s fire only on user
 * interaction (a click on a segment), never from {@link #setSelectedIndex(int)} — so
 * programmatic state restoration does not re-trigger side effects.
 */
public class HudSegmentedControl extends JComponent {

    private final String[] labels;
    private int selectedIndex;
    /** Segment under the pointer, or -1 when the pointer is outside any segment. */
    private int hoverIndex = -1;

    /**
     * Creates a segmented control.
     *
     * @param labels        segment captions (rendered in upper case); at least one
     * @param selectedIndex initially selected segment index
     */
    public HudSegmentedControl(String[] labels, int selectedIndex) {
        if (labels == null || labels.length == 0) {
            throw new IllegalArgumentException("labels must be non-empty");
        }
        this.labels = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            this.labels[i] = labels[i] != null ? labels[i].toUpperCase() : "";
        }
        this.selectedIndex = clamp(selectedIndex);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        installMouse();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns the currently selected segment index. */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Selects a segment programmatically. Does NOT fire {@link ChangeListener}s
     * (mirrors {@code AbstractButton.setSelected}); use for state restoration.
     */
    public void setSelectedIndex(int index) {
        int next = clamp(index);
        if (next != selectedIndex) {
            selectedIndex = next;
            repaint();
        }
    }

    /** Registers a listener fired when the user selects a different segment. */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    // -------------------------------------------------------------------------
    // Size
    // -------------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        Font f = baseFont();
        FontMetrics fm = getFontMetrics(f);
        int maxLabel = 0;
        for (String label : labels) {
            maxLabel = Math.max(maxLabel, fm != null ? fm.stringWidth(label) : 80);
        }
        int segW = maxLabel + 2 * AppTheme.HUD_PADDING;
        int n = labels.length;
        int total = n * segW + (n - 1) * AppTheme.HUD_SEP_W;
        return new Dimension(total, AppTheme.HUD_TABLE_ROW_HEIGHT_COMPACT);
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();

            // Gaps between segments read as HUD_BG, like the checkbox marker/text divider (§5.2).
            g2.setColor(AppTheme.HUD_BG);
            g2.fillRect(0, 0, w, h);

            Font f = baseFont();
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();

            for (int i = 0; i < labels.length; i++) {
                int sx = segmentStart(i, w);
                int ex = segmentEnd(i, w);
                int sw = ex - sx;
                boolean on = i == selectedIndex;

                Color fill;
                Color textColor;
                if (!enabled) {
                    fill = AppTheme.HUD_TABLE_ROW_HOVER;
                    textColor = AppTheme.HUD_DISABLED;
                } else if (on) {
                    fill = AppTheme.ACCENT;
                    textColor = AppTheme.SEL_FG;
                } else {
                    fill = AppTheme.HUD_TABLE_ROW_HOVER;
                    // Hover on an unselected segment brightens its text to ACCENT.
                    textColor = (i == hoverIndex) ? AppTheme.ACCENT : AppTheme.FG_MUTED;
                }

                g2.setColor(fill);
                g2.fillRect(sx, 0, sw, h);

                g2.setColor(textColor);
                int textW = fm.stringWidth(labels[i]);
                int tx = sx + (sw - textW) / 2;
                g2.drawString(labels[i], tx, baseline);
            }
        } finally {
            g2.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Geometry — equal-width segments separated by HUD_SEP_W gaps
    // -------------------------------------------------------------------------

    private int segmentStart(int i, int w) {
        int n = labels.length;
        double slot = (w - (n - 1) * AppTheme.HUD_SEP_W) / (double) n;
        return (int) Math.round(i * (slot + AppTheme.HUD_SEP_W));
    }

    private int segmentEnd(int i, int w) {
        int n = labels.length;
        double slot = (w - (n - 1) * AppTheme.HUD_SEP_W) / (double) n;
        return (int) Math.round(i * (slot + AppTheme.HUD_SEP_W) + slot);
    }

    /** Returns the segment index containing {@code mouseX}, or -1 if within a gap. */
    private int segmentAt(int mouseX) {
        int w = getWidth();
        for (int i = 0; i < labels.length; i++) {
            if (mouseX >= segmentStart(i, w) && mouseX < segmentEnd(i, w)) {
                return i;
            }
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Mouse + events
    // -------------------------------------------------------------------------

    private void installMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Select on press, not click: a click is suppressed if the pointer drifts
                // a few pixels between press and release.
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) return;
                int idx = segmentAt(e.getX());
                if (idx >= 0 && idx != selectedIndex) {
                    selectedIndex = idx;
                    repaint();
                    fireStateChanged();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(isEnabled() ? segmentAt(e.getX()) : -1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHover(-1);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void updateHover(int idx) {
        if (idx != hoverIndex) {
            hoverIndex = idx;
            repaint();
        }
    }

    private void fireStateChanged() {
        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
            l.stateChanged(evt);
        }
    }

    private int clamp(int index) {
        return Math.max(0, Math.min(labels.length - 1, index));
    }

    /**
     * Bold checkbox-role font, null-safe: as a bare {@link JComponent} this control has no UI,
     * so {@link #getFont()} can be null before it is added to a hierarchy (e.g. when a layout
     * helper queries {@link #getPreferredSize()} at build time).
     */
    private Font baseFont() {
        Font f = getFont();
        if (f == null) f = UIManager.getFont("Label.font");
        if (f == null) f = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return f.deriveFont(Font.BOLD, AppTheme.HUD_FONT_CHECKBOX);
    }
}
