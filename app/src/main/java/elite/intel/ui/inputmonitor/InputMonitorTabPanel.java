package elite.intel.ui.inputmonitor;

import com.google.common.eventbus.Subscribe;
import elite.intel.devices.DeviceService;
import elite.intel.devices.events.DeviceServiceStateEvent;
import elite.intel.eventbus.DeviceBus;
import elite.intel.ui.inputmonitor.overlay.AxesReadout;
import elite.intel.ui.inputmonitor.overlay.ButtonReadout;
import elite.intel.ui.inputmonitor.overlay.CounterReadout;
import elite.intel.ui.inputmonitor.overlay.KeyboardReadout;
import elite.intel.ui.theme.HudForms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.AppTheme.applyDarkPalette;
import static elite.intel.ui.theme.AppTheme.makeButton;

/**
 * Input Monitor tab — spawns transparent always-on-top readout overlay windows
 * that display joystick/HOTAS axis positions and button states.
 */
public class InputMonitorTabPanel extends JPanel {

    private static final int AXES_DEFAULT_W  = 200;
    private static final int AXES_DEFAULT_H  = 200;
    private static final int BTN_DEFAULT_W   = 120;
    private static final int BTN_DEFAULT_H   = 120;
    private static final int KEYBOARD_DEFAULT_W = 160;
    private static final int COUNTER_DEFAULT_W  = 160;
    private static final int SPAWN_MARGIN    = 20;
    private static final int READOUT_GAP = 10;
    private static final int READOUT_TOP_Y = 60;

    private JButton activateButton;
    private JLabel  statusLabel;
    private boolean active = false;

    private AxesReadout axesReadout;
    private ButtonReadout buttonReadout;
    private KeyboardReadout keyboardReadout;
    private CounterReadout counterReadout;

    public InputMonitorTabPanel() {
        DeviceBus.register(this);
        buildUi();
    }

    public void dispose() {
        deactivate();
        DeviceBus.unregister(this);
    }

    // -- UI -------------------------------------------------------------------

    private void buildUi() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        GridBagConstraints gbc = HudForms.baseGbc();

        // Description label
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel desc = new JLabel("<html><body style='width:420px'>"
                + getText("inputMonitor.description")
                + "</body></html>");
        desc.setForeground(InputMonitorPalette.DESCRIPTION_TEXT);
        add(desc, gbc);

        // Spacer
        nextRow(gbc);
        gbc.weighty = 0.05;
        add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;

        // Activate button
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        activateButton = makeButton(getText("inputMonitor.activate"));
        activateButton.addActionListener(e -> toggleActivation());
        add(activateButton, gbc);

        // Status label
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(InputMonitorPalette.STATUS_PENDING_TEXT);
        add(statusLabel, gbc);

        // Push everything up
        nextRow(gbc);
        gbc.weighty = 1;
        add(Box.createVerticalGlue(), gbc);

        applyDarkPalette(this);
    }

    // -- Activation -----------------------------------------------------------

    private void toggleActivation() {
        if (active) deactivate(); else activate();
    }

    private void activate() {
        if (!DeviceService.getInstance().isAvailable()) {
            statusLabel.setText(getText("inputMonitor.sdl.unavailable"));
            statusLabel.setForeground(InputMonitorPalette.STATUS_UNAVAILABLE_TEXT);
        } else {
            statusLabel.setText(" ");
        }

        // Spawn readouts in upper-right, side by side
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int axesX  = screen.width - AXES_DEFAULT_W - SPAWN_MARGIN;
        int btnX = axesX - BTN_DEFAULT_W - READOUT_GAP;
        int row2Y = READOUT_TOP_Y + AXES_DEFAULT_H + READOUT_GAP;
        int keyboardX = screen.width - KEYBOARD_DEFAULT_W - SPAWN_MARGIN;
        int counterX = keyboardX - COUNTER_DEFAULT_W - READOUT_GAP;

        axesReadout = new AxesReadout();
        axesReadout.setLocation(axesX, READOUT_TOP_Y);
        axesReadout.showReadout();

        buttonReadout = new ButtonReadout();
        buttonReadout.setLocation(btnX, READOUT_TOP_Y);
        buttonReadout.showReadout();

        keyboardReadout = new KeyboardReadout();
        keyboardReadout.setLocation(keyboardX, row2Y);
        keyboardReadout.showReadout();

        counterReadout = new CounterReadout();
        counterReadout.setLocation(counterX, row2Y);
        counterReadout.showReadout();

        active = true;
        activateButton.setText(getText("inputMonitor.deactivate"));
    }

    private void deactivate() {
        if (axesReadout != null) {
            axesReadout.closeReadout();
            axesReadout = null;
        }
        if (buttonReadout != null) {
            buttonReadout.closeReadout();
            buttonReadout = null;
        }
        if (keyboardReadout != null) {
            keyboardReadout.closeReadout();
            keyboardReadout = null;
        }
        if (counterReadout != null) {
            counterReadout.closeReadout();
            counterReadout = null;
        }
        active = false;
        if (activateButton != null) activateButton.setText(getText("inputMonitor.activate"));
    }

    // -- Event handlers -------------------------------------------------------

    @Subscribe
    public void onDeviceServiceState(DeviceServiceStateEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (!event.available()) {
                statusLabel.setText(getText("inputMonitor.sdl.unavailable"));
                statusLabel.setForeground(InputMonitorPalette.STATUS_UNAVAILABLE_TEXT);
            } else if (active) {
                statusLabel.setText(" ");
            }
        });
    }

    // -- Layout helper --------------------------------------------------------

    private static void nextRow(GridBagConstraints gbc) { gbc.gridy++; }
}
