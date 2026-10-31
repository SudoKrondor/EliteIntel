package elite.intel.setup;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.session.DirectorySetting;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Startup guard for a commander who has not finished setting the app up: it says out loud what is missing,
 * because a new install that silently does nothing is indistinguishable from a broken one.
 * <p>
 * Three separate things are checked, and each speaks on its own — a commander can easily have a working
 * language model and the wrong journal folder:
 * <ul>
 *   <li><b>No language model at all.</b> Points at the free Mistral tier or the local Gemma model and at the
 *       website for instructions - and no further: the microphone calibrates itself on its first start and speaks
 *       up only when it needs attention, so the first thing a new commander hears stays short. Short of that, a cloud setup with <b>no provider
 *       picked</b> - an install upgraded with a key whose format named no provider - is sent to the settings
 *       tab to pick one.</li>
 *   <li><b>No journal files</b> where the app is looking. Without them it knows nothing about the game.</li>
 *   <li><b>No bindings files</b> where the app is looking. Without them it cannot fly anything. This one
 *       splits in three: Elite's own folder holding only a {@code StartPreset} because the commander still runs
 *       a stock preset and has never saved custom controls (fix it in the game, not in the setting); the game's
 *       default folder not there yet, or holding no {@code StartPreset}, because the commander has not played
 *       the game far enough for it to write one (play the training, then save a binding - Alan, 2026-10-24); and
 *       any other folder, which is the wrong one (fix the setting).</li>
 * </ul>
 * <p>
 * Every line is published as a {@link VocalisationRequestEvent} straight to the TTS engine rather than routed
 * through VEGA, for the same reason {@code LocalLlmModelCheck} does it: these warnings are
 * <em>about</em> a missing or broken setup, so voicing them must not depend on the thing that is missing. Local
 * TTS is on by default, so a fresh install can speak before anything is configured.
 */
public class SetupCheck {

    private static final Logger log = LogManager.getLogger(SetupCheck.class);

    /**
     * The prefix Frontier writes on every journal file. Deliberately stricter than the {@code .log} filter the
     * parser reads with: a folder holding unrelated logs and no journals is precisely the wrong-folder mistake
     * this warns about, and the parser would find nothing to say either way.
     */
    private static final String JOURNAL_PREFIX = "Journal";
    private static final String JOURNAL_SUFFIX = ".log";

    private static volatile SetupCheck instance;

    private final BindingsLoader bindingsLoader;
    private final BooleanSupplier llmUnconfigured;
    private final BooleanSupplier cloudProviderMissing;
    private final Supplier<Path> journalDir;
    private final Supplier<Path> bindingsDir;
    private final Supplier<Path> defaultBindingsDir;

    private SetupCheck() {
        this(new BindingsLoader(),
                SetupCheck::isLlmUnconfigured,
                LlmProviderResolver::isCloudProviderMissing,
                () -> PlayerSession.getInstance().getJournalPath(),
                () -> PlayerSession.getInstance().getBindingsDir(),
                DirectorySetting::defaultBindingsPath);
    }

    /**
     * Seam for tests: everything this reads about the install comes through here - including the game's default
     * bindings folder, since the new-player message is for that folder only.
     */
    SetupCheck(BindingsLoader bindingsLoader, BooleanSupplier llmUnconfigured, BooleanSupplier cloudProviderMissing,
               Supplier<Path> journalDir, Supplier<Path> bindingsDir, Supplier<Path> defaultBindingsDir) {
        this.defaultBindingsDir = defaultBindingsDir;
        this.bindingsLoader = bindingsLoader;
        this.llmUnconfigured = llmUnconfigured;
        this.cloudProviderMissing = cloudProviderMissing;
        this.journalDir = journalDir;
        this.bindingsDir = bindingsDir;
    }

    public static synchronized SetupCheck getInstance() {
        if (instance == null) instance = new SetupCheck();
        return instance;
    }

    /**
     * Speaks whatever is missing. Safe to call on every start: a commander who has not fixed it yet still
     * needs telling, and a fully configured install says nothing at all.
     */
    public void check() {
        if (llmUnconfigured.getAsBoolean()) {
            warn("speech.setup.noLlm", "No LLM is configured (no cloud API key and no local model).");
        } else if (cloudProviderMissing.getAsBoolean()) {
            warn("speech.setup.noLlmProvider",
                    "Cloud LLM selected but no provider is chosen. Pick one on the AI Services settings tab.");
        }

        Path journals = journalDir.get();
        if (!hasJournalFiles(journals)) {
            warn("speech.setup.noJournals", "No journal files found in " + journals);
        }

        Path bindings = bindingsDir.get();
        if (!hasBindingsFiles(bindings)) {
            if (isEliteBindingsFolder(bindings)) {
                warn("speech.setup.noSavedBindings", "Bindings folder " + bindings
                        + " is Elite's own, but holds no .binds file: the commander is on a stock preset and has never saved custom controls.");
            } else if (isDefaultBindingsFolder(bindings)) {
                warn("speech.setup.noBindingsYet", "Bindings folder " + bindings
                        + " is the game's default, and is missing or holds no StartPreset: the game has not created the control input configuration files yet, so there is nothing to back up. The commander is told to complete the training and save a binding.");
            } else {
                warn("speech.setup.noBindings", "No .binds files found in " + bindings);
            }
        }
    }

    /**
     * True when this install has no language model set up at all — no cloud key stored and no local model
     * named. Deliberately the conservative reading of "we know for sure this is a fresh installation": a
     * commander who configured either kind, even one they are not currently using, is not a fresh install and
     * is not lectured about it. A selected-but-broken lane is left to the connection check, which can say
     * something far more specific than this can.
     */
    public static boolean isLlmUnconfigured() {
        return llmSetup().nothingConfigured();
    }

    private static LlmSetup llmSetup() {
        SystemSession session = SystemSession.getInstance();
        return new LlmSetup(session.getLmStudioCommandModel(), session.getAiApiKey());
    }

    /**
     * What this install has to talk to a model with. A value object so the rule can be tested without a
     * database or a session singleton behind it.
     */
    record LlmSetup(String localModel, String cloudApiKey) {

        boolean nothingConfigured() {
            return isBlank(localModel) && isBlank(cloudApiKey);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }

    /**
     * True when {@code dir} holds at least one Frontier journal file.
     */
    static boolean hasJournalFiles(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(path -> {
                String name = path.getFileName().toString();
                return name.startsWith(JOURNAL_PREFIX) && name.endsWith(JOURNAL_SUFFIX);
            });
        } catch (IOException unreadable) {
            // An unreadable folder is as useless to us as an empty one, and the commander needs the same fix.
            log.warn("Cannot read journal directory {}: {}", dir, unreadable.getMessage());
            return false;
        }
    }

    /**
     * True when {@code dir} holds at least one {@code .binds} file. Asks {@link BindingsLoader}, which owns
     * what counts as a bindings file, rather than re-deriving the rule and risking a warning that contradicts
     * what the app then reads.
     */
    boolean hasBindingsFiles(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try {
            return !bindingsLoader.listAllBindsFiles(dir).isEmpty();
        } catch (IOException unreadable) {
            log.warn("Cannot read bindings directory {}: {}", dir, unreadable.getMessage());
            return false;
        }
    }

    /**
     * True when {@code dir} is Elite's own bindings folder - it holds a {@code StartPreset.*.start} - even though
     * no {@code .binds} file has been saved into it. Frontier's stock presets (Keyboard, ControlPad, ...) live in
     * the game install, and the game writes a {@code .binds} here only once the commander edits and saves their
     * controls, so this is the "folder is right, controls were never customised" case. It needs a different fix
     * from the wrong-folder mistake, and telling such a commander to correct a folder that is already correct
     * sends them to change the one setting that is not the problem.
     */
    boolean isEliteBindingsFolder(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try {
            return bindingsLoader.findStartPresetFile(dir).isPresent();
        } catch (IOException unreadable) {
            log.warn("Cannot read bindings directory {}: {}", dir, unreadable.getMessage());
            return false;
        }
    }

    /**
     * True when {@code dir} is the game's default bindings folder rather than one the commander chose.
     * <p>
     * A new player's game has not created that folder yet - it appears once they have played through the training -
     * so missing there means "play first", not "set the folder" (Alan, 2026-10-24). A folder the commander chose that
     * is missing or empty is still the wrong-folder case.
     */
    boolean isDefaultBindingsFolder(Path dir) {
        if (dir == null) return false;
        return dir.toAbsolutePath().normalize().equals(defaultBindingsDir.get().toAbsolutePath().normalize());
    }

    /**
     * Speaks the line in the commander's language and records the detail, which is too long to say.
     */
    private void warn(String speechKey, String logDetail) {
        GameEventBus.publish(new VocalisationRequestEvent(
                StringUtls.localizedSpeech(speechKey),
                MissionCriticalAnnouncementEvent.class, false));
        UiBus.publish(new AppLogEvent(logDetail));
        log.info("Setup check: {}", logDetail);
    }
}
