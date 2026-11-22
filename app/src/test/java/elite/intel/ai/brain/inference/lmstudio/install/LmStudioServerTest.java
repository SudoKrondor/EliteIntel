package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStart;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadState;
import elite.intel.ai.brain.inference.lmstudio.install.LmStudioServer.DownloadStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LM Studio's answers as smoke testing recorded them (llmster 0.0.27, 2026-11-09).
 */
class LmStudioServerTest {

    @Test
    void aStartedDownloadCarriesItsJob() throws InstallException {
        DownloadStart start = LmStudioServer.parseDownloadStart("""
                {"job_id": "job_384ea067c8", "status": "downloading", "total_size_bytes": 6326908631,
                 "started_at": "2026-11-09T08:22:38.787Z"}""");

        assertEquals(new DownloadStart.Started("job_384ea067c8"), start);
    }

    @Test
    void anAlreadyDownloadedModelHasNoJob() throws InstallException {
        assertEquals(new DownloadStart.AlreadyDownloaded(),
                LmStudioServer.parseDownloadStart("{\"status\": \"already_downloaded\"}"));
    }

    @Test
    void aDownloadAnswerWithoutAJobIsAnError() {
        assertThrows(InstallException.class, () -> LmStudioServer.parseDownloadStart("{\"status\": \"failed\"}"));
        assertThrows(InstallException.class, () -> LmStudioServer.parseDownloadStart("<html>not json</html>"));
    }

    @Test
    void aDownloadInProgressReportsItsBytesSpeedAndFinish() throws InstallException {
        DownloadStatus status = LmStudioServer.parseDownloadStatus("""
                {"job_id": "job_384ea067c8", "downloaded_bytes": 1623388285, "total_size_bytes": 6326908631,
                 "started_at": "2026-11-09T08:22:38.787Z", "bytes_per_second": 105451697.41457711,
                 "estimated_completion": "2026-11-09T08:23:38.96Z", "status": "downloading"}""");

        assertEquals(DownloadState.DOWNLOADING, status.state());
        assertEquals(1_623_388_285L, status.downloadedBytes());
        assertEquals(6_326_908_631L, status.totalBytes());
        assertEquals(105_451_697.41457711, status.bytesPerSecond(), 0.001);
        assertEquals(Optional.of(Instant.parse("2026-11-09T08:23:38.96Z")), status.estimatedCompletion());
    }

    @Test
    void aFailedDownloadKeepsLmStudiosWholeReply() throws InstallException {
        // LM Studio documents no reason field, so a field it sends anyway must still reach the log.
        String json = """
                {"job_id": "job_1", "status": "failed", "downloaded_bytes": 700, "total_size_bytes": 5900,
                 "error": "connection reset"}""";

        DownloadStatus failed = LmStudioServer.parseDownloadStatus(json);

        assertEquals(DownloadState.FAILED, failed.state());
        assertEquals(json.strip(), failed.reply());
    }

    @Test
    void aFinishedOrPausedDownloadHasNoSpeed() throws InstallException {
        DownloadStatus paused = LmStudioServer.parseDownloadStatus("""
                {"job_id": "job_384ea067c8", "downloaded_bytes": 1728357133, "total_size_bytes": 6326908631,
                 "started_at": "2026-11-09T08:22:38.787Z", "status": "paused"}""");

        assertEquals(DownloadState.PAUSED, paused.state());
        assertEquals(0, paused.bytesPerSecond());
        assertTrue(paused.estimatedCompletion().isEmpty());
    }

    @Test
    void anUnknownDownloadStatusIsAnError() {
        assertThrows(InstallException.class,
                () -> LmStudioServer.parseDownloadStatus("{\"status\": \"queued\", \"downloaded_bytes\": 0}"));
    }

    @Test
    void theModelIsLoadedWhenItHasALoadedInstance() throws InstallException {
        String models = """
                {"models": [
                  {"type": "llm", "key": "google/gemma-4-e4b",
                   "loaded_instances": [{"id": "google/gemma-4-e4b", "config": {"context_length": 8192}}]},
                  {"type": "embedding", "key": "text-embedding-nomic-embed-text-v1.5", "loaded_instances": []}
                ]}""";

        assertTrue(LmStudioServer.isLoaded(models, "google/gemma-4-e4b"));
        assertFalse(LmStudioServer.isLoaded(models, "text-embedding-nomic-embed-text-v1.5"));
        assertFalse(LmStudioServer.isLoaded(models, "qwen/qwen3-8b"));
    }

    @Test
    void aDownloadedModelIsListedWhetherOrNotItIsLoaded() throws InstallException {
        String models = """
                {"models": [
                  {"type": "llm", "key": "google/gemma-4-e4b", "loaded_instances": []},
                  {"type": "embedding", "key": "text-embedding-nomic-embed-text-v1.5",
                   "loaded_instances": [{"id": "text-embedding-nomic-embed-text-v1.5"}]}
                ]}""";

        assertTrue(LmStudioServer.isListed(models, "google/gemma-4-e4b"));
        assertTrue(LmStudioServer.isListed(models, "text-embedding-nomic-embed-text-v1.5"));
        assertFalse(LmStudioServer.isListed(models, "qwen/qwen3-8b"));
    }

    @Test
    void theLoadedContextWindowIsRead() throws InstallException {
        String models = """
                {"models": [
                  {"type": "llm", "key": "google/gemma-4-e4b",
                   "loaded_instances": [{"id": "google/gemma-4-e4b",
                     "config": {"context_length": 8192, "parallel": 4, "flash_attention": true}}]},
                  {"type": "embedding", "key": "text-embedding-nomic-embed-text-v1.5", "loaded_instances": []}
                ]}""";

        assertEquals(OptionalInt.of(8192), LmStudioServer.loadedContextLength(models, "google/gemma-4-e4b"));
        assertEquals(OptionalInt.empty(), LmStudioServer.loadedContextLength(models, "text-embedding-nomic-embed-text-v1.5"));
    }

    @Test
    void aToolCallReplyIsRecognised() throws InstallException {
        assertTrue(LmStudioServer.hasToolCall("""
                {"choices": [{"index": 0, "message": {"role": "assistant", "content": "",
                  "tool_calls": [{"type": "function", "id": "bDiP54C5mL3YfmnhQkJNd6Ehc9EvHAfZ",
                    "function": {"name": "deploy_landing_gear", "arguments": "{}"}}]},
                  "finish_reason": "tool_calls"}]}"""));
    }

    @Test
    void aTextReplyIsNotAToolCall() throws InstallException {
        assertFalse(LmStudioServer.hasToolCall("""
                {"choices": [{"message": {"role": "assistant", "content": "Landing gear deployed."}}]}"""));
        assertFalse(LmStudioServer.hasToolCall("{\"choices\": []}"));
    }

    @Test
    void theTestRequestIsShapedAsVegasAre() {
        JsonObject body = LmStudioServer.toolCallTest("google/gemma-4-e4b");

        assertEquals("google/gemma-4-e4b", body.get("model").getAsString());
        assertEquals("required", body.get("tool_choice").getAsString());
        assertEquals("none", body.get("reasoning_effort").getAsString());
        assertFalse(body.get("stream").getAsBoolean());
        assertEquals(1, body.getAsJsonArray("tools").size());
    }
}
