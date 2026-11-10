package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.util.NvidiaSmi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report answers "why this button, or none" from a bundle alone, so each reading must say which way it went.
 */
class LmStudioSupportTest {

    @Test
    void eachCardIsJudgedAgainstTheBar() {
        String text = LmStudioSupport.graphicsCard(new NvidiaSmi.Answer.Lines(List.of(
                "566.36, NVIDIA GeForce RTX 3090, 24576, 8.6",
                "566.36, NVIDIA GeForce RTX 4080, 16376, 8.9")), true);

        assertTrue(text.contains("23000 MiB and compute capability 8.6"), text);
        assertTrue(text.contains("RTX 3090, 24576, 8.6 - meets the bar"), text);
        assertTrue(text.contains("RTX 4080, 16376, 8.9 - below the bar"), text);
        assertTrue(text.contains("Decided this run, which the button follows: meets the bar"), text);
    }

    @Test
    void noNvidiaSmiIsSaidAsSuch() {
        String text = LmStudioSupport.graphicsCard(new NvidiaSmi.Answer.Nothing(), false);

        assertTrue(text.contains("nvidia-smi: nothing - not on the PATH"), text);
        assertTrue(text.contains("does not meet the bar"), text);
    }

    @Test
    void aMatchingMarkerMatches(@TempDir Path home) throws IOException {
        UUID id = UUID.randomUUID();
        InstallMarker.write(home, id);

        assertEquals("matches", LmStudioSupport.marker(home, id));
    }

    @Test
    void anotherInstallsMarkerIsTold(@TempDir Path home) throws IOException {
        Files.writeString(home.resolve(InstallMarker.FILE), UUID.randomUUID().toString(), StandardCharsets.UTF_8);

        assertTrue(LmStudioSupport.marker(home, UUID.randomUUID()).startsWith("holds another install's id"));
    }

    @Test
    void aMissingMarkerAndAMissingHomeAreToldApart(@TempDir Path tmp) {
        assertTrue(LmStudioSupport.marker(tmp, UUID.randomUUID()).startsWith("missing"));
        assertEquals("the home folder is not there", LmStudioSupport.marker(tmp.resolve("gone"), UUID.randomUUID()));
    }

    @Test
    void theServedModelsAreListedInOrder() {
        assertEquals("answering, serves google/gemma-4-e4b, qwen/qwen3",
                LmStudioSupport.listing(new ModelListing.Listed(Set.of("qwen/qwen3", "google/gemma-4-e4b"))));
        assertEquals("not answering", LmStudioSupport.listing(new ModelListing.NotAnswering()));
    }
}
