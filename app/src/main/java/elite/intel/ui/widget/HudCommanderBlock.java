package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * Central commander identity block for the AI screen's SHORTCUTS sidebar.
 * Displays the muted Elite Intel logo, UTC clock, galactic date (year + 1286),
 * and CMDR credit balance (hidden when zero).
 *
 * <p>Clock updates via {@link #tickClock()}, called from the parent panel's shared
 * 1-second timer - this component does not own a timer.
 * Credits update via {@link #setCredits(long)} on LoadGame events.
 */
public class HudCommanderBlock extends JPanel {

    private final JLabel timeLabel;
    private final JLabel dateLabel;
    private final JLabel creditsLabel;

    private static final DateTimeFormatter TIME_FMT           = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_DAY_MONTH_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.US);

    public HudCommanderBlock(Font monoFont) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // --- muted logo ---
        ImageIcon rawIcon = loadLogoSafe(72);
        JLabel logoLabel = new JLabel(rawIcon != null ? HudGlyphs.dimIcon(rawIcon, 0.45f) : null);
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(logoLabel);

        add(Box.createVerticalStrut(HUD_GAP / 2));

        // --- app name ---
        JLabel appNameLabel = new JLabel(getText("ai.commander.appName"));
        appNameLabel.setFont(appNameLabel.getFont().deriveFont(Font.BOLD, HUD_FONT_READOUT_KEY));
        appNameLabel.setForeground(HUD_COLOR_ROLE_SECONDARY_TEXT);
        appNameLabel.setAlignmentX(CENTER_ALIGNMENT);
        appNameLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        add(appNameLabel);

        add(Box.createVerticalStrut(HUD_GAP));

        // --- UTC time (large mono) ---
        timeLabel = new JLabel("--:--:--");
        timeLabel.setFont(monoFont.deriveFont(Font.BOLD, HUD_FONT_CLOCK));
        timeLabel.setForeground(HUD_COLOR_ROLE_PRIMARY_ACTION);
        timeLabel.setAlignmentX(CENTER_ALIGNMENT);
        timeLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        add(timeLabel);

        add(Box.createVerticalStrut(HUD_GAP / 2));

        // --- galactic date ---
        dateLabel = new JLabel("-- --- ----");
        dateLabel.setFont(monoFont.deriveFont(Font.PLAIN, HUD_FONT_READOUT_VALUE));
        dateLabel.setForeground(HUD_COLOR_ROLE_SECONDARY_TEXT);
        dateLabel.setAlignmentX(CENTER_ALIGNMENT);
        dateLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        add(dateLabel);

        add(Box.createVerticalStrut(HUD_GAP / 2));

        // --- credit balance (hidden when zero) ---
        creditsLabel = new JLabel();
        creditsLabel.setFont(monoFont.deriveFont(Font.BOLD, HUD_FONT_READOUT_VALUE));
        creditsLabel.setForeground(HUD_COLOR_ROLE_CREDITS_TEXT);
        creditsLabel.setAlignmentX(CENTER_ALIGNMENT);
        creditsLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        creditsLabel.setVisible(false);
        add(creditsLabel);
    }

    /**
     * Updates the UTC time and galactic date labels. Must be called on the EDT.
     */
    public void tickClock() {
        ZonedDateTime z = Instant.now().atZone(ZoneOffset.UTC);
        timeLabel.setText(TIME_FMT.format(z));
        int galacticYear = z.getYear() + 1286;
        dateLabel.setText(DATE_DAY_MONTH_FMT.format(z).toUpperCase() + " " + galacticYear);
    }

    /**
     * Shows the credit balance when positive; hides the label otherwise.
     * Safe to call on the EDT at any time.
     */
    public void setCredits(long credits) {
        if (credits > 0) {
            String amount = NumberFormat.getNumberInstance(Locale.US).format(credits);
            creditsLabel.setText(amount + " " + getText("ai.commander.creditsSuffix"));
            creditsLabel.setVisible(true);
        } else {
            creditsLabel.setVisible(false);
        }
    }

    private static ImageIcon loadLogoSafe(int size) {
        try {
            return HudGlyphs.scaledIcon(HudCommanderBlock.class, "/images/elite-logo.png", size);
        } catch (Exception e) {
            return null;
        }
    }

}
