package elite.intel.ui.dialog;

/**
 * A dialog the user can quiet with its <em>"Don't show this again"</em> box (Alan, 2026-11-14, Storage S21; storage.md
 * rule 13). Each name is a row in {@code quieted_notices} while ticked, so a dialog joins by a constant here - no
 * migration. A dialog that asks, rather than tells, never joins: the split preset's two questions stay out (S13f).
 */
public enum QuietableNotice {
    /** APPLY's confirmation, its Linux note with it. Quieted, APPLY writes on the press. */
    BINDFORGE_APPLY_CONFIRM("bindforge.apply.confirm"),
    /** APPLY's report, quieted only when it says nothing but its plain success line. */
    BINDFORGE_APPLY_REPORT("bindforge.apply.report"),
    /** First-time startup's report, quieted only when it says nothing but its plain success line. */
    BINDFORGE_FIRST_TIME_STARTUP_REPORT("bindforge.firstTimeStartup.report"),
    /**
     * The Bind Editor's <em>"Saved."</em> after an assignment (Alan, 2026-11-14). Only that one: <em>"No changes."</em>
     * and every refusal still show.
     */
    BINDFORGE_ASSIGN_SAVED("bindforge.assign.saved");

    private final String key;

    QuietableNotice(String key) {
        this.key = key;
    }

    /** The row's name in {@code quieted_notices}. Never change one: a user's tick is stored under it. */
    public String key() {
        return key;
    }
}
