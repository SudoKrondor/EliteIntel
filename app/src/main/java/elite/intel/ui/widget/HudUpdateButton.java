package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.EventBusManager;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.SystemShutDownEvent;
import elite.intel.ui.event.UpdateAvailableEvent;
import elite.intel.ui.event.UpdateStartedEvent;
import elite.intel.util.Updater;

import javax.swing.*;

import elite.intel.ui.i18n.MultiLingualTextProvider;

/**
 * Self-contained "update application" button. Subtle HUD style, owns the
 * three-phase update state machine (up-to-date / available / updating) and
 * stays in sync with every other instance via the event bus. Used on the
 * Settings panel and the AI SHORTCUTS sidebar.
 */
public class HudUpdateButton extends HudButton {

    public HudUpdateButton() {
        this(true);
    }

    public HudUpdateButton(boolean showIcon) {
        super(MultiLingualTextProvider.getText("settings.update.upToDate"), false);
        setEnabled(false);
        if (showIcon) {
            setIcon(AppTheme.scaledIcon(getClass(), "/images/update.png", AppTheme.HUD_ICON_MAIN));
        }
        addActionListener(e -> onClick());
        EventBusManager.register(this);
    }

    private void onClick() {
        // Notify all instances first, then launch once from this instance.
        EventBusManager.publish(new UpdateStartedEvent());
        Updater.performUpdateAsync().thenAccept(launched -> {
            if (launched) {
                EventBusManager.publish(new SystemShutDownEvent());
            } else {
                SwingUtilities.invokeLater(() -> {
                    setEnabled(true);
                    setText(MultiLingualTextProvider.getText("settings.update.available"));
                });
                EventBusManager.publish(new AppLogEvent(
                        "Could not launch updater - is elite_intel_updater.jar present?"));
            }
        });
    }

    @Subscribe
    public void onUpdateAvailableEvent(UpdateAvailableEvent event) {
        SwingUtilities.invokeLater(() -> {
            setEnabled(true);
            setText(MultiLingualTextProvider.getText("settings.update.available"));
        });
    }

    @Subscribe
    public void onUpdateStartedEvent(UpdateStartedEvent event) {
        SwingUtilities.invokeLater(() -> {
            setEnabled(false);
            setText(MultiLingualTextProvider.getText("settings.update.updating"));
        });
    }

    /** Unregisters from the event bus. Must be called when the owning panel is disposed. */
    public void dispose() {
        EventBusManager.unregister(this);
    }
}
