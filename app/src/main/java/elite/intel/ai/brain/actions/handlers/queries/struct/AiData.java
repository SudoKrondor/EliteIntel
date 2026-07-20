package elite.intel.ai.brain.actions.handlers.queries.struct;


import elite.intel.util.yaml.ToYamlConvertable;

public interface AiData extends ToYamlConvertable {
    String getInstructions();
    ToYamlConvertable getData();
}
