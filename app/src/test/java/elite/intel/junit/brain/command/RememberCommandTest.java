package elite.intel.junit.brain.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.IntelActionContext;
import elite.intel.ai.brain.actions.handlers.commands.builtin.RememberCommand;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.CompanionRuntimeGraph;
import elite.intel.ai.brain.vega.CompanionRuntimeTestSupport;
import elite.intel.ai.brain.vega.memory.SessionMemoryGateway;
import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RememberCommandTest {

    private final RememberCommand command = new RememberCommand();
    private SessionMemoryGateway memory;
    private CompanionRuntimeGraph runtimeGraph;
    private Language previousLanguage;

    @BeforeEach
    void installRuntime() {
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(Language.EN);
        memory = new SessionMemoryGateway(() -> null);
        runtimeGraph = CompanionRuntimeTestSupport.install(null, null, null, memory, null, null);
    }

    @AfterEach
    void clearRuntime() {
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
        SystemSession.getInstance().setLanguage(previousLanguage);
    }

    @Test
    void declaresOneRequiredVerbatimTextParameter() {
        assertEquals(RememberCommand.ID, command.id());
        assertEquals(1, command.parameters().size());

        ActionParameterSpec text = command.parameters().get(0);
        assertEquals(RememberCommand.PARAM_TEXT, text.getName());
        assertEquals("string", text.getType());
        assertTrue(text.isRequired());
        assertTrue(text.getExtractionHint().contains("verbatim"));
        text.validate();

        assertTrue(command.isAvailableIn(IntelActionContext.COMPANION_COMMANDER));
        assertFalse(command.isAvailableIn(IntelActionContext.LEGACY_ACTION_MAP));
        assertFalse(command.isAvailableIn(IntelActionContext.GUI));
    }

    @Test
    void storesOnlyTheExactTextArgumentAsSavedText() {
        JsonObject params = new JsonObject();
        params.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra Nine Four");

        assertNull(command.execute(params, "remember that our docking code is Sierra Nine Four"));

        assertEquals(1, memory.savedTextRecords().size());
        assertEquals(MemoryKind.SAVED_TEXT, memory.savedTextRecords().get(0).kind());
        assertEquals("our docking code is Sierra Nine Four",
                memory.savedTextRecords().get(0).entries().get(0).content());
    }

    @Test
    void acceptsAClarificationValueAppendedToTheOriginalTrigger() {
        JsonObject params = new JsonObject();
        params.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra Nine Four");

        assertNull(command.execute(params,
                "remember that\nour docking code is Sierra Nine Four"));

        assertEquals("our docking code is Sierra Nine Four",
                memory.savedTextRecords().getFirst().entries().getFirst().content());
    }

    @Test
    void ignoresALeadingCompanionNameWhenExtractingTheValue() {
        JsonObject params = new JsonObject();
        params.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra Nine Four");
        String companionName = CompanionConfig.companionNameForms().getFirst();

        assertNull(command.execute(params,
                companionName + ", remember that our docking code is Sierra Nine Four"));

        assertEquals("our docking code is Sierra Nine Four",
                memory.savedTextRecords().getFirst().entries().getFirst().content());
    }

    @Test
    void removesTriggerSeparatorPunctuationButPreservesTheRememberedText() {
        JsonObject params = new JsonObject();
        params.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra Nine Four");

        assertNull(command.execute(params,
                "remember that: our docking code is Sierra Nine Four"));

        assertEquals("our docking code is Sierra Nine Four",
                memory.savedTextRecords().getFirst().savedText());
    }

    @Test
    void requestsCanonicalInputForExecutionValidation() {
        assertEquals("remember that my carrier is Indomitable",
                command.executionInput(
                        "remember that my career is Indomitable",
                        "remember that my carrier is Indomitable"));
    }

    @Test
    void ignoresModelRewritesButRejectsInputsWithoutAnExplicitTrigger() {
        JsonObject rewritten = new JsonObject();
        rewritten.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra 94");
        assertNull(command.execute(rewritten, "remember that our docking code is Sierra Nine Four"));
        assertEquals("our docking code is Sierra Nine Four",
                memory.savedTextRecords().getFirst().savedText());

        JsonObject untriggered = new JsonObject();
        untriggered.addProperty(RememberCommand.PARAM_TEXT, "our docking code is Sierra Nine Four");
        assertThrows(IllegalArgumentException.class, () -> command.execute(
                untriggered, "our docking code is Sierra Nine Four"));

        assertEquals(1, memory.savedTextRecords().size());
    }

    @Test
    void rejectsBlankOrStructuredContent() {
        JsonObject blank = new JsonObject();
        blank.addProperty(RememberCommand.PARAM_TEXT, "   ");
        assertThrows(IllegalArgumentException.class, () -> command.execute(blank, ""));

        JsonObject array = new JsonObject();
        array.add(RememberCommand.PARAM_TEXT, new JsonArray());
        assertThrows(IllegalArgumentException.class, () -> command.execute(array, ""));

        JsonObject number = new JsonObject();
        number.addProperty(RememberCommand.PARAM_TEXT, 42);
        assertThrows(IllegalArgumentException.class,
                () -> command.execute(number, "remember 42"));

        assertTrue(memory.savedTextRecords().isEmpty());
    }
}
