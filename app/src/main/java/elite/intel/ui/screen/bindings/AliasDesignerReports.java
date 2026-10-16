package elite.intel.ui.screen.bindings;

import elite.intel.bindforge.devicefiles.*;
import elite.intel.bindforge.io.DeviceRewriteResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;

/**
 * What the Alias Designer screen tells the user once a write has run - Apply, a way out, onboarding - and what
 * first setup names when activation opens it. Words only: nothing here reads or writes anything.
 */
final class AliasDesignerReports {

    private final Map<String, String> installLabels;

    /**
     * @param installLabels what each installation is called on screen, keyed as findings key them
     */
    AliasDesignerReports(Map<String, String> installLabels) {
        this.installLabels = installLabels;
    }

    /**
     * The installations' names as the list shows them, sorted.
     */
    String namesOf(List<Long> installIds) {
        return installIds.stream()
                .map(DeviceDivergence::installKey)
                .map(key -> installLabels.getOrDefault(key, key))
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * "Updated N of M", and each installation that did not take it, with why.
     */
    String revertText(DeviceFilesPush.Report report) {
        StringBuilder text = new StringBuilder(getText("bindings.aliasDesigner.wayOut.revert.result",
                report.matchingCount(), report.installations().size()));
        for (DeviceFilesPush.InstallationResult result : report.installations()) {
            if (result.reason() == null) continue;
            text.append('\n').append(getText("bindings.aliasDesigner.firstSetup.result.installation",
                    namesOf(List.of(result.target().installId())), result.reason()));
        }
        return text.toString();
    }

    String adoptText(DeviceFilesAdopt.Result result, String doneText) {
        return switch (result.outcome()) {
            case ADOPTED -> result.conflicts().isEmpty()
                    ? doneText
                    : doneText + "\n" + getText("bindings.aliasDesigner.wayOut.conflicts",
                    result.conflicts().stream()
                            .map(conflict -> conflict.inputToken() == null
                                    ? conflict.deviceName()
                                    : conflict.deviceName() + " " + conflict.inputToken())
                            .distinct()
                            .collect(Collectors.joining(", ")));
            case NOTHING_TO_ADOPT -> getText("bindings.aliasDesigner.wayOut.nothing");
            case REFUSED -> getText("bindings.aliasDesigner.wayOut.refused", result.reason());
        };
    }

    String applyText(DeviceApply.Result result) {
        StringBuilder text = new StringBuilder(revertText(result.report()));
        if (!result.cleanedUp().isEmpty()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.removed",
                    String.join(", ", result.cleanedUp())));
        }
        if (!result.notEverywhere().isEmpty()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.notEverywhere",
                    String.join(", ", result.notEverywhere())));
        }
        if (result.bindsUnread()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.bindsUnread"));
        } else if (!result.heldBack().isEmpty()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.heldBack",
                    String.join(", ", result.heldBack())));
        }
        if (result.movesUnread()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.movesUnread"));
        }
        for (DeviceApply.Move move : result.moves()) {
            text.append('\n').append(switch (move.outcome()) {
                case MOVED -> getText("bindings.aliasDesigner.apply.moved", move.bindings(), move.name());
                case NOT_APPLIED -> getText("bindings.aliasDesigner.apply.moveNotApplied", move.name());
                case NOT_FOUND -> getText("bindings.aliasDesigner.apply.moveNotFound", move.name());
                case FAILED -> getText("bindings.aliasDesigner.apply.moveFailed", move.name(),
                        moveReason(move.status()));
            });
        }
        for (DeviceApply.RenameMove move : result.renameMoves()) {
            text.append('\n').append(switch (move.outcome()) {
                case MOVED -> getText("bindings.aliasDesigner.apply.renameMoved", move.bindings(), move.from(),
                        move.to());
                case NOT_APPLIED -> getText("bindings.aliasDesigner.apply.renameNotApplied", move.from(), move.to());
                case NOT_FOUND -> getText("bindings.aliasDesigner.apply.renameNotFound", move.from());
                case FAILED -> getText("bindings.aliasDesigner.apply.renameFailed", move.from(), move.to(),
                        moveReason(move.status()));
            });
        }
        if (!result.renamesFinished().isEmpty()) {
            text.append('\n').append(getText("bindings.aliasDesigner.apply.renamed",
                    String.join(", ", result.renamesFinished())));
        }
        return text.toString();
    }

    /**
     * Why a move stopped, in the user's words - the status itself is in the log.
     */
    private static String moveReason(DeviceRewriteResult.Status status) {
        return switch (status) {
            case STALE_FILE -> getText("bindings.aliasDesigner.apply.moveReason.stale");
            case READ_FAILED, UNSUPPORTED_XML -> getText("bindings.aliasDesigner.apply.moveReason.unread");
            case BACKUP_FAILED -> getText("bindings.aliasDesigner.apply.moveReason.backup");
            case WRITE_FAILED, REWRITTEN, NO_REFERENCES -> getText("bindings.aliasDesigner.apply.moveReason.write");
        };
    }

    String onboardText(DeviceOnboarding.Result result) {
        if (!result.recorded()) return getText("bindings.aliasDesigner.onboarding.result.notRecorded");
        StringBuilder text = new StringBuilder();
        // WHY: said even with no installation to write - "updated 0 of 0" - unless every name waits for Apply.
        if (!result.installations().isEmpty() || result.atApply().isEmpty()) {
            text.append(getText("bindings.aliasDesigner.wayOut.revert.result", result.matchingCount(),
                    result.installations().size()));
            for (DeviceOnboarding.InstallationResult installation : result.installations()) {
                String install = namesOf(List.of(installation.target().installId()));
                if (installation.reason() != null) {
                    text.append('\n').append(getText("bindings.aliasDesigner.firstSetup.result.installation", install,
                            installation.reason()));
                }
                for (String kept : installation.keptButtonMaps()) {
                    text.append('\n').append(getText("bindings.aliasDesigner.onboarding.result.keptButtonMap",
                            install, kept));
                }
            }
        }
        for (DeviceOnboarding.Naming naming : result.atApply()) {
            if (!text.isEmpty()) text.append('\n');
            text.append(getText("bindings.aliasDesigner.onboarding.result.atApply", naming.name(), naming.bindings()));
        }
        return text.toString();
    }

    /**
     * What first setup should name, for activation that opens it: a file it could not read, if any.
     */
    static String problemOf(Activation.Result result) {
        if (result.reason() == null) return null;
        return result.reading() == null
                ? getText("bindings.aliasDesigner.firstSetup.readFailed", result.reason())
                : getText("bindings.aliasDesigner.firstSetup.unreadableFile", result.reason());
    }

    /**
     * The application-log line for an activation that failed, or {@code null} when it did not fail - otherwise the
     * user sees SET UP and nothing saying why (review, 2026-10-13).
     */
    static String activationFailure(Activation.Outcome outcome, String reason) {
        String what = switch (outcome) {
            case BACKUP_FAILED -> "the backup before BindForge could not be taken";
            case FAILED -> "it could not be set up";
            default -> null;
        };
        if (what == null) return null;
        return "BindForge was not activated: " + what + " - " + reason + ". SET UP on Alias Designer tries again.";
    }
}
