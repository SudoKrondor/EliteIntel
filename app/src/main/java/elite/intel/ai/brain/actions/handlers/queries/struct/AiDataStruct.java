package elite.intel.ai.brain.actions.handlers.queries.struct;

import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

public class AiDataStruct implements AiData {

    private final String instructions;
    private final ToYamlConvertable data;

    public AiDataStruct(String instructions, ToYamlConvertable data) {
        this.instructions = instructions;
        this.data = data;
    }

    private static final String FORMAT_PREFIX =
            "CRITICAL: This response is read aloud by a text-to-speech engine. " +
                    "Output MUST be plain spoken English only. " +
                    "FORBIDDEN in text_to_speech_response: asterisks, hyphens used as bullets, markdown headers (#), bold (**text**), " +
                    "numbered lists, brackets, colons followed by lists, or any formatting symbols. " +
                    "Two to four sentences maximum. Answer only what was asked. " +
                    "Do not invent, estimate, or assume any data not explicitly present in the provided fields.\n\n";

    @Override public String getInstructions() {
        return FORMAT_PREFIX + instructions;
    }

    @Override public ToYamlConvertable getData() {
        return data;
    }

    @Override public String toYaml() {
        return YamlFactory.toYaml(this);
    }
}
