package elite.intel.ui.widget;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every text field and drop-down wears the table filter bar's frame; a combo in a grid keeps the grid's. Read back from
 * the painted pixels, because the look is what is under test.
 */
class HudFieldFrameTest {

    private static final int WIDTH = 200;
    private static final int HEIGHT = HudPalette.HUD_FIELD_HEIGHT;

    @BeforeAll
    static void lookAndFeel() {
        FlatLightLaf.setup();
    }

    @Test
    void aTextFieldAtRestHasTheTealFrameAndCyanMarks() {
        BufferedImage painted = paint(new HudTextField());

        assertColor(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, 1, 1);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, WIDTH - 2, HEIGHT - 2);
    }

    @Test
    void aDisabledTextFieldDimsItsFrameAndLosesTheMarks() {
        HudTextField field = new HudTextField();
        field.setEnabled(false);

        BufferedImage painted = paint(field);

        assertColor(HudPalette.HUD_COLOR_ROLE_DISABLED, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND, painted, 1, 1);
    }

    @Test
    void aFocusedTextFieldBrightensItsFrame() {
        HudTextField field = new HudTextField();

        BufferedImage painted = withFocusOn(field, () -> paint(field));

        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION, painted, WIDTH / 2, 0);
    }

    @Test
    void aPasswordFieldHasTheSameFrame() {
        BufferedImage painted = paint(new HudPasswordField());

        assertColor(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, 1, 1);
    }

    @Test
    void aComboKeepsItsMarksOverTheArrowCellAndTheCellReachesTheFrame() {
        BufferedImage painted = paint(new HudComboBox<>(new String[]{"value"}));

        assertColor(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, 1, 1);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, WIDTH - 2, HEIGHT - 2);
        int divider = arrowCellX(painted);
        assertColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER, painted, divider, 1);
        assertColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER, painted, divider, HEIGHT - 2);
    }

    @Test
    void aPickerKeepsItsMarksOverItsEditorAndFramesFocusOnTheEditor() {
        HudComboBox<String> picker = HudComboBox.picker(new String[]{"value"}, s -> s, (s, q) -> true);

        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, paint(picker), 1, 1);

        Component editor = picker.getEditor().getEditorComponent();
        BufferedImage focused = withFocusOn(editor, () -> paint(picker));
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION, focused, WIDTH / 2, 0);
    }

    @Test
    void aDisabledComboPaintsNoFlatLafPaleBackground() {
        HudComboBox<String> combo = new HudComboBox<>(new String[]{"value"});
        combo.setEnabled(false);

        BufferedImage painted = paint(combo);

        assertColor(HudPalette.HUD_COLOR_ROLE_DISABLED, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND, painted, WIDTH / 2, 1);
    }

    @Test
    void aComboInAGridKeepsTheWarmLineAndNoMarksOrCell() {
        HudComboBox<String> combo = new HudComboBox<>(new String[]{"value"});
        new DefaultCellEditor(combo);
        combo.setBackground(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);

        BufferedImage painted = paint(combo);

        assertColor(HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND, painted, 1, 1);
        assertColor(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND, painted, WIDTH - 2, HEIGHT - 2);
    }

    @Test
    void theListMatchesTheFieldItDropsFromAndInAGridStaysWarm() {
        HudComboBox<String> field = new HudComboBox<>(new String[]{"value"});
        HudComboBox<String> cell = new HudComboBox<>(new String[]{"value"});
        new DefaultCellEditor(cell);

        assertEquals(HudPalette.HUD_COLOR_ROLE_FIELD_BACKGROUND, opened(field).getList().getBackground());
        assertEquals(HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND, opened(cell).getList().getBackground());
    }

    @Test
    void aSpinnerHasTheFrameAndMarksOverItsStepCell() {
        BufferedImage painted = paint(new HudSpinner(100, 0, 600, 10));

        assertColor(HudPalette.HUD_COLOR_ROLE_FRAME_BORDER, painted, WIDTH / 2, 0);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, 1, 1);
        assertColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK, painted, WIDTH - 2, HEIGHT - 2);
    }

    @Test
    void aSpinnerStillTakesATypedValueAndStepsWithinItsRange() throws Exception {
        HudSpinner spinner = new HudSpinner(590, 0, 600, 10);
        JButton up = stepButton(spinner, "Spinner.nextButton");

        up.doClick();
        assertEquals(600, spinner.getValue());
        up.doClick();
        assertEquals(600, spinner.getValue());

        JFormattedTextField text = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        text.setText("250");
        text.commitEdit();
        assertEquals(250, spinner.getValue());

        stepButton(spinner, "Spinner.previousButton").doClick();
        assertEquals(240, spinner.getValue());
    }

    @Test
    void thePaletteWalkLeavesASpinnersTextWithoutAFrameOfItsOwn() {
        HudSpinner spinner = new HudSpinner(100, 0, 600, 10);
        JFormattedTextField text = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        Border before = text.getBorder();

        AppTheme.applyDarkPalette(spinner);

        assertSame(before, text.getBorder());
    }

    @Test
    void aSearchFieldInfoCellSitsBeforeTheClearCellAndRunsItsAction() {
        HudSearchField search = new HudSearchField("find", "clear", HudSearchField.Variant.TABLE_FILTER);
        int[] runs = {0};
        search.setInfoAction(() -> runs[0]++);
        search.setSize(WIDTH, HEIGHT);
        layOut(search);

        HudGlyphButton info = find(search, HudGlyphButton.class);
        JButton clear = find(search, JButton.class);
        int infoX = SwingUtilities.convertPoint(info, 0, 0, search).x;
        int clearX = SwingUtilities.convertPoint(clear, 0, 0, search).x;
        assertTrue(infoX < clearX);

        info.dispatchEvent(new MouseEvent(info, MouseEvent.MOUSE_ENTERED, 0, 0, 1, 1, 0, false));
        info.dispatchEvent(new MouseEvent(info, MouseEvent.MOUSE_PRESSED, 0, 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        info.dispatchEvent(new MouseEvent(info, MouseEvent.MOUSE_RELEASED, 0, 0, 1, 1, 1, false, MouseEvent.BUTTON1));
        assertEquals(1, runs[0]);
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T found = find(nested, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton stepButton(HudSpinner spinner, String name) {
        for (Component child : spinner.getComponents()) {
            if (name.equals(child.getName())) {
                return (JButton) child;
            }
        }
        throw new AssertionError("no " + name);
    }

    /**
     * The popup with its open-time styling applied, which needs no window on screen.
     */
    private static BasicComboPopup opened(HudComboBox<String> combo) {
        BasicComboPopup popup = (BasicComboPopup) combo.getUI().getAccessibleChild(combo, 0);
        for (javax.swing.event.PopupMenuListener listener : ((JPopupMenu) popup).getPopupMenuListeners()) {
            listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));
        }
        return popup;
    }

    /**
     * The first column from the right, inside the frame and the marks, painted in the divider's colour.
     */
    private static int arrowCellX(BufferedImage painted) {
        int rgb = HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER.getRGB();
        for (int x = WIDTH - 3; x > 0; x--) {
            if (painted.getRGB(x, HEIGHT / 2) == rgb) {
                return x;
            }
        }
        throw new AssertionError("no arrow cell divider");
    }

    private static BufferedImage paint(JComponent component) {
        component.setSize(WIDTH, HEIGHT);
        layOut(component);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            component.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layOut(nested);
            }
        }
    }

    private static BufferedImage withFocusOn(Component owner, java.util.function.Supplier<BufferedImage> painting) {
        KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultKeyboardFocusManager() {
            @Override
            public Component getFocusOwner() {
                return owner;
            }
        });
        try {
            return painting.get();
        } finally {
            KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
        }
    }

    private static void assertColor(Color expected, BufferedImage image, int x, int y) {
        assertEquals(expected.getRGB(), image.getRGB(x, y),
                () -> "pixel (" + x + "," + y + ") is #" + Integer.toHexString(image.getRGB(x, y)));
    }
}
