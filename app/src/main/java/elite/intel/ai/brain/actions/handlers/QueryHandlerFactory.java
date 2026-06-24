package elite.intel.ai.brain.actions.handlers;

import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class QueryHandlerFactory {

    private static final Logger log = LogManager.getLogger(QueryHandlerFactory.class);
    private static QueryHandlerFactory instance;
    private final Map<String, IntelQuery> queryHandlers = new HashMap<>();

    private QueryHandlerFactory() {
        // Private constructor for singleton
    }

    public static QueryHandlerFactory getInstance() {
        if (instance == null) {
            instance = new QueryHandlerFactory();
        }
        return instance;
    }


    public Map<String, IntelQuery> registerQueryHandlers() {
        for (Map.Entry<String, IntelQuery> entry : QueryRegistry.getInstance().byId().entrySet()) {
            queryHandlers.put(entry.getKey(), entry.getValue());
        }
        log.info("Registered {} built-in query handler(s) from QueryRegistry", queryHandlers.size());
        return queryHandlers;
    }
}
