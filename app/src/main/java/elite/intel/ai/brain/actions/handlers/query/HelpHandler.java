package elite.intel.ai.brain.actions.handlers.query;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    @Override public JsonObject handle(String action, JsonObject params, String originalUserInput) {

        JsonElement key = params.get("key");
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