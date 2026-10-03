package elite.intel.ai.brain.inference.lmstudio;

import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LMStudioModelListTest {

    @Test
    void theModelListSitsBesideTheStandardChatAddress() {
        assertEquals(Optional.of(URI.create("http://localhost:1234/v1/models")),
                LMStudioClient.modelsUri("http://localhost:1234/v1/chat/completions"));
        assertEquals(Optional.of(URI.create("http://192.168.1.20:4321/v1/models")),
                LMStudioClient.modelsUri(" http://192.168.1.20:4321/v1/chat/completions "));
    }

    @Test
    void anAddressOfAnotherShapeIsNotGuessedAt() {
        assertEquals(Optional.empty(), LMStudioClient.modelsUri("http://localhost:1234/api/generate"));
        assertEquals(Optional.empty(), LMStudioClient.modelsUri(null));
    }

    @Test
    void theIdsAreReadFromTheList() {
        ModelListing listing = LMStudioClient.parseModelList(200,
                "{\"data\":[{\"id\":\"google/gemma-4-e4b\",\"object\":\"model\"},{\"id\":\"qwen/qwen3-8b\"}]}");

        assertEquals(new ModelListing.Listed(Set.of("google/gemma-4-e4b", "qwen/qwen3-8b")), listing);
    }

    @Test
    void anyHttpAnswerMeansTheServerIsUpEvenWhenTheListIsUnreadable() {
        assertInstanceOf(ModelListing.Unknown.class, LMStudioClient.parseModelList(404, "not found"));
        assertInstanceOf(ModelListing.Unknown.class, LMStudioClient.parseModelList(200, "<html>"));
        assertInstanceOf(ModelListing.Unknown.class, LMStudioClient.parseModelList(200, "{\"object\":\"list\"}"));
    }

    @Test
    void aModelMatchesWithOrWithoutItsPublisherPrefixAndIgnoringCase() {
        ModelListing.Listed listed = new ModelListing.Listed(Set.of("google/gemma-4-e4b"));

        assertTrue(listed.includes("google/gemma-4-e4b"));
        assertTrue(listed.includes("Gemma-4-E4B"));
        assertFalse(listed.includes("gemma-4"), "a partial name is a different model");
        assertFalse(listed.includes("qwen/qwen3-8b"));
    }
}
