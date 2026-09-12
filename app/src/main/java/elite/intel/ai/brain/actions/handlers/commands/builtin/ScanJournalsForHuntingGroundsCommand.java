package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.gameapi.HuntingGroundJournalScanner;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads the commander's whole journal archive and learns every hunting ground and pirate massacre
 * contract in it.
 * <p>
 * WHY this is a command and not a startup step: a commander who has flown for years has an archive
 * that takes real time to read, and they should not pay for it on every launch to learn something
 * that changes only when they fly. Said once, it recovers everything they have already done. After
 * that the live subscribers keep the ledger current, and saying it again reads only the journals
 * written since.
 */
@RegisterCommand
public final class ScanJournalsForHuntingGroundsCommand implements IntelCommand {

    public static final String ID = "scan_journals_for_hunting_grounds";

    private static final Logger log = LogManager.getLogger(ScanJournalsForHuntingGroundsCommand.class);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Read the commander's saved game journals and learn from them every star system with resource "
                + "extraction sites and every pirate-massacre mission provider, building the local hunting-ground "
                + "record from past flying.";
    }

    /**
     * Reads files and writes two tables - no game input, so it runs from anywhere.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Thread.ofVirtual().name("hunting-ground-scan").start(this::scanAndAnnounce);
        return StringUtls.localizedResponse("handler.pirate.scanStarted");
    }

    /**
     * WHY on its own thread with its own announcement: an archive of years is minutes of reading, and
     * the commander is flying. The command hands back at once and the finding arrives when it arrives.
     */
    private void scanAndAnnounce() {
        try {
            HuntingGroundJournalScanner.Result result =
                    new HuntingGroundJournalScanner().scan(PlayerSession.getInstance().getJournalPath());
            VegaRuntime.narrator().announce(describe(result), false);
        } catch (Exception e) {
            log.error("Hunting ground journal scan failed", e);
            VegaRuntime.narrator().announce(StringUtls.localizedResponse("handler.pirate.scanFailed"), false);
        }
    }

    private String describe(HuntingGroundJournalScanner.Result result) {
        if (result.journalsRead() == 0) {
            return StringUtls.localizedResponse("handler.pirate.scanNoJournals");
        }
        if (result.huntingGroundsLearned() == 0 && result.contractsLearned() == 0) {
            return StringUtls.localizedResponse("handler.pirate.scanNothingNew", result.journalsRead());
        }
        return StringUtls.localizedResponse("handler.pirate.scanFinished",
                result.journalsRead(),
                result.huntingGroundsLearned(),
                result.contractsLearned(),
                result.after().huntingGrounds(),
                result.after().contracts());
    }
}
