package elite.intel.ui.screen;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRouteHelpPanelTest {

    @Test
    void buildViewStateSortsRemainingLegsAndPreservesTradeDetails() {
        TradeRouteHelpPanel.CommodityData gold = new TradeRouteHelpPanel.CommodityData(
                "Gold", 24, 48_000L, 5_000L, 53_000L, 7_000L, 5_000L, 120_000L);
        TradeRouteHelpPanel.RouteLegData later = leg(
                4, "Achenar", "Dawes Hub", "Alioth", "Irkutsk", 80_000L);
        TradeRouteHelpPanel.RouteLegData current = new TradeRouteHelpPanel.RouteLegData(
                2, "Sol", "Galileo", "Achenar", "Dawes Hub", List.of(gold));

        TradeRouteHelpPanel.RouteViewState state = TradeRouteHelpPanel.buildViewState(
                List.of(later, current),
                new TradeRouteHelpPanel.CurrentPlace("Sol", "Galileo"));

        assertEquals(List.of(2, 4), state.legs().stream()
                .map(TradeRouteHelpPanel.RouteLegData::legNumber)
                .toList());
        assertEquals(200_000L, state.totalProfit());
        assertEquals(gold, state.legs().get(0).commodities().get(0));
        assertEquals("Sol", state.currentPlace().system());
    }

    @Test
    void buildViewStateFiltersNullEntriesAndCopiesTheInput() {
        List<TradeRouteHelpPanel.RouteLegData> legs = new ArrayList<>();
        legs.add(leg(3, "Sol", "Galileo", "Achenar", "Dawes Hub", 50_000L));
        legs.add(null);

        TradeRouteHelpPanel.RouteViewState state = TradeRouteHelpPanel.buildViewState(
                legs, new TradeRouteHelpPanel.CurrentPlace(null, null));
        legs.clear();

        assertEquals(1, state.legs().size());
        assertEquals("", state.currentPlace().system());
        assertEquals("", state.currentPlace().station());
    }

    @Test
    void buildViewStateExpandsOneTradeLegIntoBuyAndSellPoints() {
        TradeRouteHelpPanel.CommodityData gold = new TradeRouteHelpPanel.CommodityData(
                "Gold", 24, 48_000L, 5_000L, 53_000L, 7_000L, 5_000L, 120_000L);
        TradeRouteHelpPanel.RouteLegData leg = new TradeRouteHelpPanel.RouteLegData(
                3, "Sol", "Galileo", "Achenar", "Dawes Hub", List.of(gold));

        TradeRouteHelpPanel.RouteViewState state = TradeRouteHelpPanel.buildViewState(
                List.of(leg),
                new TradeRouteHelpPanel.CurrentPlace("Achenar", "Dawes Hub"));

        assertEquals(2, state.points().size());
        TradeRouteHelpPanel.RoutePointData buy = state.points().get(0);
        TradeRouteHelpPanel.RoutePointData sell = state.points().get(1);
        assertEquals(1, buy.pointNumber());
        assertEquals(TradeRouteHelpPanel.RoutePointAction.BUY, buy.action());
        assertEquals("Sol", buy.system());
        assertEquals("Galileo", buy.station());
        assertNull(buy.profit());
        assertEquals(2, sell.pointNumber());
        assertEquals(TradeRouteHelpPanel.RoutePointAction.SELL, sell.action());
        assertEquals("Achenar", sell.system());
        assertEquals("Dawes Hub", sell.station());
        assertEquals(120_000L, sell.profit());
        assertEquals(1, TradeRouteHelpPanel.activePointIndex(state));
    }

    @Test
    void routePointMatchRequiresTheKnownRouteStation() {
        TradeRouteHelpPanel.RouteLegData leg = leg(
                1, "Sol", "Galileo", "Achenar", "Dawes Hub", 50_000L);

        assertTrue(TradeRouteHelpPanel.isAtRoutePoint(
                new TradeRouteHelpPanel.CurrentPlace("sol", "galileo"), leg));
        assertTrue(TradeRouteHelpPanel.isAtRoutePoint(
                new TradeRouteHelpPanel.CurrentPlace("ACHENAR", "DAWES HUB"), leg));
        assertFalse(TradeRouteHelpPanel.isAtRoutePoint(
                new TradeRouteHelpPanel.CurrentPlace("Sol", ""), leg));
        assertFalse(TradeRouteHelpPanel.isAtRoutePoint(
                new TradeRouteHelpPanel.CurrentPlace("Sol", "Daedalus"), leg));
        assertFalse(TradeRouteHelpPanel.isAtRoutePoint(
                new TradeRouteHelpPanel.CurrentPlace("Alioth", "Irkutsk"), leg));
    }

    @Test
    void routeValueCopyHitTargetsOnlyTheReservedSystemAndStationArea() {
        Rectangle cell = new Rectangle(80, 0, 180, 26);

        assertTrue(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 3, "Sol", 234));
        assertTrue(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 4, "Galileo", 240));
        assertFalse(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 3, "Sol", 233));
        assertFalse(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 5, "Gold", 240));
        assertFalse(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 3, "—", 240));
        assertFalse(TradeRouteHelpPanel.isRouteValueCopyHit(cell, 4, " ", 240));
    }

    @Test
    void contextReadoutsKeepValuesVisibleToTheRightOfTheirLabels() throws ReflectiveOperationException {
        TradeRouteHelpPanel panel = new TradeRouteHelpPanel();
        panel.setSize(1_744, 900);
        layoutTree(panel);

        JTextField locationField = privateField(panel, "currentLocationField", JTextField.class);
        JTextField shipField = privateField(panel, "currentShipField", JTextField.class);
        JButton shipProfileButton = privateField(panel, "currentShipProfileButton", JButton.class);
        JTextField systemField = privateField(panel, "startSystemField", JTextField.class);

        assertFalse(locationField.isEditable());
        assertFalse(shipField.isEditable());
        assertFalse(systemField.isEditable());
        assertReadoutValueIsToTheRight(systemField, 200);
        assertReadoutValueIsToTheRight(locationField, 200);
        assertReadoutValueIsToTheRight(shipField, 200);
        assertFieldActionFollows(shipField, shipProfileButton);
    }

    private static TradeRouteHelpPanel.RouteLegData leg(
            int number,
            String sourceSystem,
            String sourceStation,
            String destinationSystem,
            String destinationStation,
            long profit) {
        return new TradeRouteHelpPanel.RouteLegData(
                number,
                sourceSystem,
                sourceStation,
                destinationSystem,
                destinationStation,
                List.of(new TradeRouteHelpPanel.CommodityData(
                        "Silver", 10, 10L, 100L, 20L, 200L, 10L, profit)));
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutTree(child);
            }
        }
    }

    private static <T> T privateField(Object target, String name, Class<T> type) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void assertReadoutValueIsToTheRight(JComponent value, int minimumWidth) {
        Container form = gridBagParent(value);
        JComponent label = labelForRow(form, value);
        int valueX = xWithin(value, form);
        assertTrue(value.getWidth() >= minimumWidth, () -> "Value hierarchy: " + hierarchyBounds(value));
        assertTrue(valueX >= label.getX() + label.getWidth(),
                () -> "Label bounds: " + label.getBounds() + ", value bounds: " + value.getBounds());
    }

    private static void assertFieldActionFollows(JComponent value, JComponent action) {
        Container form = gridBagParent(value);
        assertEquals(form, gridBagParent(action));
        assertEquals(value.getHeight(), action.getHeight());
        assertTrue(xWithin(action, form) >= xWithin(value, form) + value.getWidth(),
                () -> "Field bounds: " + value.getBounds() + ", action bounds: " + action.getBounds());
    }

    private static JComponent labelForRow(Container form, JComponent value) {
        GridBagLayout layout = (GridBagLayout) form.getLayout();
        int row = layout.getConstraints(value).gridy;
        for (java.awt.Component component : form.getComponents()) {
            GridBagConstraints constraints = layout.getConstraints(component);
            if (constraints.gridx == 0 && constraints.gridy == row) {
                return (JComponent) component;
            }
        }
        throw new AssertionError("No label found for " + value.getClass().getSimpleName());
    }

    private static Container gridBagParent(JComponent component) {
        for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getLayout() instanceof GridBagLayout) {
                return parent;
            }
        }
        throw new AssertionError("No GridBag parent for " + component.getClass().getSimpleName());
    }

    private static int xWithin(java.awt.Component component, Container ancestor) {
        int x = 0;
        for (java.awt.Component current = component; current != ancestor; current = current.getParent()) {
            x += current.getX();
        }
        return x;
    }

    private static String hierarchyBounds(java.awt.Component component) {
        StringBuilder bounds = new StringBuilder();
        for (java.awt.Component current = component; current != null; current = current.getParent()) {
            if (!bounds.isEmpty()) {
                bounds.append(" <- ");
            }
            bounds.append(current.getClass().getSimpleName()).append(current.getBounds());
        }
        return bounds.toString();
    }
}
