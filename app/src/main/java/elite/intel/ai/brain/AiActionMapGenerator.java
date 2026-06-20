package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.actions.query.RegisterQuery;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static elite.intel.ai.brain.commons.AiEndPoint.CONNECTION_CHECK_COMMAND;
import elite.intel.ai.brain.actions.handlers.query.ConnectionCheckQueryCommand;
import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;

/**
 * Builds the LLM action map (phrase-group -> action id) from the self-describing
 * command and query registries, deterministically ordered. Reproduces every block
 * of {@link AiActionsMap#actionMap(boolean)} so it can eventually replace the manual
 * {@code AiActionAliasProvider.addAliases} list.
 * <p>
 * NOT wired into the runtime yet: this is a parallel generator validated by a golden
 * test against the current map. Ordering is id-sorted then refined by the optional
 * {@code before} hints on {@link RegisterCommand}/{@link RegisterQuery} (topological
 * sort); with no hints declared the result is a stable id-sorted order.
 * <p>
 * Composition rule: an action is included only if it has a localized phrase in the
 * alias bundle (i.e. {@code localizedAiActionKeys(id) != id}); ids without a phrase
 * are invisible to the LLM today and stay excluded. The non-action additions
 * (mode fallback, connection-check, custom commands) are appended verbatim after the
 * ordered actions, exactly as the current {@code actionMap}.
 */
public class AiActionMapGenerator {

    private static final Logger log = LogManager.getLogger(AiActionMapGenerator.class);

    /**
     * Assembles the action map for the given context.
     *
     * @param status             session status used for context visibility (ignored when isDryRun)
     * @param isDryRun            true = show everything (visibility not filtered), matching dry-run actionMap
     * @param conversationalMode  selects the fallback entry (general conversation vs ignore)
     * @return ordered phrase-group -> id map (LinkedHashMap)
     */
    public Map<String, String> generate(Status status, boolean isDryRun, boolean conversationalMode) {
        // a) single cross-registry list of self-describing actions
        List<IntelAction> actions = new ArrayList<>();
        actions.addAll(CommandRegistry.getInstance().byId().values());
        actions.addAll(QueryRegistry.getInstance().byId().values());

        // b) composition filter (reproduces the current map's membership)
        List<IntelAction> visible = new ArrayList<>();
        for (IntelAction action : actions) {
            String id = action.id();
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

        // c) deterministic order: id-sorted base refined by 'before' topological sort
        List<IntelAction> ordered = orderByBefore(visible);

        // d) ordered actions into the map
        Map<String, String> map = new LinkedHashMap<>();
        for (IntelAction action : ordered) {
            map.put(StringUtls.localizedAiActionKeys(action.id()), action.id());
        }

        // e) non-action additions, appended after the ordered actions (as in actionMap)
        if (conversationalMode) {
            map.put("general conversation", GeneralConversationQueryCommand.ID);
        } else {
            map.put("ignore_nonsensical_input", IgnoreNonsensicalInputCommand.ID);
        }
        map.put(CONNECTION_CHECK_COMMAND, ConnectionCheckQueryCommand.ID);
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
