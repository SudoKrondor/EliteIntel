package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.BindsValues;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.GameKeyboardLayout;
import elite.intel.bindforge.storage.EditedDeviceLabels;
import elite.intel.ui.support.BindingsGroupTableFactory.RowAction;
import elite.intel.ui.support.BindingsGroupTableFactory.RowActionCell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One grid holds every control. The section tabs share a tab row with modes that are not sections, so choosing one of
 * those leaves the section as it was, and each row offers the action that fits what is bound on it.
 */
class GameModePanelTest {

    private static final int ALL = 0;
    private static final int SHIP = 2;
    private static final int ON_FOOT = 4;
    private static final int ANOMALIES = 5;

    private static final String KEYBOARD = "SetSpeedZero";
    private static final String STICK_ONLY = "LandingGearToggle";
    private static final String NOTHING = "HumanoidJumpButton";

    private static final byte[] BINDS = """
            <Root>
                <SetSpeedZero>
                    <Primary Device="Keyboard" Key="Key_X" />
                    <Secondary Device="{NoDevice}" Key="" />
                </SetSpeedZero>
                <LandingGearToggle>
                    <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                    <Secondary Device="{NoDevice}" Key="" />
                </LandingGearToggle>
                <HumanoidJumpButton>
                    <Primary Device="{NoDevice}" Key="" />
                    <Secondary Device="{NoDevice}" Key="" />
                </HumanoidJumpButton>
            </Root>
            """.getBytes(StandardCharsets.UTF_8);

    private final GameModePanel panel = new GameModePanel(new JPanel(), new GameModePanel.EditRunner() {
        @Override
        public boolean running() {
            return false;
        }

        @Override
        public void run(String title, GameModePanel.Change change, Runnable onEdt, Runnable afterFailure) {
        }
    }, count -> {
    });
    private final JTabbedPane row = new JTabbedPane();

    @BeforeEach
    void showAFile() throws IOException {
        panel.addSectionTabs(row);
        row.addTab("anomalies", new JPanel());
        show(new String(BINDS, StandardCharsets.UTF_8));
    }

    @AfterEach
    void forgetCustomCommandControls() {
        BindingsMonitor.useCustomCommandControls(Set.of());
    }

    @Test
    void oneGridHoldsEveryControlWhateverIsBoundOnIt() {
        assertEquals(Set.of(KEYBOARD, STICK_ONLY, NOTHING), actions().keySet());
    }

    @Test
    void aKeyboardKeyOffersClearWhetherOrNotVegaNeedsTheControl() throws IOException {
        BindingsMonitor.useCustomCommandControls(Set.of());
        assertEquals(RowAction.CLEAR, actions().get(KEYBOARD));

        require(KEYBOARD);
        assertEquals(RowAction.CLEAR, actions().get(KEYBOARD));
    }

    @Test
    void autoFixIsOfferedOnlyOnWhatVegaRequires() throws IOException {
        require(STICK_ONLY, NOTHING);
        assertEquals(RowAction.AUTO_FIX, actions().get(STICK_ONLY), "Clear reaches keyboard slots only");
        assertEquals(RowAction.AUTO_FIX, actions().get(NOTHING));

        show(unassigned("ControlNoCommandUses", STICK_ONLY));
        assertEquals(RowAction.NONE, actions().get("ControlNoCommandUses"));
    }

    @Test
    void aRowAutoFixCanOnlyRefuseOffersNothingEvenWhenRequired() throws IOException {
        require("Pause", "EjectAllCargo", "BothSlotsOnSticks");
        show("""
                <Root>
                    <Pause><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></Pause>
                    <EjectAllCargo><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></EjectAllCargo>
                    <BothSlotsOnSticks>
                        <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                        <Secondary Device="T16000M" DeviceIndex="0" Key="Joy_4" />
                    </BothSlotsOnSticks>
                </Root>
                """);

        Map<String, RowAction> actions = actions();

        assertEquals(RowAction.NONE, actions.get("Pause"));
        assertEquals(RowAction.NONE, actions.get("EjectAllCargo"));
        assertEquals(RowAction.NONE, actions.get("BothSlotsOnSticks"));
    }

    @Test
    void showUnassignedLeavesOnlyControlsWithNothingBoundAnywhere() {
        unassignedCheckBox().doClick();

        assertEquals(Set.of(NOTHING), actions().keySet());
        assertEquals(getText("bindings.sectionTab.all", 1), row.getTitleAt(ALL));
        assertEquals(getText("bindings.sectionTab.ship", 0), row.getTitleAt(SHIP));
    }

    @Test
    void takingShowUnassignedOffBringsEveryRowBack() {
        unassignedCheckBox().doClick();
        unassignedCheckBox().doClick();

        assertEquals(Set.of(KEYBOARD, STICK_ONLY, NOTHING), actions().keySet());
    }

    @Test
    void aSectionTabNarrowsTheGrid() {
        row.setSelectedIndex(SHIP);
        assertEquals(Set.of(KEYBOARD, STICK_ONLY), actions().keySet());

        row.setSelectedIndex(ON_FOOT);
        assertEquals(Set.of(NOTHING), actions().keySet());
    }

    @Test
    void aModeThatIsNotASectionLeavesTheSectionAsItWas() {
        row.setSelectedIndex(ON_FOOT);

        row.setSelectedIndex(ANOMALIES);

        assertEquals(Set.of(NOTHING), actions().keySet());
    }

    @Test
    void aReadWhileAnotherModeIsUpStillNarrowsToTheSection() {
        row.setSelectedIndex(SHIP);
        row.setSelectedIndex(ANOMALIES);

        panel.search("");

        assertEquals(Set.of(KEYBOARD, STICK_ONLY), actions().keySet());
    }

    @Test
    void theCountsOnTheRowFollowTheSearchAndLeaveTheOtherModesTitleAlone() {
        panel.search("jump");

        assertEquals(getText("bindings.sectionTab.all", 1), row.getTitleAt(ALL));
        assertEquals(getText("bindings.sectionTab.ship", 0), row.getTitleAt(SHIP));
        assertEquals(getText("bindings.sectionTab.onFoot", 1), row.getTitleAt(ON_FOOT));
        assertEquals("anomalies", row.getTitleAt(ANOMALIES));
    }

    /**
     * Makes the controls required, as a custom command that taps them does, and reads the fixture again.
     */
    private void require(String... bindingIds) throws IOException {
        BindingsMonitor.useCustomCommandControls(Set.of(bindingIds));
        show(new String(BINDS, StandardCharsets.UTF_8));
    }

    private void show(String binds) throws IOException {
        byte[] content = binds.getBytes(StandardCharsets.UTF_8);
        panel.show(new GameModePanel.Shown("Test.binds", content, KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(content),
                GameKeyboardLayout.OTHER, EditedDeviceLabels.NONE, BindsValues.NONE));
    }

    private static String unassigned(String tag, String stickOnly) {
        return """
                <Root>
                    <%s><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></%s>
                    <%s><Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" /><Secondary Device="{NoDevice}" Key="" /></%s>
                </Root>
                """.formatted(tag, tag, stickOnly, stickOnly);
    }

    private JCheckBox unassignedCheckBox() {
        // By its tooltip, because a HudCheckBox paints its own label and leaves the Swing text empty.
        String tooltip = getText("bindings.filter.unassignedOnly.tooltip");
        return descendants(panel.footerControls(), JCheckBox.class).stream()
                .filter(box -> tooltip.equals(box.getToolTipText()))
                .findFirst().orElseThrow();
    }

    /**
     * The rows on screen: each control with the action its trailing cell offers.
     */
    private Map<String, RowAction> actions() {
        return descendants(panel, JTable.class).stream()
                .flatMap(table -> java.util.stream.IntStream.range(0, table.getRowCount())
                        .mapToObj(r -> Map.entry(String.valueOf(table.getValueAt(r, 0)),
                                ((RowActionCell) table.getValueAt(r, table.getColumnCount() - 1)).action())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
    }

    private static <T> List<T> descendants(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container container) found.addAll(descendants(container, type));
        }
        return found;
    }
}
