package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.db.dao.HelpDao;
import elite.intel.db.util.Database;
import elite.intel.util.StringUtls;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.ArrayList;
import java.util.List;

public class HelpHandler extends BaseQueryAnalyzer implements IntelQuery {

    @Override public String id() { return "help_handler"; }

    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", false,
                "The help topic / feature the commander wants help with, e.g. mining, trade, bindings.",
                List.of("mining", "trade"),
                "Extract the topic keyword from the question; otherwise omit it.");
        key.validate();
        return List.of(key);
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) {

        JsonElement key = params.get(PARAM_KEY);
        String topic = key == null ? null : key.getAsString();
        if (topic == null) {
            return process(StringUtls.localizedLlm("query.help.noTopic"));
        }

        List<String> data = Database.withDao(HelpDao.class, dao -> {
            List<String> list = new ArrayList<>();
            String[] queries = topic
                    .replaceAll("the", "")
                    .replace("_", " ")
                    .split(" ");

            for (String q : queries) {
                List<HelpDao.HelpEntity> help = dao.getHelp(q, q);
                for (HelpDao.HelpEntity h : help) {
                    list.add(h.getHelpText());
                }
            }
            return list;
        });

        StringBuilder sb = new StringBuilder();
        for (String helpText : data) {
            sb.append(helpText).append("\n\n");
        }

        return process(sb.toString());

    }

    record DataDto(List<String> data) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}