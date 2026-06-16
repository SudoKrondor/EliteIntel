package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Single modal scaffold (§7.2). Returns a wrapper JPanel for an undecorated
 * dialog's setContentPane(). Composition, not a base class.
 *
 * Assembles:
 *  - window frame: MatteBorder HUD_ORANGE_FILL_HOVER, thickness HUD_BORDER_THICKNESS_ACCENT;
 *  - header HudDialogHeader(title, onClose) when title != null;
 *  - body inside side inset HUD_DIALOG_BODY_INSET (when scrollBody, wrapped in
 *    HudScrollPane with viewport bg overridden to HUD_DIALOG_BODY);
 *  - footer: shared HudFooter (modal=true) — dismiss/BACK on the left, primary+extra on the right.
 *
 * Does NOT orchestrate showing or scrim (kept outside: runWithModalScrim(owner, showModal)).
 * The default button is set by the caller after setContentPane (the scaffold has no rootPane).
 */
public final class HudModalScaffold {

    private HudModalScaffold() {}

    public static JPanel build(HudModalSpec spec) {
        final int inset = HudPalette.HUD_DIALOG_BODY_INSET;
        final boolean hasFooter = !spec.footerButtons().isEmpty();

        // --- body ---
        Component bodyComp;
        if (spec.scrollBody()) {
            JScrollPane sp = AppTheme.hudScrollPane(spec.body());
            sp.getViewport().setBackground(HudPalette.HUD_DIALOG_BODY); // override HUD_PANEL_BG
            bodyComp = sp;
        } else {
            bodyComp = spec.body();
        }

        // content = body (+ footer) within a single side inset, warm HUD_DIALOG_BODY background
        JPanel content = new JPanel(new BorderLayout(0, HudPalette.HUD_GAP));
        content.setOpaque(true);
        content.setBackground(HudPalette.HUD_DIALOG_BODY);
        int bottom = hasFooter ? 0 : inset; // footer border carries the bottom gap; otherwise inset
        content.setBorder(new EmptyBorder(inset, inset, bottom, inset));
        content.add(bodyComp, BorderLayout.CENTER);
        if (hasFooter) content.add(buildFooter(spec), BorderLayout.SOUTH);

        // --- wrapper (window frame) ---
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(HudPalette.HUD_BG);
        wrapper.setBorder(BorderFactory.createMatteBorder(
                HudPalette.HUD_BORDER_THICKNESS_ACCENT, HudPalette.HUD_BORDER_THICKNESS_ACCENT,
                HudPalette.HUD_BORDER_THICKNESS_ACCENT, HudPalette.HUD_BORDER_THICKNESS_ACCENT,
                HudPalette.HUD_ORANGE_FILL_HOVER));

        if (spec.title() != null) {
            wrapper.add(new HudDialogHeader(spec.title(), spec.onClose()), BorderLayout.NORTH);
        }
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private static JPanel buildFooter(HudModalSpec spec) {
        // Trailing actions left-to-right: extras first (insertion order), then primary outermost right.
        List<AbstractButton> trailing = new ArrayList<>();
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.EXTRA) trailing.add(fb.button());
        }
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.PRIMARY) trailing.add(fb.button());
        }
        AbstractButton dismiss = null;
        for (HudModalSpec.FooterButton fb : spec.footerButtons()) {
            if (fb.role() == HudModalSpec.Role.DISMISS) dismiss = fb.button();
        }
        // Modal footer: BACK/dismiss on the left.
        return HudFooter.build(true, dismiss, null, trailing);
    }
}
