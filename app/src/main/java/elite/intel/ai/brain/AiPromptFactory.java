package elite.intel.ai.brain;


public interface AiPromptFactory {
    String generateAnalysisPrompt();
    String generateUserInputSystemPrompt(String rawUserInput);
    String normalizeInput(String rawUserInput);

    String appendCloudBehavior();
    String generateSensorPrompt();
}
