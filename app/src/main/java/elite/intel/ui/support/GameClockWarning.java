package elite.intel.ui.support;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.GameClockDisagreementEvent;

import javax.swing.*;
import java.awt.*;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * The dialog telling the commander that the game and the PC disagree about the time, what that keeps breaking,
 * and how to fix it. The app log is ours, not theirs - most commanders never read it - so this is the one place
 * they hear about it.
 * <p>
 * A whole-hour gap is a time zone or daylight-saving setting gone wrong (typically after a Windows Update), so it
 * gets that cause and the time-zone steps. Anything else is a clock that has drifted, and syncing it is the whole fix.
 * The gap is always a minute or more ({@code GameClock.WARNING_THRESHOLD}), so it is told in hours or minutes.
 */
public final class GameClockWarning {

    private GameClockWarning() {
    }

    public static void show(Component parent, GameClockDisagreementEvent event) {
        JOptionPane.showMessageDialog(
                parent,
                message(event, SystemSession.getInstance().getLanguage()),
                getText("gameClock.dialog.title"),
                JOptionPane.WARNING_MESSAGE);
    }

    static String message(GameClockDisagreementEvent event, Language language) {
        boolean timeZone = event.wholeHours() > 0;
        String cause = timeZone
                ? getText(language, "gameClock.dialog.cause.timeZone")
                : getText(language, "gameClock.dialog.cause.drift");
        String fix = timeZone
                ? getText(language, "gameClock.dialog.fix.timeZone")
                : getText(language, "gameClock.dialog.fix.sync");
        return getText(language, "gameClock.dialog.body", amount(event, language)) + " " + cause
                + "\n\n" + getText(language, "gameClock.dialog.consequences")
                + "\n\n" + fix
                + "\n\n" + getText(language, "gameClock.dialog.check");
    }

    private static String amount(GameClockDisagreementEvent event, Language language) {
        if (event.wholeHours() > 0) return getText(language, "gameClock.dialog.amount.hours", event.wholeHours());
        long minutes = Math.max(1, Math.round(event.offset().abs().getSeconds() / 60.0));
        return getText(language, "gameClock.dialog.amount.minutes", minutes);
    }
}
