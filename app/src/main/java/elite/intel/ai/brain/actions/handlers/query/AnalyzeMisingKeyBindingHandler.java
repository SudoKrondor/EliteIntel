package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonObject;
import elite.intel.db.dao.KeyBindingDao.KeyBinding;
import elite.intel.db.managers.KeyBindingManager;
import elite.intel.util.StringUtls;

import java.util.List;

public class AnalyzeMisingKeyBindingHandler extends BaseQueryAnalyzer implements QueryHandler {

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
