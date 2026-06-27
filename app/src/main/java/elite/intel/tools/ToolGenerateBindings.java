package elite.intel.tools;

import elite.intel.ai.hands.BindingsLoader;
import elite.intel.ai.hands.KeyBindingsParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class ToolGenerateBindings {
    private static final Logger log = LogManager.getLogger(ToolGenerateBindings.class);
    public static final String FILE_NAME = "KeyboardBindingsMapping.txt";

    public static void main(String[] args) {
        try {
            KeyBindingsParser parser = KeyBindingsParser.getInstance();
            File bindingsFile = new BindingsLoader().getLatestBindsFile();
            Map<String, KeyBindingsParser.KeyBinding> bindings = new TreeMap<>(parser.parseBindings(bindingsFile));

            StringBuilder sb = new StringBuilder();
            sb.append("####################################################################\n");
            sb.append("# Enum cheatsheet. Provides game bindings in form of enum\n");
            sb.append("# Enum name, AI/User facing command, Game Keybind\n");
            sb.append("####################################################################\n\n");
            boolean first = true;
            for (String binding : bindings.keySet()) {
                String userCommand = generateUserFriendlyCommand(binding);
                if (!first) {
                    sb.append(",\n");
                }
                sb.append(String.format("%s(\"%s\")",toEnumName(userCommand), binding));
                first = false;
            }
            
            File outputFile = new File(FILE_NAME);
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(sb.toString().replace("__", "_"));
                log.info("Generated {} at {}", FILE_NAME, outputFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to write {}: {}", FILE_NAME, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to generate GameCommandMapping: {}", e.getMessage());
        }
    }

    private static String generateUserFriendlyCommand(String binding) {
        // Convert CamelCase to snake_case
        StringBuilder result = new StringBuilder();
        boolean lastWasUpper = false;
        for (char c : binding.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (lastWasUpper) {
                    result.append(Character.toLowerCase(c));
                } else {
                    result.append("_").append(Character.toLowerCase(c));
                }
                lastWasUpper = true;
            } else {
                result.append(c);
                lastWasUpper = false;
            }
        }
        String snakeCase = result.toString().replaceAll("^_", "");

        // Simplify verbs and terms
        snakeCase = snakeCase.replace("toggle_", "");
        snakeCase = snakeCase.replace("order_", "request_");
        snakeCase = snakeCase.replace("_open", "");
        snakeCase = snakeCase.replace("hyperspace", "jump_to_hyperspace");
        snakeCase = snakeCase.replace("supercruise", "enter_supercruise");

        return snakeCase;
    }

    private static String toEnumName(String userCommand) {
        return "BINDING_" + userCommand.toUpperCase().replace("_", "_");
    }
}