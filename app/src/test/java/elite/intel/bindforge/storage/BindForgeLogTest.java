package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BindForge log is only worth carrying if it stays BindForge's and stays readable: the shipped {@code log4j2.xml}
 * decides that, so this runs against that file rather than a test's own.
 */
class BindForgeLogTest {

    @Test
    void takesBindForgesInfoLinesAndNothingElse() throws IOException {
        String run = UUID.randomUUID().toString();

        LogManager.getLogger("elite.intel.bindforge.storage.Probe").info("storage {}", run);
        LogManager.getLogger("elite.intel.bindforge.rules.Probe").info("rules {}", run);
        LogManager.getLogger("elite.intel.ui.screen.bindings.Probe").info("screen {}", run);
        // WHY: rules logs at debug to the main log, and reaches this appender by additivity, past its parent's level.
        LogManager.getLogger("elite.intel.bindforge.rules.Probe").debug("chatter {}", run);
        LogManager.getLogger("elite.intel.ai.hands.Probe").error("keystrokes {}", run);

        String log = Files.readString(logFile(), StandardCharsets.UTF_8);
        assertTrue(log.contains("storage " + run), log);
        assertTrue(log.contains("rules " + run), log);
        assertTrue(log.contains("screen " + run), log);
        assertFalse(log.contains("chatter " + run), "debug stays out of it");
        assertFalse(log.contains("keystrokes " + run), "only BindForge's lines");
    }

    private static Path logFile() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        RollingFileAppender appender = context.getConfiguration().getAppender("bindForgeFile");
        return Path.of(appender.getFileName());
    }
}
