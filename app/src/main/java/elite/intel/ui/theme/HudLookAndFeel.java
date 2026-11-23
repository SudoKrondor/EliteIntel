package elite.intel.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.ui.widget.HudToolTipUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * The app's look and feel: FlatLaf, the app's fonts, and the UI defaults that dress the components Swing builds for
 * us in the HUD palette.
 * <p>
 * One place, because two processes need exactly the same setup: the app itself, and the colour theme preview
 * ({@code ThemePreviewRenderer}), which draws the real widgets in a second process. A difference between the two
 * would show the commander a preview the app does not draw.
 */
public final class HudLookAndFeel {

    private static final Logger log = LogManager.getLogger(HudLookAndFeel.class);

    /** Platform sans-serif for UI labels: the previous custom font did not cover Cyrillic glyphs reliably. */
    private static final String UI_FONT_FAMILY = Font.SANS_SERIF;
    private static final String MONO_FONT_RESOURCE = "/fonts/UbuntuSansMono-Regular.ttf";

    private HudLookAndFeel() {
    }

    /** Text and FlatLaf settings that must be in place before anything starts the AWT toolkit. */
    public static void prepare() {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        FlatLightLaf.setup();
    }

    /**
     * Installs the look and feel, the fonts and the HUD defaults. Call it on the event thread, after
     * {@link #prepare()} and the colour theme ({@link ActiveTheme}), before any window is built.
     *
     * @return the app's monospace font, or the platform monospace when the bundled one cannot be loaded
     */
    public static Font install() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Could not install the FlatLaf look and feel", e);
        }
        // FlatLaf paints hover/pressed over the renderer; neutralise it here.
        UIManager.put("TableHeader.hoverBackground", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TableHeader.hoverForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("TableHeader.pressedBackground", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TableHeader.pressedForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        Font mono = installFonts();
        installHudDefaults();
        return mono;
    }

    /**
     * Sets how text is anti-aliased, for a process with no desktop to ask. WHY: a headless process gets no desktop
     * text hints, so it would draw text differently from the app on screen. Passing the app's own hint makes the
     * preview match the app pixel for pixel. Call it after {@link #install()}.
     */
    public static void useTextAntialiasing(Object textAntialiasingHint) {
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasingHint);
    }

    private static Font installFonts() {
        UIManager.put("defaultFont", new FontUIResource(new Font(UI_FONT_FAMILY, Font.PLAIN, (int) HudPalette.HUD_FONT_UI_DEFAULT)));
        try (InputStream in = HudLookAndFeel.class.getResourceAsStream(MONO_FONT_RESOURCE)) {
            if (in == null) throw new IOException("Missing font resource " + MONO_FONT_RESOURCE);
            Font mono = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(HudPalette.HUD_FONT_MONO_BASE);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(mono);
            UIManager.put("monospaceFont", new FontUIResource(mono));
            return mono;
        } catch (FontFormatException | IOException e) {
            log.error("Failed to load custom font: {}", e.getMessage());
            return new Font(Font.MONOSPACED, Font.PLAIN, (int) HudPalette.HUD_FONT_MONO_BASE);
        }
    }

    private static void installHudDefaults() {
        UIManager.put("Panel.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("OptionPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TabbedPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TabbedPane.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TabbedPane.contentAreaColor", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        // The selected tab of a STOCK JTabbedPane - the ones inside components the JDK builds for us, such
        // as JColorChooser. FlatLaf paints the selected tab in TabbedPane.focusColor, a pale blue, and
        // leaves selectedForeground unset so the label falls back to the near-white foreground above:
        // white on white, and the tab a commander is looking at is the one they cannot read. The app's
        // own tabs never hit this - HudTabbedPane installs a UI that paints its tabs itself - which is
        // why it stayed invisible until a colour picker put a stock tab strip on screen.
        UIManager.put("TabbedPane.focusColor", HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        UIManager.put("TabbedPane.hoverColor", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND);
        UIManager.put("TabbedPane.selectedForeground", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("TabbedPane.underlineColor", HudPalette.HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_UNDERLINE);
        UIManager.put("TabbedPane.disabledForeground", HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("TabbedPane.disabledUnderlineColor", HudPalette.HUD_COLOR_ROLE_DISABLED);
        // Same story one control along: a TitledBorder built by the JDK - the "Preview" frame inside
        // JColorChooser is the only one in the app - takes its title from TitledBorder.titleColor, which
        // the light look-and-feel leaves black, on our dark background. The app draws its own titled
        // frames with HudTitledCard and never builds one of these, so this only ever dresses a component we
        // did not lay out ourselves.
        UIManager.put("TitledBorder.titleColor", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("TitledBorder.border", new BorderUIResource(
                new LineBorder(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, HudPalette.HUD_BORDER_THICKNESS)));
        UIManager.put("Label.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("CheckBox.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("RadioButton.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        UIManager.put("Button.foreground", HudPalette.HUD_COLOR_ROLE_BUTTON_TEXT);
        UIManager.put("Button.background", HudPalette.HUD_COLOR_ROLE_PRIMARY_BUTTON_BACKGROUND);
        UIManager.put("Button.disabledText",       HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("Button.disabledForeground", HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("ScrollPane.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("Viewport.background", HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        UIManager.put("TextField.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("PasswordField.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("TextArea.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("EditorPane.background", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("TextField.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("PasswordField.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TextArea.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("EditorPane.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("TextField.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("PasswordField.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("TextArea.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("EditorPane.inactiveForeground", HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
        UIManager.put("Table.background", HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        UIManager.put("Table.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("Table.selectionBackground", HudPalette.HUD_COLOR_ROLE_ACCENT_FILL);
        UIManager.put("Table.selectionForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
        // A JList dresses like a table: the light look-and-feel would otherwise paint it white with a blue
        // selection (the voice tuning list, the journal import sources).
        UIManager.put("List.background", HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND);
        UIManager.put("List.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("List.selectionBackground", HudPalette.HUD_COLOR_ROLE_ACCENT_FILL);
        UIManager.put("List.selectionForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
        UIManager.put("List.selectionInactiveBackground", HudPalette.HUD_COLOR_ROLE_ACCENT_FILL);
        UIManager.put("List.selectionInactiveForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);
        UIManager.put("ComboBox.background",              HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.foreground",              HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        UIManager.put("ComboBox.disabledBackground",       HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.disabledForeground",       HudPalette.HUD_COLOR_ROLE_DISABLED);
        UIManager.put("ComboBox.buttonDisabledBackground", HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
        UIManager.put("ComboBox.maximumRowCount",          8);
        // Width=0 suppresses the vertical separator between value and arrow button.
        // FlatLaf re-installs buttonSeparatorColor from the theme after per-instance nulling,
        // so zeroing the width is the only reliable suppression (update() skips it when width <= 0).
        UIManager.put("ComboBox.buttonSeparatorWidth",     0);
        // Editable combo editor calls selectAll() on setSelectedItem; override FlatLaf's
        // cold-blue theme default so selection follows the warm HUD palette.
        UIManager.put("ComboBox.selectionBackground", HudPalette.HUD_COLOR_ROLE_ACCENT_FILL);
        UIManager.put("ComboBox.selectionForeground", HudPalette.HUD_COLOR_ROLE_SELECTED_TEXT);

        // Tooltips: a flat HUD-dark chip with a warm 1px rail instead of the pale LAF default, so every
        // setToolTipText across the app matches the cockpit look (canon §13). The popup drop shadow is killed
        // app-wide (tooltips, menus, combo popups) because the HUD is shadowless by design.
        UIManager.put("ToolTip.background", HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
        UIManager.put("ToolTip.foreground", HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
        // Explicit small tooltip font: without this the tooltip inherits the 18px UI defaultFont and reads oversized.
        UIManager.put("ToolTip.font", new FontUIResource(
                new Font(UI_FONT_FAMILY, Font.PLAIN, (int) HudPalette.HUD_FONT_TOOLTIP)));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, HudPalette.HUD_BORDER_THICKNESS),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        UIManager.put("Popup.dropShadowPainted", Boolean.FALSE);
        // Repaints a tooltip once it is on screen, or a tooltip in a window of its own can show as a blank box on Linux.
        UIManager.put("ToolTipUI", HudToolTipUI.class.getName());
        // A tooltip stays while the pointer rests on its control, like the binding grid's callout: Swing's 4 seconds
        // took it away before a longer one could be read. Leaving the control, a click or a key still hides it.
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    }
}
