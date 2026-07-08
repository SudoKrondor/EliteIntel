package elite.intel.util.json;

import com.google.gson.annotations.SerializedName;

public record LlmMetadata(
        String model,
        @SerializedName("usage") Usage usage
) {
    private static final String UNAVAILABLE = "n/a";

    public String model() {
        return model;
    }

    /**
     * Null-safe one-line rendering for the diagnostics log: the model (or {@code "n/a"} when the response
     * carried none, e.g. a connection check that returns no model/usage), plus the token usage only when
     * present. Static so a null {@code meta} itself renders as {@code "n/a"} instead of the literal "null".
     */
    public static String describe(LlmMetadata meta) {
        if (meta == null) {
            return UNAVAILABLE;
        }
        String modelName = meta.model != null ? meta.model : UNAVAILABLE;
        return meta.usage != null ? modelName + " >" + meta.usage : modelName;
    }

    @Override public String toString() {
        return describe(this);
    }

    public record Usage(
            @SerializedName("prompt_tokens") int promptTokens,
            @SerializedName("completion_tokens") int completionTokens,
            @SerializedName("total_tokens") int totalTokens,
            @SerializedName("prompt_tokens_details") TokenDetails promptDetails,
            @SerializedName("completion_tokens_details") TokenDetails completionDetails

    ) {
        @Override public String toString() {
            return " | Prompt Tokens: " + promptTokens +
                   " | Completion: " + completionTokens +
                    (promptDetails != null ? " | Cached: " + promptDetails.cachedTokens : "") +
                   " | Total: " + totalTokens;
        }

        public record TokenDetails(
                @SerializedName("text_tokens") int textTokens,
                @SerializedName("cached_tokens") int cachedTokens,
                @SerializedName("reasoning_tokens") int reasoningTokens,
                @SerializedName("accepted_prediction_tokens") int acceptedPredictionTokens,
                @SerializedName("rejected_prediction_tokens") int rejectedPredictionTokens
        ) {

        }
    }
}