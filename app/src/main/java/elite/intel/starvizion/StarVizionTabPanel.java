package elite.intel.starvizion;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.starvizion.event.SvServiceStateEvent;
import elite.intel.starvizion.input.SdlInputService;
import elite.intel.starvizion.overlay.AxesVizlet;
import elite.intel.starvizion.overlay.ButtonVizlet;
import elite.intel.ui.view.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.view.AppTheme.*;

/**
 * StarVizion tab — spawns transparent always-on-top Vizlet overlay windows
 * that display joystick/HOTAS axis positions and button states.
 */
public class StarVizionTabPanel extends JPanel {

    private static final int AXES_DEFAULT_W  = 200;
    private static final int AXES_DEFAULT_H  = 200;
    private static final int BTN_DEFAULT_W   = 120;
    private static final int BTN_DEFAULT_H   = 120;
    private static final int SPAWN_MARGIN    = 20;
    private static final int VIZLET_GAP      = 10;
    private static final int VIZLET_TOP_Y    = 60;

    private JButton activateButton;
    private JLabel  statusLabel;
    private boolean active = false;

    private SdlInputService sdlInputService;
    private AxesVizlet  axesVizlet;
    private ButtonVizlet buttonVizlet;

    public StarVizionTabPanel() {
        EventBusManager.register(this);
        buildUi();
    }

    public void dispose() {
        deactivate();
        if (sdlInputService != null) sdlInputService.stop();
        EventBusManager.unregister(this);
    }

    // -- UI -------------------------------------------------------------------

    private void buildUi() {
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        GridBagConstraints gbc = AppTheme.baseGbc();

        // Description label
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel desc = new JLabel("<html><body style='width:420px'>"
                + getText("starvizion.description")
                + "</body></html>");
        desc.setForeground(FG_MUTED);
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
        activateButton = makeButton(getText("starvizion.activate"));
        activateButton.addActionListener(e -> toggleActivation());
        add(activateButton, gbc);

        // Status label
        nextRow(gbc);
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(FG_MUTED);
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
        // Lazy-init SDL service on first activation — result arrives via SvServiceStateEvent
        if (sdlInputService == null) {
            sdlInputService = SdlInputService.getInstance();
            sdlInputService.start();
            statusLabel.setText(getText("starvizion.sdl.initializing"));
            statusLabel.setForeground(FG_MUTED);
        }

        // Spawn vizlets in upper-right, side by side
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int axesX  = screen.width - AXES_DEFAULT_W - SPAWN_MARGIN;
        int btnX   = axesX - BTN_DEFAULT_W - VIZLET_GAP;

        axesVizlet = new AxesVizlet();
        axesVizlet.setLocation(axesX, VIZLET_TOP_Y);
        axesVizlet.showVizlet();

        buttonVizlet = new ButtonVizlet();
        buttonVizlet.setLocation(btnX, VIZLET_TOP_Y);
        buttonVizlet.showVizlet();

        active = true;
        activateButton.setText(getText("starvizion.deactivate"));
    }

    private void deactivate() {
        if (axesVizlet != null) { axesVizlet.closeVizlet(); axesVizlet = null; }
        if (buttonVizlet != null) { buttonVizlet.closeVizlet(); buttonVizlet = null; }
        active = false;
        if (activateButton != null) activateButton.setText(getText("starvizion.activate"));
    }

    // -- Event handlers -------------------------------------------------------

    @Subscribe
    public void onSvServiceState(SvServiceStateEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (!event.available()) {
                statusLabel.setText(getText("starvizion.sdl.unavailable"));
                statusLabel.setForeground(DISABLED_FG);
            } else if (active) {
                statusLabel.setText(" ");
            }
        });
    }

    // -- Layout helper --------------------------------------------------------

    private static void nextRow(GridBagConstraints gbc) { gbc.gridy++; }
}
