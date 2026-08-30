package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.ExoBio;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * "The organics here are already sampled" - the one thing about a body the journal cannot tell us.
 *
 * <p>The game remembers every organism a commander has ever logged; the journal does not report it.
 * A commander who sampled a body before installing this app therefore has a body the game will not
 * let them scan again and we believe is untouched, and no event will ever arrive to settle it. The
 * commander is the only source, so they get to say it: this records
 * {@link LocationDto#setBioScansCompleted(boolean)} by hand, which is what the overlay card, the
 * survey briefing and the exobiology queries all read.
 *
 * <p><b>One command with a state rather than a mark/unmark pair.</b> Two commands this close in
 * meaning cannot be told apart by the semantic reducer - it is the sibling problem that collapsed
 * ten carrier queries into three - and every phrase that reached the model would be a coin toss
 * between them. As a single boolean the reflex owns it outright: each alias pins its own literal
 * {@code state}, so "the bio scans here are already done" and "undo the bio scan mark" dispatch
 * deterministically without the model being consulted at all.
 *
 * <p>Not flagged dangerous, on the same licence as hardpoints: it is reversible by its own other
 * half, self-guarding (offered and executed only at a body with biology on it), and it names the
 * body it acted on out loud, so a misfire is both obvious and one sentence away from undone.
 */
@RegisterCommand
public final class SetCurrentBodyExobiologySurveyCompleteCommand implements IntelCommand {
    public static final String ID = "set_current_body_exobiology_survey_complete";

    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", true,
                "True to record this body's biological survey as already complete, false to take that record back.",
                List.of("true", "false"),
                "already scanned/already done/nothing left here → true; "
                        + "undo/cancel/that was wrong/not scanned after all → false. "
                        + "Never guess: if the commander did not say which way, do not call this.");
        state.validate();
        return List.of(state);
    }

    @Override
    public String llmDescription() {
        return "Record by hand that the commander has ALREADY sampled every organism on the body they are at, "
                + "in a past play session the journal never reported - or, with state=false, take that record "
                + "back. Only affects our own bookkeeping: it removes the body from the sampling overlay and "
                + "stops it being offered for exobiology. Never use it to report a scan just performed; the "
                + "journal records those on its own.";
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    /**
     * Offered only at a body that has biology on it, which is the guard against the whole class of
     * false positives this command invites: a stray "we're done here" said in supercruise, at a
     * station, or over a barren moon cannot reach a command that is not in the offered set. It stays
     * offered on a body already recorded as complete, because that is the body the undo is for.
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return bodyWithBiology() != null;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        if (params.get(PARAM_STATE) == null) {
            return StringUtls.localizedResponse("handler.common.llmParamFailed");
        }
        LocationDto body = bodyWithBiology();
        if (body == null) {
            // Re-checked rather than trusted from visibility: the commander can leave the body between
            // the turn being offered and the words arriving, and this writes to a stored row.
            return StringUtls.localizedResponse("handler.exobiology.noBiologyHere");
        }
        boolean complete = params.get(PARAM_STATE).getAsBoolean();
        long systemAddress = body.getSystemAddress();
        long bodyId = body.getBodyId();
        locationManager.updateBody(systemAddress, bodyId, location -> location.setBioScansCompleted(complete));
        String name = describe(body);
        return complete
                ? StringUtls.localizedResponse("handler.exobiology.surveyRecordedComplete", name)
                : StringUtls.localizedResponse("handler.exobiology.surveyRecordCleared", name);
    }

    /**
     * The body the commander is at, when it is one a biological survey could apply to, otherwise null.
     *
     * <p>"Has biology" is the DSS/FSS signal count or a detected genus list - either is enough, because
     * a body can carry a genus list from a scan whose signal count never landed on the row.
     */
    private LocationDto bodyWithBiology() {
        LocationData<Long, Long> here = playerSession.getLocationData();
        if (here == null || here.getSystemAddress() == null || here.getSystemAddress() == 0) return null;
        if (here.getInGameId() == null) return null;
        LocationDto body = locationManager.findByLocationData(here);
        if (body == null || body.getBodyId() < 0) return null;
        boolean hasBiology = ExoBio.bioSignalsDetected(body) > 0
                || (body.getGenus() != null && !body.getGenus().isEmpty());
        return hasBiology ? body : null;
    }

    /**
     * Named out loud so a misfire is visible. The short name is what a commander calls the body.
     */
    private static String describe(LocationDto body) {
        String shortName = body.getPlanetShortName();
        if (shortName != null && !shortName.isBlank()) return shortName;
        String name = body.getPlanetName();
        return name == null || name.isBlank()
                ? StringUtls.localizedResponse("handler.exobiology.thisBody")
                : name;
    }
}
