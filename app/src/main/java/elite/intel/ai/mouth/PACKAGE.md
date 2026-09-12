# `elite.intel.ai.mouth` - Developer Reference

The mouth package owns everything from a
`VocalisationRequestEvent` to speaker output. It normalises the various vox event types produced by other packages, synthesises speech via offline Kokoro or Supertonic 3, Google Cloud, or the Edge consumer Read Aloud service, and publishes an authoritative playback lifecycle while STT remains active for barge-in.

---

## Pipeline Overview

```
AiVoxResponseEvent  NavigationVocalisationEvent  RadioTransmissionEvent  …
        │                       │                        │
        └───────────────────────┴──────┬─────────────────┘
                                       ▼
                          [VocalisationRouter]
                          - normalise all types to VocalisationRequestEvent
                          - gate: optional per event type (radar, discovery, …)
                          - RadioTransmissionEvent: pick random non-session voice, isRadio=true
                          - every request carries one request-scoped VocalisationHandle
                                       │
                          VocalisationRequestEvent (main EventBus)
                                       │
                        ┌─────────────┴──────────────┐
                        ▼                            ▼
              [KokoroTTS]                   [GoogleTTSImpl]
              (offline, sherpa-onnx)        (Google Cloud TTS API)
                        │                            │
              synthesisQueue               ttsQueue (synthesis)
              KokoroTTS-Synthesis          TTSThread
              - split sentences            - split sentences
              - generate() via sherpa-onnx - Google Cloud API call
              - RadioFilter (if isRadio)   - 24kHz LINEAR16
              - AudioDeClicker.sanitize()        │
                        │                   vocalizationQueue
              playbackQueue                VocalizationThread
              KokoroTTS-Playback           - SourceDataLine.write()
              - SourceDataLine.write()     - VocalisationSuccessfulEvent
                        │                            │
                        └─────────────┬──────────────┘
                                       ▼
                                   Speaker
                          handle completion updates shared IsSpeakingEvent state
```

`EdgeTTSImpl` is a third main-mouth branch with the same synthesis/playback queue shape as Google. Its transport
returns MPEG, which is decoded and validated before the shared PCM sanitation, volume, and playback stages.

---

## 1. Event Taxonomy

All vox events are subclasses of a base vox event.
`VocalisationRouter` is the single subscriber for all of them and normalises everything to
`VocalisationRequestEvent`.

| Event | Always routed? | canBeInterrupted | isRadio | Notes |
|---|---|---|---|---|
| `AiVoxResponseEvent` | Yes | true (default) | false | LLM spoken answer; optional `CompletableFuture<Void>` for SPEAK commands |
| `MissionCriticalAnnouncementEvent` | Yes | false | false | High-priority; not gated by settings |
| `AiVoxDemoEvent` | Yes | true | false | UI voice preview; bypasses all session checks |
| `NavigationVocalisationEvent` | Yes | true | false | Jump/route announcements |
| `RadarContactAnnouncementEvent` | Setting-gated | true | false | Suppressed if radar voice disabled |
| `DiscoveryAnnouncementEvent` | Setting-gated | true | false | Suppressed if discovery voice disabled |
| `MiningAnnouncementEvent` | Setting-gated | true | false | Suppressed if mining voice disabled |
| `RouteAnnouncementEvent` | Setting-gated | true | false | Suppressed if route voice disabled |
| `RadioTransmissionEvent` | Yes | true | **true** | Random non-session voice; simulates NPC radio |

`TTSInterruptEvent` is handled directly by each backend; `VocalisationRouter`
does not touch it.

### `VocalisationRequestEvent` fields

| Field | Type | Meaning |
|---|---|---|
| `originType` | `Class<?>` | The original event class; used by Google to publish `VocalisationSuccessfulEvent` via reflection |
| `voiceName` | `String` (nullable) | Override voice; null = use session-default voice |
| `canBeInterrupted` | `boolean` | Whether `TTSInterruptEvent` mid-playback should abort this utterance |
| `isRadio` | `boolean` | Apply `RadioFilter` during synthesis |
| `handle` | `VocalisationHandle` | Request id, interruptibility, ownership, and exactly-once completion |
| `completionFuture` | `CompletableFuture<Void>` | The handle's non-null future; completed after final playback or another terminal outcome |

---

## 2. `VocalisationRouter`

`VocalisationRouter` is the central normaliser. It subscribes to every vox event type and converts each to a
`VocalisationRequestEvent` on the main EventBus.

**`AiVoxResponseEvent` special handling**: If the event carries a
`CompletableFuture`, `VocalisationRouter` passes it through to `VocalisationRequestEvent`; otherwise the request creates its own future. The eligible active Mouth claims the request's `VocalisationHandle` during the same EventBus dispatch. Guava queues reentrant posts, so the no-Mouth check runs through `GameEventBus.afterCurrentDispatch` only after the outer post has drained; checking immediately after a nested `publish` would reject the request before a Mouth sees it. Only the handle publishes `IsSpeakingEvent`, using a process-wide active-request count, so overlapping requests cannot report a false idle state. STT continues listening while the state is true and treats a commander transcript as barge-in.

**`RadioTransmissionEvent` special handling**: The router sets `isRadio=true` on the resulting
`VocalisationRequestEvent`; the engine `RadioVoicing` names draws a random voice of its own cast other than the current ship voice. Cloud mouths ignore it; a local engine owns this route, picked by the
**game
client's** language (`GameLanguage`, off the journal header), because the words are the client's own prose: Kokoro for every client but the Russian one, Supertonic for that. When it is not the main mouth it runs as a dedicated `RADIO_MOUTH` service - Kokoro beside a Supertonic main under a Latin-script client; the reverse never arises, because a Russian client withdraws Kokoro as the main mouth too (`TtsProvider.forSession`: Kokoro must voice BOTH the commander's language and the client's, else Supertonic is the local engine). A radio task also carries the client's language into `generate(...)` (`SherpaOnnxTTS.languageOf`), and a RADIO-role engine is built for it; a client relaunched in another language restarts the mouth (`FileheaderEventSubscriber`).

---

## 3. `MouthInterface` - The Extension Point

```java
public interface MouthInterface extends ManagedService {
    void interruptAndClear();

    @Subscribe
    void onVoiceProcessEvent(VocalisationRequestEvent event);
}
```

An eligible running backend must call `event.handle().claimForPlayback()` before enqueueing work. A backend that does not own the event (Google for radio, or radio-role Kokoro for main speech) leaves it unclaimed for the correct backend. A claimed handle must be completed or failed on every terminal path.

`ManagedService` provides `start()` and `stop()`. Implementations initialize their engine/workers and register
on `GameEventBus` during `start()`, then unregister and settle owned handles during `stop()`.

Current implementations: `KokoroTTS` and `SupertonicTTS` (offline), `GoogleTTSImpl` (cloud), and `EdgeTTSImpl` (cloud).

---

## 4. The sherpa-onnx pipeline (`sherpa/`) and the Kokoro backend (`kokoro/`)

### Overview

`sherpa/SherpaOnnxTTS` is the abstract pipeline every local engine runs on: the two queues below, the synthesis and playback threads, interrupts, the persistent line, the MAIN / RADIO `Role` and the
`MainVoicePlaybackGate` ducking. It lives once, so the two engines cannot drift apart on interrupt semantics. A concrete engine supplies only what its model needs: `buildOfflineTts(language)`, whether it
`rebuildsOnLanguageSwitch()`, `generate(...)`, the `sentenceBoundary()` regex, and its voice cast (`defaultVoiceName`, `isInTheCast`, `sidOf`, `radioVoiceNameFor`). `sherpa/RadioVoiceDraw` is the one copy of the radio voice draw; each cast enum hands it `values()`.

`KokoroTTS` is a singleton on that pipeline. It uses the `kokoro-multi-lang-v1_0` ONNX model loaded from
`AppPaths.getTtsModelDir()` via the sherpa-onnx JNI library. Output sample rate is 24000 Hz, 16-bit mono.

### Two-Queue Pipeline

```
onVoiceProcessEvent()
    │  split text into sentences
    │  push SynthesisTask per sentence → synthesisQueue (BlockingQueue)
    │
KokoroTTS-Synthesis thread (daemon)
    │  pop SynthesisTask
    │  resetNumericLocale()
    │  tts.generate(text, sid, speed) → float[] samples → PCM bytes
    │  if isRadio: RadioFilter.apply(pcm)
    │  AudioDeClicker.sanitize(pcm, fadeMs)   ← fade-in to suppress pop
    │  push PlaybackTask → playbackQueue (BlockingQueue)
    │
KokoroTTS-Playback thread (daemon)
    │  pop PlaybackTask
    │  SourceDataLine.write(pcm)  ← persistent line, never closed between sentences
    │  on last sentence: complete CompletableFuture
```

The `SourceDataLine` is opened once in
`start()` and kept open for the session lifetime. Closing and reopening between sentences causes audible pops and delays.

### Critical Constraint - Never Call `tts.release()`

`tts.release()` **must not** be called in `stop()`. The `KokoroMultiLangLexicon`
destructor in sherpa-onnx has a SIGSEGV that crashes the JVM when
`release()` is called after the model has been used. Language changes (which require a new model instance) are handled by rebuilding the
`OfflineTts` object at a safe point in
`start()` rather than releasing it at runtime.

### LC_NUMERIC Locale Fix (`resetNumericLocale()`)

ONNX Runtime calls `setlocale(LC_ALL, "")` during its initialization, which on some systems sets
`LC_NUMERIC` to a locale using `,` as the decimal separator
(e.g. French). espeak-ng, invoked internally by sherpa-onnx's `generate()`, uses
`stof()` to parse floats - which hard-crashes if `LC_NUMERIC` is not `"C"`.
`resetNumericLocale()` sets it back to `"C"` via JNA before every `generate()`
call. This must not be removed.

### Sentence Splitting

```java
"(?<=[.,!?])\\s+(?=\\S)"
```

Commas are included as sentence boundaries (unlike Google). This matches the natural rhythm of comma-heavy text and distributes synthesis load across shorter chunks. The
`CompletableFuture` is attached only to the **last** sentence's
`PlaybackTask`.

### Language to langCode Mapping

```
kokoroLangCode(Language):
  EN  →  "en-us"
  FR  →  "fr"
  ES  →  "es"
  (all others)  →  null  →  speak with English accent
```

When the langCode is null, the model synthesises in English regardless of session language. This is a deliberate fallback - Kokoro Multi-Lang v1.0 only has native accent support for these three languages.

Cyrillic has no entry at all: Kokoro's phonemizer cannot read it, so `TtsProvider.forLanguage` never lets Kokoro be the engine for a Russian or Ukrainian commander - Supertonic stands in (next section).

### Interruption

`interruptAndClear()`:

1. Drain `synthesisQueue` and `playbackQueue`, completing any pending futures.
2. Set `interruptRequested` atomic flag.
3. Call `line.stop()`, `line.flush()`, `line.start()` to silence the speaker mid-word.
4. Clear the flag.

The `canBeInterrupted` field on `VocalisationRequestEvent` controls whether
`TTSInterruptEvent` triggers this. Mission-critical announcements set it to false and are immune to interrupt.

---

## 4a. Supertonic TTS Backend (`supertonic/`)

`SupertonicTTS` is the alternative local engine, the second `SherpaOnnxTTS` subclass: the same two-queue pipeline, the same `Role` (MAIN / RADIO), the same `MainVoicePlaybackGate` ducking and the same never-`release()` rule, because all of that is the base class. It runs the `sherpa-onnx-supertonic-3-tts-int8-*` model from `AppPaths.getTtsModelDir()` through the same JNI.

What differs:

- **One multilingual model.** The language is not baked into the model config; each
  `generateWithConfigAndCallback` call passes it as `GenerationConfig.extra("lang", supertonicLangCode(language))`
  (`en`, `de`, `fr`, `es`, `it`, `pt` for both `PT` and `PTBZ`, `ru`, `uk`). The engine is built once and never rebuilt on a language switch.
- **Cyrillic.** Supertonic reads Russian and Ukrainian natively. That is why it exists here: `TtsProvider.forLanguage`
  substitutes it for Kokoro in the Cyrillic locales (the settings panel greys the Kokoro segment out there), and
  `RadioVoicing` hands it the radio channel whenever the game client is Russian, whatever the main mouth.
- **Ten
  voices** (`SupertonicVoices`, `M1`-`M5`, `F1`-`F5`, by `sid`), all offered; the fleet grid shows them by enum name. `DEFAULT_VOICE` is the ship default and the collapse target for a stored name the cast does not carry.
- **44.1 kHz native
  output**, resampled to the pipeline's `SAMPLE_RATE` (24 kHz) before the declicker, the radio filter and the playback line, so nothing downstream sees a second rate.
- **Sentence split on `.!?` only** - comma clauses stay together so the model's own prosody carries the pause.

Selecting it is a stored setting like any other engine (`game_session.ttsProvider = SUPERTONIC`), written by the LOCAL column's engine switch in the AI services settings. Kokoro remains the shipped default and the fallback for an unreadable stored value.

## 5. Google TTS Backend (`google/`)

### Overview

`GoogleTTSImpl` is a singleton that calls the Google Cloud TTS API using an API key from
`systemSession.getTtsApiKey()`. Audio is returned as 24kHz LINEAR16 PCM. The API key is never logged and must not be transmitted outside the Cloud TTS endpoint.
Legacy `Chirp-HD` voices receive plain-text input because their API rejects SSML; Chirp3-HD and Standard voices
retain punctuation-aware SSML pauses.

### Queue Pipeline

```
onVoiceProcessEvent()
    │  text preprocessing (replacements)
    │  split sentences
    │  push SynthesisRequest per sentence → ttsQueue (BlockingQueue)
    │
TTSThread (daemon)
    │  pop SynthesisRequest
    │  build Google Cloud TTS request + VoiceSelectionParams
    │  HTTP call → 24kHz LINEAR16 PCM bytes
    │  push to vocalizationQueue (BlockingQueue)
    │
VocalizationThread (daemon)
    │  pop PCM bytes
    │  SourceDataLine.write()
    │  publish VocalisationSuccessfulEvent (via reflection on originType)
```

### Text Preprocessing

`processVoiceRequest()` applies these substitutions before synthesis:

- `"present"` → `"detected"` (avoids "weapons present" being read oddly)
- `"_"` → `" "` (underscores from internal identifiers)
- `"*"` → `""` (markdown emphasis stripped)

### Sentence Splitting

```java
"(?<=[.!?])\\s+(?=\\S)"
```

Commas are not boundaries (unlike Kokoro). Google's Chirp3-HD models handle longer sentences more naturally.

### Language Override in `GoogleVoiceProvider`

For non-English sessions, `getVoiceParams()` ignores the named-voice `voiceMap`
entirely and substitutes a language-specific Google Standard voice:

| Language | Male voice | Female voice |
|---|---|---|
| EN | Chirp3-HD/Chirp-HD (named, per `GoogleVoices` enum) | same |
| FR | `fr-FR-Standard-G` | `fr-FR-Standard-E` |
| ES | `es-ES-Standard-B` | `es-ES-Standard-E` |
| DE | `de-DE-Standard-H` | `de-DE-Standard-G` |
| IT | `it-IT-Standard-C` | `it-IT-Standard-A` |
| PT | `pt-PT-Standard-B` | `pt-PT-Standard-A` |
| RU | `ru-RU-Standard-B` | `ru-RU-Standard-E` |
| UK | `uk-UA-Standard-B` | `uk-UA-Standard-B` |

Gender is resolved from
`GoogleVoices.isMale()` on the current session voice. This means non-English users keep their chosen voice's gender but get a locale- appropriate model.

### `VocalisationSuccessfulEvent`

Published after each sentence via
`Class.forName(originType.getName())` reflection to reconstruct the original event class. Subscribers use this to track per-sentence TTS completion (e.g., for UI feedback).

---

## 5A. Edge Read Aloud Backend (`edge/`)

`EdgeTTSImpl` follows the same two-queue and `VocalisationHandle` contract as the other mouths. The transport
uses the consumer Read Aloud HTTP voice-list endpoint and WebSocket synthesis protocol. Escaped text is capped
at 4096 UTF-8 bytes per request, MPEG frames are assembled before `EdgeMp3Decoder` converts them to 24 kHz,
mono, signed PCM-16 little endian, and only that decoded PCM reaches `AudioDeClicker`.

Application speech speed drives Edge's SSML prosody rate. SSML volume remains `+0%`; the application volume is
applied exactly once to decoded PCM through `AudioDeClicker.applyVolume`. Edge is a main-mouth provider only; radio stays on the local-engine route.

The integration is unofficial and is not supported or endorsed by Microsoft. `dev.mccue:jlayer-decoder` is
packaged under its own LGPL terms; see the repository's `THIRD_PARTY_NOTICES.md`.

---

## 6. Audio Utilities

### `AudioDeClicker`

Static utility operating on PCM-16 LE at 24000 Hz.

**`sanitize(byte[] pcm, int fadeMs)`
** - called by the synthesis thread before pushing to playback. Applies a linear fade-in over
`fadeMs` milliseconds at the start of each sentence chunk. This prevents the hard onset click caused by a
`SourceDataLine` transitioning from silence to a non-zero sample.

**`applyVolume(byte[] pcm, float gain)`** - scales every sample by `gain` in
[0.0, 1.0] with clamp to signed 16-bit range. Used for volume ramping during interrupts or gain normalisation passes.

The `removeClicks` method exists but is commented out; only `applyFade` is active.

### `RadioFilter`

Static utility. Applies a shortwave radio transmission effect in-place to a PCM-16 LE buffer at 24000 Hz mono. Called from the synthesis thread (after
`generate()`, before `AudioDeClicker.sanitize()`) when `isRadio=true`.

**Processing chain**:

1. Butterworth highpass biquad (fc=300 Hz, Q=0.707) - removes bass and voice fundamental
2. Butterworth lowpass biquad (fc=5500 Hz, Q=0.707) - retains sibilance and upper harmonics
3. Light static noise (NOISE_AMPLITUDE=50f, ~0.15% of full scale)
4. GAIN=1.4 compensation for energy lost through the bandpass

The biquad coefficients are precomputed constants (see class header for derivation). Implemented as direct-form II transposed biquad for numerical stability.

---

## 7. Voice Catalogs

### `KokoroVoices` (53 voices)

Each voice has a `sid` (speaker ID 0-52) passed to `tts.generate()`. Voice names are prefixed by accent/gender code:

| Prefix | Accent | Gender |
|---|---|---|
| `af_` / `am_` | American English | Female / Male |
| `bf_` / `bm_` | British English | Female / Male |
| `ef_` / `em_` | (European Spanish-accented) | Female / Male |
| `ff_` | French-accented | Female |
| `hf_` / `hm_` | (unspecified) | Female / Male |
| `if_` / `im_` | Italian-accented | Female / Male |
| `jf_` / `jm_` | Japanese-accented | Female / Male |
| `pf_` / `pm_` | (unspecified) | Female / Male |
| `zf_` / `zm_` | (unspecified) | Female / Male |

Default voice: `GEORGE` (sid=26).

### `GoogleVoices` (11 voices)

| Name | Language | Model | Gender |
|---|---|---|---|
| ANNA | en-GB | Chirp-HD-F | Female |
| EMMA | en-US | Chirp3-HD-Despina | Female |
| JAKE | en-US | Chirp3-HD-Iapetus | Male |
| JAMES | en-AU | Chirp3-HD-Algieba | Male |
| JENNIFER | en-US | Chirp3-HD-Sulafat | Female |
| JOSEPH | en-US | Chirp3-HD-Sadachbia | Male |
| MARY | en-US | Chirp3-HD-Zephyr | Female |
| MICHAEL | en-US | Chirp3-HD-Charon | Male |
| OLIVIA | en-GB | Chirp3-HD-Aoede | Female |
| RACHEL | en-US | Chirp3-HD-Zephyr | Female |
| STEVE | en-US | Chirp3-HD-Algenib | Male |

Default: `JENNIFER`.

### `GoogleVoiceProvider`

Singleton, implements `VoiceProvider<VoiceSelectionParams>`.

- `getUserSelectedVoice()` - reads `SystemSession.getGoogleVoice()`; falls back to `JENNIFER`.
- `getRandomVoice()` - picks any `GoogleVoices` that is not the current session voice; used by `VocalisationRouter` for
  `RadioTransmissionEvent`.
- `getVoiceParams(voiceName)` - accepts either the enum constant name (`EMMA`) or the display name (
  `Emma`); applies the non-EN language override (see Section 5) before falling back to the static `voiceMap`.

---

## 8. `VocalisationHandle` Completion Contract

The `SPEAK` custom command blocks the command executor thread on the handle's `CompletableFuture<Void>`:

1. Every `VocalisationRequestEvent` owns one non-null `VocalisationHandle`; an optional caller future is reused.
2. Exactly one eligible Mouth claims the handle synchronously before queue admission. If publication returns unclaimed, the future fails immediately instead of hanging without a Mouth.
3. Every sentence task carries the same handle and marks only the final sentence as terminal. Successful final playback completes it.
4. Blank text after sanitization, synthesis/device/playback errors, cancellation, queue interruption, and service stop all settle the handle exactly once.
5. Targeted cancellation uses `requestId`; urgent speech and barge-in interrupt all interruptible requests.
6. The first claimed handle publishes `IsSpeakingEvent(true)` and the last settled handle publishes `false`. STT uses this only to identify barge-in and never disables recognition.

---

## 9. Adding a New TTS Backend

1. Create a class (or sub-package) and implement `MouthInterface`.
2. Register on `GameEventBus` only after successful initialization in `start()`, and unregister in `stop()`.
3. In `start()`, open a
   `SourceDataLine` at 24000 Hz, 16-bit, mono, signed LE. Keep the line open for the session lifetime; do not reopen per sentence.
4. In `onVoiceProcessEvent()`:
    - Ignore events owned by another backend, then claim `event.handle()` before queueing.
    - Split text with the sentence regex.
    - If `event.isRadio()`, apply `RadioFilter.apply(pcm)` after synthesis.
    - Call `AudioDeClicker.sanitize(pcm, fadeMs)` on each sentence chunk before write.
    - Carry the handle through every task and complete it after the last sentence.
5. Implement `interruptAndClear()`: settle interruptible handles, drain queues, and flush the `SourceDataLine`.
6. Subscribe to `TTSInterruptEvent`: a non-null `requestId` cancels only that handle; a global event settles all
   interruptible queued/current handles and flushes active interruptible playback.
7. Wire the new implementation in `ApiFactory` / `AppController` alongside the existing provider selection logic.

---

## Key Classes - Quick Reference

| Class | Role |
|---|---|
| `MouthInterface` | Extension point for TTS backends |
| `VocalisationHandle` | Request ownership, correlation, completion, and authoritative speaking-state count |
| `subscribers/VocalisationRouter` | Normalises all vox events to `VocalisationRequestEvent` |
| `sherpa/SherpaOnnxTTS` | The offline pipeline both local engines run on: queues, threads, interrupts, line, roles |
| `sherpa/RadioVoiceDraw` | The radio voice draw, generic over a cast enum |
| `kokoro/KokoroTTS` | Offline backend; sherpa-onnx kokoro-multi-lang-v1_0 |
| `kokoro/KokoroVoices` | 53 Kokoro voice enum with sid values |
| `supertonic/SupertonicTTS` | Alternative offline backend; sherpa-onnx Supertonic 3 (int8), the Cyrillic-capable one |
| `supertonic/SupertonicVoices` | 10 Supertonic voice enum with sid values |
| `RadioVoicing` | Which engine voices radio, by the game client's language: Kokoro, or Supertonic for a Russian client |
| `TtsProvider` | The stored engine choice; `forSession` swaps Kokoro for Supertonic when the commander OR the game client is Cyrillic |
| `google/GoogleTTSImpl` | Cloud backend; Google Cloud TTS API |
| `google/GoogleVoices` | 11 Google voice enum with gender and Chirp3-HD character |
| `google/GoogleVoiceProvider` | Voice selection + non-EN language override |
| `edge/EdgeTTSImpl` | Edge Read Aloud queueing, decoded-PCM volume, playback, and request lifecycle |
| `edge/EdgeReadAloudClient` | HTTP/WebSocket protocol, clock-skew retry, timeouts, and cancellation |
| `edge/EdgeMp3Decoder` | MPEG to validated 24 kHz mono PCM-16 LE decoding |
| `google/VoiceProvider<T>` | Interface for voice provider implementations |
| `AudioDeClicker` | Fade-in + volume scaling on PCM-16 LE |
| `RadioFilter` | Bandpass + static noise shortwave radio effect |
| `subscribers/events/VocalisationRequestEvent` | Normalised TTS event (origin, voice, flags, handle) |
| `subscribers/events/AiVoxResponseEvent` | LLM spoken answer; carries optional CompletableFuture |
| `subscribers/events/TTSInterruptEvent` | Global or request-id-targeted interrupt signal |
| `subscribers/events/VocalisationSuccessfulEvent` | Per-sentence completion event (Google and Edge) |

## Key Constants

| Constant | Value | Location |
|---|---|---|
| `SAMPLE_RATE` | `24000` Hz | `SherpaOnnxTTS`, `GoogleTTSImpl`, `AudioDeClicker` |
| Default Kokoro voice | `ISABELLA` | `KokoroVoices.DEFAULT_VOICE` |
| Default Supertonic voice | `F1` (sid=0) | `SupertonicVoices.DEFAULT_VOICE` |
| Default Google voice | `JENNIFER` | `GoogleVoiceProvider` |
| Edge escaped-text limit | `4096` UTF-8 bytes | `EdgeSentenceSplitter` |
| Default Edge voice | `en-US-EmmaMultilingualNeural` | `EdgeVoices` |
| `NOISE_AMPLITUDE` | `50f` (~0.15% full scale) | `RadioFilter` |
| `GAIN` | `1.4f` | `RadioFilter` |
| HP cutoff | `300 Hz` | `RadioFilter` |
| LP cutoff | `5500 Hz` | `RadioFilter` |
| Kokoro sentence split | `(?<=[.,!?])\s+(?=\S)` | `KokoroTTS.sentenceBoundary` |
| Supertonic sentence split | `(?<=[.!?])\s+(?=\S)` | `SupertonicTTS.sentenceBoundary` |
| Google sentence split | `(?<=[.!?])\s+(?=\S)` | `GoogleTTSImpl` |
