# `elite.intel.ai.mouth` - Developer Reference

The mouth package owns everything from a
`VocalisationRequestEvent` to speaker output. It normalises the various vox event types produced by other packages, synthesises speech via one of two backends (offline Kokoro or Google Cloud), and gates the microphone while audio is playing.

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
                          - AiVoxResponseEvent: publish IsSpeakingEvent true (wraps CompletableFuture)
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
                          IsSpeakingEvent(false) published when done
```

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
| `completionFuture` | `CompletableFuture<Void>` (nullable) | Completed when last sentence finishes; used to block SPEAK custom commands |

---

## 2. `VocalisationRouter`

`VocalisationRouter` is the central normaliser. It subscribes to every vox event type and converts each to a
`VocalisationRequestEvent` on the main EventBus.

**`AiVoxResponseEvent` special handling**: If the event carries a
`CompletableFuture`, `VocalisationRouter` wraps it in a new
`VocalisationRequestEvent`. If no future is present, the router creates one, publishes
`IsSpeakingEvent(true)`, and attaches a `whenComplete` callback that publishes
`IsSpeakingEvent(false)`. This is the authoritative pair that gates the STT microphone.

**`RadioTransmissionEvent` special handling**: The router uses `getRandomVoice()`
on the active voice provider (Kokoro or Google), selecting any voice that is NOT the current session voice. Sets
`isRadio=true` on the resulting
`VocalisationRequestEvent`.

---

## 3. `MouthInterface` - The Extension Point

```java
public interface MouthInterface extends ManagedService {
    void interruptAndClear();

    @Subscribe
    void onVoiceProcessEvent(VocalisationRequestEvent event);
}
```

`ManagedService` provides `start()` and `stop()`. Implementations register themselves on the EventBus (
`EventBusManager.register(this)`) in their constructor so
`@Subscribe` is live immediately, and launch their daemon threads in `start()`.

Current implementations: `KokoroTTS` (offline), `GoogleTTSImpl` (cloud).

---

## 4. Kokoro TTS Backend (`kokoro/`)

### Overview

`KokoroTTS` is a singleton. It uses the `kokoro-multi-lang-v1_0` ONNX model loaded from
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

### Interruption

`interruptAndClear()`:

1. Drain `synthesisQueue` and `playbackQueue`, completing any pending futures.
2. Set `interruptRequested` atomic flag.
3. Call `line.stop()`, `line.flush()`, `line.start()` to silence the speaker mid-word.
4. Clear the flag.

The `canBeInterrupted` field on `VocalisationRequestEvent` controls whether
`TTSInterruptEvent` triggers this. Mission-critical announcements set it to false and are immune to interrupt.

---

## 5. Google TTS Backend (`google/`)

### Overview

`GoogleTTSImpl` is a singleton that calls the Google Cloud TTS API using an API key from
`systemSession.getTtsApiKey()`. Audio is returned as 24kHz LINEAR16 PCM. The API key is never logged and must not be transmitted outside the Cloud TTS endpoint.

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

| Name | Language | Model | Gender | speechRate |
|---|---|---|---|---|
| ANNA | en-GB | Chirp-HD-F | Female | (see enum) |
| EMMA | en-US | Chirp3-HD-Despina | Female | (see enum) |
| JAKE | en-US | Chirp3-HD-Iapetus | Male | (see enum) |
| JAMES | en-AU | Chirp3-HD-Algieba | Male | (see enum) |
| JENNIFER | en-US | Chirp3-HD-Sulafat | Female | 1.2 (default) |
| JOSEPH | en-US | Chirp3-HD-Sadachbia | Male | (see enum) |
| MARY | en-US | Chirp3-HD-Zephyr | Female | (see enum) |
| MICHAEL | en-US | Chirp3-HD-Charon | Male | (see enum) |
| OLIVIA | en-GB | Chirp3-HD-Aoede | Female | (see enum) |
| RACHEL | en-US | Chirp3-HD-Zephyr | Female | (see enum) |
| STEVE | en-US | Chirp3-HD-Algenib | Male | (see enum) |

Default: `JENNIFER`. `GoogleVoiceProvider.getSpeechRate()` returns 1.2 if the voice is not found.

### `GoogleVoiceProvider`

Singleton, implements `VoiceProvider<VoiceSelectionParams>`.

- `getUserSelectedVoice()` - reads `SystemSession.getGoogleVoice()`; falls back to `JENNIFER`.
- `getRandomVoice()` - picks any `GoogleVoices` that is not the current session voice; used by `VocalisationRouter` for
  `RadioTransmissionEvent`.
- `getVoiceParams(voiceName)` - accepts either the enum constant name (`EMMA`) or the display name (
  `Emma`); applies the non-EN language override (see Section 5) before falling back to the static `voiceMap`.

---

## 8. `CompletableFuture` Completion Contract

The `SPEAK` custom command blocks the command executor thread until TTS finishes. This is implemented via
`CompletableFuture<Void>`:

1. `AiVoxResponseEvent` carries an optional `CompletableFuture`.
2. If present, `VocalisationRouter` passes it through to `VocalisationRequestEvent`.
3. The synthesis thread (Kokoro) or the vocalization thread (Google) completes the future after the **last
   ** sentence's audio finishes writing to the line.
4.
`interruptAndClear()` completes all pending futures immediately (with null) so the command executor is never left blocked.

If `AiVoxResponseEvent` carries no future, `VocalisationRouter` creates one and attaches
`IsSpeakingEvent` true/false publication to it instead.

---

## 9. Adding a New TTS Backend

1. Create a class (or sub-package) and implement `MouthInterface`.
2. Call `EventBusManager.register(this)` in the constructor.
3. In `start()`, open a
   `SourceDataLine` at 24000 Hz, 16-bit, mono, signed LE. Keep the line open for the session lifetime; do not reopen per sentence.
4. In `onVoiceProcessEvent()`:
    - Split text with the sentence regex.
    - If `event.isRadio()`, apply `RadioFilter.apply(pcm)` after synthesis.
    - Call `AudioDeClicker.sanitize(pcm, fadeMs)` on each sentence chunk before write.
    - Complete `event.getCompletionFuture()` (if non-null) after the last sentence.
5. Implement `interruptAndClear()`: drain queues, complete pending futures, flush the `SourceDataLine`.
6. Subscribe to `TTSInterruptEvent` and call `interruptAndClear()` only when
   `event.hasAiReference()` matches your backend or when `canBeInterrupted` is true.
7. Wire the new implementation in `ApiFactory` / `AppController` alongside the existing provider selection logic.

---

## Key Classes - Quick Reference

| Class | Role |
|---|---|
| `MouthInterface` | Extension point for TTS backends |
| `subscribers/VocalisationRouter` | Normalises all vox events to `VocalisationRequestEvent` |
| `kokoro/KokoroTTS` | Offline backend; sherpa-onnx kokoro-multi-lang-v1_0 |
| `kokoro/KokoroVoices` | 53 Kokoro voice enum with sid values |
| `google/GoogleTTSImpl` | Cloud backend; Google Cloud TTS API |
| `google/GoogleVoices` | 11 Google voice enum with speechRate and gender |
| `google/GoogleVoiceProvider` | Voice selection + non-EN language override |
| `google/VoiceProvider<T>` | Interface for voice provider implementations |
| `AudioDeClicker` | Fade-in + volume scaling on PCM-16 LE |
| `RadioFilter` | Bandpass + static noise shortwave radio effect |
| `subscribers/events/VocalisationRequestEvent` | Normalised TTS event (origin, voice, flags, future) |
| `subscribers/events/AiVoxResponseEvent` | LLM spoken answer; carries optional CompletableFuture |
| `subscribers/events/TTSInterruptEvent` | Interrupt signal; `hasAiReference` field |
| `subscribers/events/VocalisationSuccessfulEvent` | Per-sentence completion event (Google only) |

## Key Constants

| Constant | Value | Location |
|---|---|---|
| `SAMPLE_RATE` | `24000` Hz | `KokoroTTS`, `GoogleTTSImpl`, `AudioDeClicker` |
| Default Kokoro voice | `GEORGE` (sid=26) | `KokoroTTS` |
| Default Google voice | `JENNIFER` | `GoogleVoiceProvider` |
| Default Google speechRate | `1.2` | `GoogleVoiceProvider` |
| `NOISE_AMPLITUDE` | `50f` (~0.15% full scale) | `RadioFilter` |
| `GAIN` | `1.4f` | `RadioFilter` |
| HP cutoff | `300 Hz` | `RadioFilter` |
| LP cutoff | `5500 Hz` | `RadioFilter` |
| Kokoro sentence split | `(?<=[.,!?])\s+(?=\S)` | `KokoroTTS` |
| Google sentence split | `(?<=[.!?])\s+(?=\S)` | `GoogleTTSImpl` |