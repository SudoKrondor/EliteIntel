package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.commands.custom.CustomCommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.ConnectionCheckQuery;
import elite.intel.ai.brain.actions.handlers.queries.GeneralConversationQuery;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.actions.handlers.queries.RegisterQuery;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;

/**
 * Builds the legacy phrase-to-action map from available, localized commands and queries. Action ids provide the
 * stable base order, optional {@code before} hints refine it, and runtime fallback entries are appended last.
 */
public class AiActionMapGenerator {

    private static final Logger log = LogManager.getLogger(AiActionMapGenerator.class);

    /**
     * Assembles the action map for the given context.
     *
     * @param status session status used for context visibility, ignored in dry-run mode
     * @param isDryRun whether status visibility should be ignored
     * @param conversationalMode selects the conversational or ignore fallback
     * @return ordered phrase-group -> id map (LinkedHashMap)
     */
    public Map<String, String> generate(Status status, boolean isDryRun, boolean conversationalMode) {
        List<IntelAction> actions = new ArrayList<>();
        actions.addAll(CommandRegistry.getInstance().byId().values());
        actions.addAll(QueryRegistry.getInstance().byId().values());

        List<IntelAction> visible = new ArrayList<>();
        for (IntelAction action : actions) {
            String id = action.id();
            if (!action.isAvailableIn(IntelActionContext.LEGACY_ACTION_MAP)) {
                continue;
            }
            // Include only if the alias bundle for the current language actually DEFINES a key
            // for this id. This is distinct from "phrase != id": a present-but-equal-to-id phrase
            // (interrupt=interrupt, disembark=disembark) counts as defined and stays in; an id with
            // no key (no localized phrase, e.g. EN-side dead RU-only entries) is excluded.
            if (!AiActionAliasTextProvider.hasKey(SystemSession.getInstance().getLanguage(), id)) {
                continue;
            }
            // Context visibility only applies in real (non-dry-run) mode.
            if (!isDryRun && !action.isVisibleForLLM(status)) {
                continue;
            }
            visible.add(action);
        }

        List<IntelAction> ordered = orderByBefore(visible);

        Map<String, String> map = new LinkedHashMap<>();
        for (IntelAction action : ordered) {
            map.put(StringUtls.localizedAiActionKeys(action.id()), action.id());
        }

        if (conversationalMode) {
            map.put("general conversation", GeneralConversationQuery.ID);
        } else {
            map.put("ignore_nonsensical_input", IgnoreNonsensicalInputCommand.ID);
        }
        map.put(CONNECTION_CHECK_COMMAND, ConnectionCheckQuery.ID);
        CustomCommandRegistry.getInstance().contributeToActionMap(map);

        return map;
    }

    /**
     * Stable topological sort over the {@code before} edges. Base order is by id;
     * an edge {@code X before Y} forces X ahead of Y. Dangling references (target id
     * not among the visible actions) are logged and ignored. A cycle aborts generation
     * with {@link IllegalStateException}. With no edges the result equals the id-sorted base.
     */
    private List<IntelAction> orderByBefore(List<IntelAction> visible) {
        List<IntelAction> base = new ArrayList<>(visible);
        base.sort(Comparator.comparing(IntelAction::id));

        Map<String, IntelAction> byId = new LinkedHashMap<>();
        for (IntelAction action : base) {
            byId.put(action.id(), action);
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String id : byId.keySet()) {
            adjacency.put(id, new ArrayList<>());
            indegree.put(id, 0);
        }

        for (IntelAction action : base) {
            String x = action.id();
            for (String y : beforeOf(action)) {
                if (!byId.containsKey(y)) {
                    log.warn("'before' reference '{}' on action '{}' is not a visible action - edge ignored", y, x);
                    continue;
                }
                adjacency.get(x).add(y);
                indegree.put(y, indegree.get(y) + 1);
            }
        }

        // Kahn's algorithm; ties broken by id (natural String order) for determinism.
        PriorityQueue<String> ready = new PriorityQueue<>();
        for (String id : byId.keySet()) {
            if (indegree.get(id) == 0) {
                ready.add(id);
            }
        }

        List<IntelAction> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.poll();
            result.add(byId.get(id));
            for (String y : adjacency.get(id)) {
                int d = indegree.get(y) - 1;
                indegree.put(y, d);
                if (d == 0) {
                    ready.add(y);
                }
            }
        }

        if (result.size() != base.size()) {
            List<String> cycleNodes = new ArrayList<>();
            for (String id : byId.keySet()) {
                if (indegree.get(id) > 0) {
                    cycleNodes.add(id);
                }
            }
            throw new IllegalStateException("Cycle detected in 'before' ordering constraints among: " + cycleNodes);
        }
        return result;
    }

    /** Reads the {@code before} hints from the registration annotation; empty when absent. */
    private String[] beforeOf(IntelAction action) {
        Class<?> type = action.getClass();
        RegisterCommand rc = type.getAnnotation(RegisterCommand.class);
        if (rc != null) {
            return rc.before();
        }
        RegisterQuery rq = type.getAnnotation(RegisterQuery.class);
        if (rq != null) {
            return rq.before();
        }
        return new String[0];
    }
}
