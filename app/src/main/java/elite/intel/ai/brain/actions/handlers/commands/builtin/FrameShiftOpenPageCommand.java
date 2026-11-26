package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.integration.FrameShiftLink;
import elite.intel.integration.FrameShiftLink.OpenResult;
import elite.intel.integration.FrameShiftLink.Page;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Opens a page in FrameShift Command, the companion app that registers itself on the integration socket. One
 * tool serves every page: FrameShift sends its page list when it connects, and that list is the parameter's
 * allowed values. Offered only while FrameShift is connected, so a commander without it never sees the tool.
 * <p>
 * The answer is spoken from FrameShift's reply, not narrated by a second LLM round: opened, refused, gone, or
 * silent past the timeout.
 */
@RegisterCommand
public final class FrameShiftOpenPageCommand implements IntelCommand {

    public static final String ID = "frameshift_open_page";
    static final String PARAM_PAGE = "page";

    private final FrameShiftLink link;

    public FrameShiftOpenPageCommand() {
        this(FrameShiftLink.getInstance());
    }

    FrameShiftOpenPageCommand(FrameShiftLink link) {
        this.link = link;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String llmDescription() {
        return "Open a page in the FrameShift Command companion app (not a game panel). 'page' is the FrameShift page id.";
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return link.isConnected();
    }

    /** Read per turn: the allowed values are whatever pages FrameShift registered last. */
    @Override
    public List<ActionParameterSpec> parameters() {
        List<Page> pages = link.pages();
        String listed = pages.stream().map(p -> p.id() + " = " + p.label()).collect(Collectors.joining(", "));
        return List.of(new ActionParameterSpec(
                PARAM_PAGE,
                "string",
                true,
                "FrameShift page to open. Pages: " + listed + ".",
                List.of(),
                "Match the page the commander names, in any language, to its id.",
                pages.stream().map(Page::id).toList()));
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String pageId = params != null && params.has(PARAM_PAGE) && params.get(PARAM_PAGE).isJsonPrimitive()
                ? params.get(PARAM_PAGE).getAsString().strip() : "";
        return switch (link.openPage(pageId)) {
            case OpenResult.Opened opened -> StringUtls.localizedResponse("handler.frameshift.opened", opened.page().label());
            case OpenResult.Refused refused -> StringUtls.localizedResponse("handler.frameshift.refused", refused.page().label());
            case OpenResult.Offline ignored -> StringUtls.localizedResponse("handler.frameshift.offline");
            case OpenResult.TimedOut ignored -> StringUtls.localizedResponse("handler.frameshift.timedOut");
            case OpenResult.UnknownPage ignored -> StringUtls.localizedResponse("handler.frameshift.unknownPage");
        };
    }
}
