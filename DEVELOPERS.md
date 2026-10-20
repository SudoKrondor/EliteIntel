# Elite Intel Developer Guidelines

Welcome to developing for Elite Intel, a Java 21 desktop app (Swing + FlatLaf) that gives *Elite Dangerous*
commanders real-time AI voice interaction. This guide describes the architecture as it stands on `master`
(the current production release) and the rules every contribution must meet.

**Before writing any code,
read [Contribution Rules](#contribution-rules).** Pull requests that do not meet them will be closed without review.

## Package References

- [elite.intel.ai.ears](app/src/main/java/elite/intel/ai/ears/PACKAGE.md) - speech-to-text and audio input
- [elite.intel.ai.mouth](app/src/main/java/elite/intel/ai/mouth/PACKAGE.md) - text-to-speech and playback
- [elite.intel.ai.hands](app/src/main/java/elite/intel/ai/hands/PACKAGE.md) - keystroke simulation and game control dispatch
- [elite.intel.bindforge](app/src/main/java/elite/intel/bindforge/PACKAGE.md) - the `.binds` file and its rules
- [elite.intel.devices](app/src/main/java/elite/intel/devices/PACKAGE.md) - controller / HOTAS polling
- [CODING_STANDARD.md](CODING_STANDARD.md) - design, robustness, structure, error-handling and testing conventions
- [LOCALIZATION_GUIDE.md](LOCALIZATION_GUIDE.md) - adding or editing a language

---

## Contribution Rules

These rules are not negotiable. A pull request that breaks any of them will be closed without review.

### 1. Branch and pull request target

- **All pull requests must target the `V1.2`
  branch.** Base your branch on the current `V1.2` and open the PR against `V1.2`. This applies to bug fixes as well as features.
- Do **not** open pull requests against `master`. `master` holds the released build and only receives release merges.
- Keep each pull request to one concern. Rebase onto the latest `V1.2` before you request a review.

### 2. New features need agreement first

- **A pull request that adds a feature or changes behavior users can see must be agreed with the project maintainer
  before you start
  work.** Explain the proposal on [Matrix](https://matrix.to/#/#krondor:matrix.org), or open a GitHub issue and link it from the PR.
- Feature PRs that were not agreed in advance will be closed, however good the code is.
- Bug fixes do not need prior agreement. They still target `V1.2`, and the PR must say how to reproduce the bug.

### 3. Cross-platform parity (Windows and Linux)

- Elite Intel is supported on **Windows and Linux**. Every change must build, run and behave the same way on both.
- Do not use OS-specific paths, shell commands, path separators or file-system assumptions. Use `java.nio.file`
  and the existing platform helpers.
- Code that differs per OS must have an implementation for
  **both** platforms in the same pull request. A Windows-only or Linux-only feature is not accepted.
- Native code (JNI/JNA) is allowed only for a library that is signed or ships from a trusted upstream, is available for both Windows and Linux, and is bundled with the application (see `distribution/native`).

### 4. Self-contained installation

- **The app must work as installed, with nothing else to set
  up.** A change must not require users to run scripts, install runtimes, interpreters or packages (Python, Node, system libraries, and so on), or configure external services before they can use it.
- Every dependency is either resolved by Gradle and packaged into the build, or shipped in `distribution/`.
- Optional cloud services (LLM, TTS) are set up only through the app's own settings UI, as the existing providers are.

### 5. Updates are delivered by the built-in updater

- **Every change must reach existing installations through the built-in updater (`updater/`)
  alone.** The updater downloads the latest release zip, extracts it over the install directory and relaunches the app. A change that needs a reinstall, a manual step, or the installer to run again is not accepted.
- Everything a change needs at runtime (jars, native libraries, models, resources) must be part of the release package, so that one updater run delivers it whole.
- The updater overwrites and adds files, but **never deletes
  them**. Code must keep working when files from an older release are still in the install directory, and must never depend on a file being absent.
- User data must survive an update unchanged: the databases, settings, encrypted keys and anything else the commander created. Never ship a file that would overwrite user data.
- Schema and data changes are applied by the app on its first start after the update, through new database migrations (see [Database](#database)). Never through an external script, and never by asking users to delete or reset their database.
- Do not change the updater's contract (the release asset name, the install-directory layout, the relaunch mechanism) without the maintainer's agreement. Updater changes must keep working for installations upgrading from the current release.

### 6. Consistent UX and onboarding

- New UI must follow the existing look, layout, terminology and interaction patterns (Swing + FlatLaf, the existing tabs and dialogs). Do not add a new visual style or a parallel settings flow.
- First-run setup goes through the existing setup checks (`elite.intel.setup`). A feature that needs configuration must say so through those checks, in the same way the LLM, journal and bindings checks do.
- Do not add expert toggles to make up for missing logic. Where possible, read the game state and decide for the user.
- All user-facing text is localized through the `resources/i18n` bundles, and every supported language gets the new keys (see [LOCALIZATION_GUIDE.md](LOCALIZATION_GUIDE.md)). Use Claude for translations if you do not know all supported languages.

### 7. Testing

- New behavior and bug fixes come with unit tests (JUnit 5, under `app/src/test`, mirroring the source packages). A bug fix includes a test that fails without the fix.
- **Do not change existing tests just to make the suite
  pass.** A failing test means you broke something, or that the test must change because the agreed behavior changed. If you think a test is wrong, explain why in the PR description. Changing a test's assertions without that explanation gets the PR rejected.
- The full suite must pass: `./gradlew app:test`. Run it all, not only the tests you added.
- Unit tests must not call live cloud services. `local-integration` tests (live LLM) are opt-in and stay out of the default run.
- Unit tests must not be platform specific and pass on headles Linux CI

### 8. No regressions

- **Existing features must keep
  working.** A change must not remove, degrade or silently change existing behavior unless the maintainer agreed to it in advance.

### 9. Database changes: new migrations only

- **Never modify, rename, reorder or delete an existing SQL migration
  file**, whether or not it has shipped yet. Installed databases have already run those files, so editing one leaves those databases different from a fresh install.
- Every schema or data change goes in a
  **new** migration file that follows the existing numbering and directory pattern (see [Database](#database)). That includes fixing a mistake made by an earlier migration.
- A migration must upgrade every existing installation from the previous release without losing data, and it must produce the same result when it runs on a fresh install.

### 10. Hard architectural constraints

- **No reading or writing game memory, and no process
  injection.** Game data comes only from the journal and its companion files (the officially documented third-party API), EDSM/Spansh, and the EDDN relay (subscribe-only).
- **No AFK automation.** Every in-game action must come from a commander's input.
- **No changes to game
  files** apart from the binding edits BindForge makes with the commander's consent. It always takes a backup first and never changes a binding the commander already set.
- API keys are stored encrypted using the existing mechanism (`Cypher`, read through `SystemSession`). Never log them or send them anywhere except the provider they belong to.

### 11. Pull request checklist

- [ ] Branched from and targeting `V1.2`
- [ ] Feature agreed with the maintainer (link the Matrix discussion or issue), or it is a bug fix with repro steps
- [ ] Builds and runs on Windows **and** Linux
- [ ] No extra installation steps for users
- [ ] Delivered by the built-in updater alone. User data survives the update
- [ ] No existing SQL file touched. Database changes are new migrations that follow the numbering pattern
- [ ] UI and onboarding consistent with the existing app, strings localized
- [ ] New or updated unit tests. Existing tests unchanged, or each change explained
- [ ] `./gradlew clean build` and `./gradlew app:test` pass
- [ ] Follows [CODING_STANDARD.md](CODING_STANDARD.md)
- [ ] Cloud integrations: provide a temporary test key (one week) privately to the maintainer

---

## Getting Started

### Prerequisites

| Tool       | Version                  | Notes                                                        |
|------------|--------------------------|--------------------------------------------------------------|
| **Java**   | **21 LTS**               | Required. Tested with Eclipse Temurin / OpenJDK 21.          |
| **Gradle** | **8.14.x** (via wrapper) | Do **not** install Gradle manually - always use `./gradlew`. |
| **Git**    | Any recent               | -                                                            |

> The build uses a Java 21 toolchain. Make sure `JAVA_HOME` points to a JDK 21 installation.

### Checkout

```bash
git clone https://github.com/<your-fork>/EliteIntel.git
cd EliteIntel
git checkout V1.2
```

### Build

```bash
./gradlew clean build          # Full build (both modules)
./gradlew app:build            # Build main app only
./gradlew shadowJar            # Create fat JAR → distribution/elite_intel.jar
```

### Test

```bash
./gradlew app:test                 # JUnit 5 suite; runs against an in-memory SQLite database
./gradlew localIntegrationTest     # Live-LLM routing checks (opt-in; needs a running LLM)
```

### Run

```bash
java -Djava.library.path=distribution/native/sherpa-onnx -jar distribution/elite_intel.jar
```

### IDE Configuration (IntelliJ IDEA)

1. **Open** the project root as a Gradle project (`File → Open`).
2. **SDK**: set the project SDK to Java 21 (`File → Project Structure → Project → SDK`).
3. **Run configuration**: add the following to the **VM options** of the `App` run configuration:
   ```
   -Djava.library.path=distribution/native/sherpa-onnx
   ```
   The local Sherpa-ONNX STT/TTS native libraries need this to load. Without it, local STT and TTS do not start.
4. **Unused-declaration false positives**: see [IDE Note](#ide-note-unused-declaration-false-positives).

---

## Architecture

Elite Intel is a Gradle multi-module project (`app/` is the main application, `updater/` the auto-updater). It is
**entirely
event-driven** on Guava EventBus: modules do not depend on each other directly. Each stage runs on its own thread.

### Data flow

```
Microphone → STT (Parakeet, local) → UserInputEvent
  ↓
VegaSubsystemGate → NormalizedUserInputEvent
  ↓
Reflex (deterministic) ─or─ semantic reducer → LLM tool call → execution gateway
  ↓
Keystroke / query handler / EDSM-Spansh call
  ↓
VocalisationRequestEvent → TTS (Kokoro / Supertonic 3 / Google Cloud) → speaker
```

1. The app watches the journal and its companion files (`Status.json`, `NavRoute.json`, and others) and keeps a local SQLite cache of the relevant data. EDSM and Spansh are queried for specific features. The EDDN relay is read silently in the background to fill a few ledgers.
2. The commander speaks. Parakeet transcribes the audio locally and the text is normalized.
3. A deterministic
   **reflex** resolves a known training phrase (exact, or a damaged transcript of one) without consulting the LLM. It fires only when the phrase names exactly one safe command and supplies every argument that command needs.
4. Otherwise the
   **VEGA** pipeline (`elite.intel.ai.brain.vega`) offers the LLM only the tools visible in the current game context and only the session state relevant to the turn, which keeps the prompt small. The LLM picks a tool (a command or a query) or replies conversationally.
5. Commands usually send a keystroke or a short key sequence through `elite.intel.ai.hands`, which presses whatever the commander's bindings say. Queries gather the data, and the LLM turns it into the spoken answer.

### Core packages

| Package                 | Purpose                                                                                   |
|-------------------------|-------------------------------------------------------------------------------------------|
| `elite.intel.eventbus`  | The buses: `GameEventBus` (game/AI), `UiBus` (Swing), plus audio/device buses             |
| `elite.intel.gameapi`   | Journal parsing and events, pre-scan, EDSM/Spansh clients, EDDN listener                  |
| `elite.intel.ai.brain`  | LLM layer; the VEGA pipeline lives in `ai.brain.vega`                                     |
| `elite.intel.ai.ears`   | STT (Parakeet, local) and audio calibration                                               |
| `elite.intel.ai.mouth`  | TTS (Kokoro and Supertonic 3, local; Google Cloud)                                        |
| `elite.intel.ai.hands`  | Keyboard simulation and game control dispatch                                             |
| `elite.intel.ai.embed`  | Shared ONNX embedding model (reflex and semantic reducer)                                 |
| `elite.intel.bindforge` | The `.binds` file: parse, watch, edit, back up; conflict and auto-assign rules            |
| `elite.intel.devices`   | Game controller / HOTAS polling (push-to-talk, BindForge)                                 |
| `elite.intel.io`        | Generic file mechanics: `AtomicFiles`, `TimestampedBackups`, `FilePoller`                 |
| `elite.intel.ui`        | Swing MVC (`AppView`, `AppController`) and the native HUD overlay (`ui.overlay`)          |
| `elite.intel.db`        | SQLite via JDBI 3 + HikariCP; DAOs and managers                                           |
| `elite.intel.session`   | In-memory game/app state (`PlayerSession`, `SystemSession`, `Status`)                     |
| `elite.intel.setup`     | First-run configuration checks (LLM, journal folder, bindings folder)                     |
| `elite.intel.i18n`      | `Language`; the bundles live in `resources/i18n`                                          |
| `elite.intel.jukebox`   | The commander's own music, ducked under speech                                            |

The HUD overlay is a separate native program (`overlay/`, C). Prebuilt Windows and Linux binaries ship in
`distribution/overlays`, so a Java-only change does not need it rebuilt.

### Event system

- **Publish**: `GameEventBus.publish(new SomeEvent(...))` for game/AI events, `UiBus.publish(...)` for Swing.
- **Subscribe**: annotate a method with `@Subscribe`.
-
**Registration**: `elite.intel.gameapi.SubscriberRegistration` scans its configured packages at startup. Singletons call `GameEventBus.register(this)` in their constructor. A class in a scanned package that must not be on the live bus is marked `@PreScanOnly`.
- **Key events**: `UserInputEvent` (STT output), `NormalizedUserInputEvent` (cleaned phrase),
  `VocalisationRequestEvent` (text to speak), `IsSpeakingEvent` (mic gate), `LoadSessionEvent` (session init),
  `GameEvents.StatusEvent` (`Status.json` telemetry), `AudioMonitorEvent` (audio levels for UI display only).

---

## Extension Points

### Commands and queries

Add a class annotated `@RegisterCommand` under `elite.intel.ai.brain.actions.handlers.commands` (or
`@RegisterQuery` under `...handlers.queries`). The registries scan these packages reflectively. Put localized trigger phrases in `ai_action_aliases*.properties`. A command that destroys stored state the commander cannot get back overrides `isDangerous()`, and VEGA asks for confirmation before running it.

### Journal events

Extend `BaseEvent`, register the event in `EventRegistry`, add a subscriber under
`elite.intel.gameapi.journal.subscribers`, and annotate the handler with `@Subscribe`. Data that a subscriber collects in the background is stored without being announced.

### LLM providers

Implement `LlmProviderAdapter` (request body and response parsing) in `elite.intel.ai.brain.vega.llm`, add the vendor to `ProviderEnum` with its display name, then fill in the exhaustive switches that stop compiling without it (`VegaLlmGatewayFactory.cloudGateway`, `cloudModel`, `ApiFactory.getAnalysisEndpoint`). Adapters already exist for Anthropic, OpenAI, Gemini, Grok, DeepSeek and Mistral (cloud), and LM Studio, the only local host. The provider is a stored setting the commander picks; it is never detected from the key's format.

### STT

Implement `elite.intel.ai.ears.EarsInterface` (a `ManagedService` with `start()` / `stop()`). Run on your own thread and publish `UserInputEvent` when a transcription is ready. Use `AudioCalibrator` for RMS thresholds and
`AudioFormatDetector` for device detection. The current backend is
`elite.intel.ai.ears.parakeet.ParakeetSTTImpl` (offline, sherpa-onnx), which expects 16 kHz mono audio.
`Resampler` handles the conversion.

### TTS

Implement `elite.intel.ai.mouth.MouthInterface`: subscribe to `VocalisationRequestEvent`, claim the request's handle before queueing synthesis work, and support `interruptAndClear()`. Run on your own thread with an internal queue. Current backends:

- `kokoro.KokoroTTS`: primary, offline (sherpa-onnx). No API key.
- `supertonic.SupertonicTTS`: alternative, offline (sherpa-onnx, Supertonic 3, multilingual). Both local engines extend `sherpa.SherpaOnnxTTS`.
- `google.GoogleTTSImpl`: cloud, Google Cloud Text-to-Speech. Needs an API key in the System settings tab.

### HUD overlay cards

Implement `HudObjectiveSource` and register it in `NativeHudOverlay.defaultSources()`. The renderer only understands `HudObjective` / `HudRow`, so the C program does not change.

### VEGA grounding facts

Implement `MemoryFactSource`, annotated `@RegisterMemoryFactSource`, under
`elite.intel.ai.brain.vega.memory.facts.sources`. The facts block is capped (6 facts in total, 2 per source). To add a source, cut an existing one instead of raising the cap.

---

## Database

SQLite via JDBI 3 + HikariCP, with versioned migrations in `app/src/main/resources/db-migration/`.

- **Two
  databases.** The shared `database.db` (settings, catalogues, everything learned about the galaxy) is built from the top level of `db-migration/`. Each commander has their own `cmdr_<FID>.db`, built from
  `db-migration/commander/`. A table lives in exactly one of them.
- Migrations run automatically at startup in filename order (`DatabaseMigrator`), which is how an update reaches each installed database.
- **Never edit an existing migration
  file.** Add a new one. Name it with the next free number in the current release's band: the first two digits are the release (`11XXX` = V1.1, `12XXX` = V1.2), so new V1.2 migrations number from `12000`. The file name is `<number>__<short_description>.sql` (two underscores), for example
  `12003__hud_card_order.sql`. Shared-database migrations go in `db-migration/`, and commander-database migrations go in
  `db-migration/commander/`. Older files numbered before this rule (`000XX`, `010XX`) keep their names.
- **No semicolon inside a migration's comments.** The migrator splits statements before it strips comments.
- Callers go through a **manager** (`MaterialManager`, `ReminderManager`, ...), never a DAO directly.

---

## Licensing

The project is released under CC0 1.0 Universal; see `LICENSE`. Third-party components keep their own licenses and must be listed in `THIRD_PARTY_NOTICES.md` when distribution requires it. Any dependency you add must have a license that allows redistribution in the installer.

## IDE Note: unused-declaration false positives

Some classes are never referenced statically because they are discovered at runtime through reflection: event subscribers (`@Subscribe`), queries (`@RegisterQuery`), commands (`@RegisterCommand`) and fact sources (`@RegisterMemoryFactSource`). IDEs flag them as unused.
**The warning is cosmetic.** The fix is to mark those annotations as entry points.

### IntelliJ IDEA

1. `File > Settings` → `Editor > Inspections` → **Unused declaration**.
2. On the **Entry points** tab, open **Annotations** (`...`) and add:
    - `com.google.common.eventbus.Subscribe`
   - `elite.intel.ai.brain.actions.handlers.queries.RegisterQuery`
   - `elite.intel.ai.brain.actions.handlers.commands.RegisterCommand`

IntelliJ stores these in `.idea/misc.xml`, which is checked in (`.gitignore` ignores `.idea/*` but re-includes
`!.idea/misc.xml`), so everyone gets them on pull. If you add a new reflection-marker annotation, add it to that list and commit `misc.xml`.

### Other IDEs

Mark the same annotations as entry-point annotations in your IDE's unused-code inspection. If it has no such option, ignore the warnings: they do not affect compilation or runtime.

## Contact

- Feature proposals and questions: [Matrix](https://matrix.to/#/#krondor:matrix.org)
- Bugs: open an issue on GitHub
