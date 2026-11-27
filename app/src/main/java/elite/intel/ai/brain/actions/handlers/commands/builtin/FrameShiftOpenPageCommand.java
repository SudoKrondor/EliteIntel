package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.integration.FrameShiftLink;
import elite.intel.integration.FrameShiftLink.Destination;
import elite.intel.integration.FrameShiftLink.OpenResult;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Opens a page in FrameShift Command, the companion app that registers itself on the integration socket. One
 * tool serves every page and tab: FrameShift sends its page list, with each page's tabs, when it connects, and that
 * list is the parameter's allowed values - {@code materials}, or {@code materials/manufactured} for one tab. One
 * value rather than a page and a tab, so the model cannot pair a tab with the wrong page. Offered only while FrameShift is connected, so a commander without it never sees the tool.
 * <p>
 * The answer is spoken from FrameShift's reply, not narrated by a second LLM round: opened, refused, gone, or
 * silent past the timeout.
 */
@RegisterCommand
public final class FrameShiftOpenPageCommand implements IntelCommand {

    public static final String ID = "frameshift_open_page";
    static final String PARAM_PAGE = "page";
    private static final Pattern NOT_A_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

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
        return "Open a page, or a tab of a page, in the FrameShift Command companion app (not a game panel).";
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return link.isConnected();
    }

    /**
     * Read per turn: the allowed values are whatever pages and tabs FrameShift registered last. The description
     * names only the ones whose label the id does not already say ({@code navigation = Route Planner}), to keep
     * the prompt short.
     */
    @Override
    public List<ActionParameterSpec> parameters() {
        List<Destination> destinations = link.destinations();
        String renamed = destinations.stream()
                .filter(d -> !words(d.id()).equals(words(d.label())))
                .map(d -> d.id() + " = " + d.label())
                .collect(Collectors.joining(", "));
        return List.of(new ActionParameterSpec(
                PARAM_PAGE,
                "string",
                true,
                "FrameShift page to open, or page/tab for one tab of it."
                        + (renamed.isEmpty() ? "" : " Named differently: " + renamed + "."),
                List.of(),
                "Match what the commander names, in any language, to its id. Use page/tab only when they name the tab.",
                destinations.stream().map(Destination::id).toList()));
    }

    /** An id or label as lower-case words, so {@code ships-builds} and {@code Ships & Builds} compare equal. */
    private static String words(String text) {
        return NOT_A_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        String pageId = params != null && params.has(PARAM_PAGE) && params.get(PARAM_PAGE).isJsonPrimitive()
                ? params.get(PARAM_PAGE).getAsString().strip() : "";
        return switch (link.openPage(pageId)) {
            case OpenResult.Opened opened -> StringUtls.localizedResponse("handler.frameshift.opened", opened.destination().label());
            case OpenResult.Refused refused -> StringUtls.localizedResponse("handler.frameshift.refused", refused.destination().label());
            case OpenResult.Offline ignored -> StringUtls.localizedResponse("handler.frameshift.offline");
            case OpenResult.TimedOut ignored -> StringUtls.localizedResponse("handler.frameshift.timedOut");
            case OpenResult.UnknownPage ignored -> StringUtls.localizedResponse("handler.frameshift.unknownPage");
        };
    }
}
