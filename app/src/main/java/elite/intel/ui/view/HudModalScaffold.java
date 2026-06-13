package elite.intel.ui.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * Single modal scaffold (§7.2). Returns a wrapper JPanel for an undecorated
 * dialog's setContentPane(). Composition, not a base class.
 *
 * Assembles:
 *  - window frame: MatteBorder HUD_ORANGE_FILL_HOVER, thickness HUD_BORDER_THICKNESS_ACCENT;
 *  - header HudDialogHeader(title, onClose) when title != null;
 *  - body inside side inset HUD_DIALOG_BODY_INSET (when scrollBody, wrapped in
 *    HudScrollPane with viewport bg overridden to HUD_DIALOG_BODY);
 *  - footer: dismiss on the left, primary+extra on the right, rule hudModalFooterBorder().
 *
 * Does NOT orchestrate showing or scrim (kept outside: runWithModalScrim(owner, showModal)).
 * The default button is set by the caller after setContentPane (the scaffold has no rootPane).
 */
public final class HudModalScaffold {

    private HudModalScaffold() {}

    public static JPanel build(HudModalSpec spec) {
        final int inset = AppTheme.HUD_DIALOG_BODY_INSET;
        final boolean hasFooter = !spec.footerButtons().isEmpty();

        // --- body ---
        Component bodyComp;
        if (spec.scrollBody()) {
            JScrollPane sp = AppTheme.hudScrollPane(spec.body());
            sp.getViewport().setBackground(AppTheme.HUD_DIALOG_BODY); // override HUD_PANEL_BG
            bodyComp = sp;
        } else {
            bodyComp = spec.body();
        }

        // content = body (+ footer) within a single side inset, warm HUD_DIALOG_BODY background
        JPanel content = new JPanel(new BorderLayout(0, AppTheme.HUD_GAP));
        content.setOpaque(true);
        content.setBackground(AppTheme.HUD_DIALOG_BODY);
        int bottom = hasFooter ? 0 : inset; // footer border carries the bottom gap; otherwise inset
        content.setBorder(new EmptyBorder(inset, inset, bottom, inset));
        content.add(bodyComp, BorderLayout.CENTER);
        if (hasFooter) content.add(buildFooter(spec), BorderLayout.SOUTH);

        // --- wrapper (window frame) ---
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(AppTheme.HUD_BG);
        wrapper.setBorder(BorderFactory.createMatteBorder(
                AppTheme.HUD_BORDER_THICKNESS_ACCENT, AppTheme.HUD_BORDER_THICKNESS_ACCENT,
                AppTheme.HUD_BORDER_THICKNESS_ACCENT, AppTheme.HUD_BORDER_THICKNESS_ACCENT,
                AppTheme.HUD_ORANGE_FILL_HOVER));

        if (spec.title() != null) {
            wrapper.add(new HudDialogHeader(spec.title(), spec.onClose()), BorderLayout.NORTH);
        }
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private static JPanel buildFooter(HudModalSpec spec) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(AppTheme.hudModalFooterBorder());

        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.HUD_GAP, 0));
        west.setOpaque(false);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.HUD_GAP, 0));
        east.setOpaque(false);

        // EAST: extra buttons first (in insertion order), then primary - primary is outermost right
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.EXTRA) east.add(fb.button());
        }
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.PRIMARY) east.add(fb.button());
        }
        // WEST: dismiss
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.DISMISS) west.add(fb.button());
        }

        footer.add(west, BorderLayout.WEST);
        footer.add(east, BorderLayout.EAST);
        return footer;
    }
}
