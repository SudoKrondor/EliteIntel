package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryIds;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.db.dao.KeyBindingDao.KeyBinding;
import elite.intel.db.managers.KeyBindingManager;
import elite.intel.util.StringUtls;

import java.util.List;

@RegisterQuery
public class AnalyzeMisingKeyBindingQueryCommand extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return QueryIds.KEY_BINDINGS_ANALYSIS; }


    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) throws Exception {
        KeyBindingManager bindingManager = KeyBindingManager.getInstance();
        List<KeyBinding> missingBindings = bindingManager.getMissingBindings();

        if (!missingBindings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (KeyBinding key : missingBindings) {
                sb.append(key.getKeyBinding()).append(", ");
            }
            return process(StringUtls.localizedLlm("query.bindings.missing", sb.toString()));
        } else {
            return process(StringUtls.localizedLlm("query.bindings.none"));
        }
    }
}
