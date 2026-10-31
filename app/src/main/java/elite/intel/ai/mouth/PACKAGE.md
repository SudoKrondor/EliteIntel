# `elite.intel.ai.mouth` - Developer Reference

The mouth package owns everything from a
`VocalisationRequestEvent` to speaker output. It normalises the various vox event types produced by other packages, synthesises speech via offline Supertonic 3 or Google Cloud, and publishes an authoritative playback lifecycle while STT remains active for barge-in.

---

## Pipeline Overview

```
AiVoxResponseEvent  AiVoxDemoEvent  RadioTransmissionEvent  …
        │                       │                        │
        └───────────────────────┴──────┬─────────────────┘
                                       ▼
                          [VocalisationRouter]
                          - normalise system, audition, and radio requests
                          - gate RadioTransmissionEvent on the radio toggle
                          - local radio engine selects a voice; isRadio=true
                          - every request carries one request-scoped VocalisationHandle
                                       │
                          VocalisationRequestEvent (main EventBus)
                                       │
                        ┌─────────────┴──────────────┐
                        ▼                            ▼
              [SupertonicTTS]               [GoogleTTSImpl]
              (offline, sherpa-onnx)        (Google Cloud TTS API)
                        │                            │
              synthesisQueue               ttsQueue (synthesis)
              SupertonicTTS-Synthesis      TTSThread
              - split sentences            - split sentences
              - generate() via sherpa-onnx - Google Cloud API call
              - fade + volume + effects    - 24kHz LINEAR16
              - tones where enabled              │
                        │                   vocalizationQueue
              playbackQueue                VocalizationThread
              SupertonicTTS-Playback       - SourceDataLine.write()
              - SourceDataLine.write()     - VocalisationSuccessfulEvent
                        │                            │
                        └─────────────┬──────────────┘
                                       ▼
                                   Speaker
                          handle completion updates shared IsSpeakingEvent state
```

---

## 1. Event Taxonomy

The events below extend the base vox event and are normalised by `VocalisationRouter` to
`VocalisationRequestEvent`. VEGA uses its speech gateway for tracked requests.

| Event | Always routed? | canBeInterrupted | isRadio | Notes |
|---|---|---|---|---|
| `AiVoxResponseEvent` | Yes | true unless it carries a completion future | false | LLM spoken answer; optional `CompletableFuture<Void>` for SPEAK commands |
| `MissionCriticalAnnouncementEvent` | Yes | false | false | High-priority; not gated by settings. VEGA's voice, so marked VEGA speech (away-from-ship effect applies) |
| `AiVoxDemoEvent` | Yes | true | false for ship, true for carrier | Fleet/carrier voice audition; carrier uses the radio engine even when radio is off |
| `RadioTransmissionEvent` | Only while radio is on | true | **true** | Local engine draws a voice unless the transmission names one |

`TTSInterruptEvent` is handled directly by each backend; `VocalisationRouter`
does not touch it.

### `VocalisationRequestEvent` fields

| Field | Type | Meaning |
|---|---|---|
| `originType` | `Class<?>` | The original event class; used by Google to publish `VocalisationSuccessfulEvent` via reflection |
| `voiceName` | `String` (nullable) | Override voice; null draws a cast voice for radio or uses the provider default for ordinary speech |
| `canBeInterrupted` | `boolean` | Whether `TTSInterruptEvent` mid-playback should abort this utterance |
| `isRadio` | `boolean` | Route through the local radio engine; use its enhanced or baseline radio processor |
| `handle` | `VocalisationHandle` | Request id, interruptibility, ownership, and exactly-once completion |
| `completionFuture` | `CompletableFuture<Void>` | The handle's non-null future; completed after final playback or another terminal outcome |

---

## 2. `VocalisationRouter`

`VocalisationRouter` normalises system responses, mission-critical announcements, Fleet/carrier auditions, and
radio transmissions to `VocalisationRequestEvent` on the main EventBus. VEGA speech uses its own gateway.

**`AiVoxResponseEvent` special handling**: If the event carries a
`CompletableFuture`, `VocalisationRouter` passes it through to `VocalisationRequestEvent`; otherwise the request creates its own future. The eligible active Mouth claims the request's `VocalisationHandle` during the same EventBus dispatch. Guava queues reentrant posts, so the no-Mouth check runs through `GameEventBus.afterCurrentDispatch` only after the outer post has drained; checking immediately after a nested `publish` would reject the request before a Mouth sees it. Only the handle publishes `IsSpeakingEvent`, using a process-wide active-request count, so overlapping requests cannot report a false idle state. STT continues listening while the state is true and treats a commander transcript as barge-in.

**`RadioTransmissionEvent` special handling**: The router sets `isRadio=true` on the resulting
`VocalisationRequestEvent`; Supertonic, the radio engine `RadioVoicing` names for every game client, draws a voice from its own cast when the request does not name one. Cloud mouths ignore it. The words are the
**game
client's** own prose, in the client's language (`GameLanguage`, off the journal header). When Supertonic is not the main mouth (a Google commander) it runs as a dedicated `RADIO_MOUTH` service beside it. A radio task carries the client's language into `generate(...)` (`SherpaOnnxTTS.languageOf`), read per request, so a client relaunched in another language needs no mouth restart: `FileheaderEventSubscriber` only hands the new language to `GameLanguage`.

---

## 3. `MouthInterface` - The Extension Point

```java
public interface MouthInterface extends ManagedService {
    void interruptAndClear();

    @Subscribe
    void onVoiceProcessEvent(VocalisationRequestEvent event);
}
```

An eligible running backend must call `event.handle().claimForPlayback()` before enqueueing work. A backend that does not own the event (Google for radio, or radio-role Supertonic for main speech) leaves it unclaimed for the correct backend. A claimed handle must be completed or failed on every terminal path.

`ManagedService` provides `start()` and `stop()`. Implementations initialize their engine/workers and register
on `GameEventBus` during `start()`, then unregister and settle owned handles during `stop()`.

Current implementations: `SupertonicTTS` (offline) and `GoogleTTSImpl` (cloud). Supertonic is the only local engine. The engine it replaced was removed in V1.2: migration `12015` moved its commanders to Supertonic, whose cast carries the same voice names, and commander migration `12001` moved the three voices Supertonic never got (`ES_DORA`, `IT_SARA`, `PT_DORA`) to its default.

---

## 4. The sherpa-onnx pipeline (`sherpa/`)

### Overview

`sherpa/SherpaOnnxTTS` is the abstract pipeline every local engine runs on: the two queues below, the synthesis and playback threads, interrupts, the persistent line, the MAIN / RADIO `Role` and the
`MainVoicePlaybackGate` ducking. Supertonic is its only engine. A concrete engine supplies only what its model needs: `buildOfflineTts()` (called once per process),
`generate(...)`, the `sentenceBoundary()` regex, and its voice cast (`defaultVoiceName`, `isInTheCast`, `sidOf`, `radioVoiceNameFor`). `sherpa/RadioVoiceDraw` is the one copy of the radio voice draw; each cast enum hands it `values()`.

Models load from `AppPaths.getTtsModelDir()` via the sherpa-onnx JNI library. The pipeline's sample rate is 24000 Hz, 16-bit mono.

### Two-Queue Pipeline

```
onVoiceProcessEvent()
    │  split text into sentences
    │  push SynthesisTask per sentence → synthesisQueue (BlockingQueue)
    │
<Engine>-Synthesis thread (daemon)
    │  pop SynthesisTask
    │  resetNumericLocale()
    │  tts.generate(text, sid, voice's pace) → float[] samples → PCM bytes
    │  VoiceEqualizer.apply(pcm, voice's EQ)  ← clean voice, before any radio processing
    │  AudioDeClicker.sanitize(pcm, fadeMs)   ← fade-in to suppress pop
    │  enhanced processor, or baseline RadioFilter for radio
    │  apply volume × voice's trim (+ Supertonic boost) ← after the processor, whose compressor would undo it
    │  attach optional first/last sentence tones
    │  push PlaybackTask → playbackQueue (BlockingQueue)
    │
<Engine>-Playback thread (daemon)
    │  pop PlaybackTask
    │  SourceDataLine.write(pcm)  ← persistent line, never closed between sentences
    │  on last sentence: complete CompletableFuture
```

The `SourceDataLine` is opened once in
`start()` and kept open for the session lifetime. Closing and reopening between sentences causes audible pops and delays.

### Critical Constraint - Never Call `tts.release()`

`tts.release()` **must
not** be called in `stop()`. The rule dates from an earlier engine whose lexicon destructor in sherpa-onnx crashed the JVM with a SIGSEGV when `release()` was called after the model had been used. It stays in the base class until native release is proven safe for Supertonic.

### LC_NUMERIC Locale Fix (`resetNumericLocale()`)

ONNX Runtime calls `setlocale(LC_ALL, "")` during its initialization, which on some systems sets
`LC_NUMERIC` to a locale using `,` as the decimal separator (e.g. French). A native front end that parses floats with `stof()` hard-crashes if `LC_NUMERIC`
is not `"C"`. `resetNumericLocale()` sets it back to `"C"` via JNA before every `generate()` call. It costs one libc call, and which of Supertonic's native parsers are locale-sensitive is not known.

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

`SupertonicTTS` is the local engine, the `SherpaOnnxTTS` subclass: the same two-queue pipeline, the same `Role` (MAIN / RADIO), the same `MainVoicePlaybackGate` ducking and the same never-`release()` rule, because all of that is the base class. It runs the `sherpa-onnx-supertonic-3-tts-int8-*` model from `AppPaths.getTtsModelDir()` through the same JNI.

What differs:

- **One multilingual model.** The language is not baked into the model config; each
  `generateWithConfigAndCallback` call passes it as `GenerationConfig.extra("lang", supertonicLangCode(language))`
  (`en`, `de`, `fr`, `es`, `it`, `pt` for both `PT` and `PTBZ`, `ru`, `uk`). The engine is built once and never rebuilt on a language switch.
-
**Cyrillic.** Supertonic reads Russian and Ukrainian natively, so they are app languages like any other, and a Russian client's radio chatter is read as `ru`.
- **45
  voices** (`SupertonicVoices`, by `sid`), all offered: the ten upstream presets (`F1`-`F5`, `M1`-`M5`, sids 0-9) and 35 fitted voices that keep the pre-V1.2 voice names (sids 10-44). The fleet grid shows each as "display name - gender". `DEFAULT_VOICE` is the ship default and the collapse target for a stored name the cast does not carry.
- **44.1 kHz native
  output**, resampled to the pipeline's `SAMPLE_RATE` (24 kHz) before the declicker, the radio filter and the playback line, so nothing downstream sees a second rate.
- **Sentence split on `.!?` only** - comma clauses stay together so the model's own prosody carries the pause.
- **Text is normalised before synthesis** (`SupertonicTextNormalizer`, through the base class's overridable
  `textForSynthesis`): two-to-four-letter capitalised words are spelled letter by letter in the voice's language
  (`FSD`, `SRV`), decimals with a unit letter and comma decimals are read as numbers, thousands grouping is read the
  right way round whatever the separator, procedural sector codes (`XO-R b19-3`) are spelled whole, and whole numbers
  are spelled through `SignedAndDecimalNumbers.inWordsIncludingWholeNumbers` - only this voice asks for whole numbers. Italian also spells a class letter (`classe A`).
- **Russian gets its stress
  marked** (`supertonic/stress/RussianStress`, last step of `textForSynthesis`, `RU` only). Supertonic guesses Russian stress from the letters and gets homographs wrong (`голоса`: го́лоса / голоса́). A combining acute (U+0301) after the vowel moves it, because the model decomposes its input and learned that token from é and á. A Java port of RUAccent's tiny pipeline (`Accentuator`) puts one on every word, and ё back where it was dropped (`все` -> `всё`): ё dictionary and model, then homographs judged against the sentence, then the stress dictionary or the character model. Models and dictionaries live in `distribution/tts/ruaccent-tiny2.1` (MIT), load once - at `buildOfflineTts` when the commander or the game client is Russian, else on first use - in about a second, and cost a few ms a sentence. The port only inserts marks and ё: RUAccent's own pipeline deletes characters it does not know and reflows spacing. Missing or failing models fail open to unmarked text, logged.
  `AccentuatorTest` pins the port to the Python original's output.
- **Ten diffusion steps** (`NUM_STEPS`, raised from 8 on 2026-10-25), at a `SPEED_MULTIPLIER` of 0.85, which is also
  the fastest the model is asked to go (`MAX_MODEL_SPEED`).
- **Each voice is tuned on its
  own** (`VoiceTuning`: speed, a 0-150% volume trim on top of the Voice/Radio level, and a six-band `VoiceEqualizer` - low shelf, four peaks, high shelf, ±12 dB). The settings live in the shared
  `supertonic_voice_tuning` table (migration `12016`, one row per tuned voice) and are read through
  `SupertonicVoiceTuningManager`, which holds the table in memory: the synthesis thread asks once per sentence and never touches the database. The commander sets them in `SupertonicVoiceTuningDialog` (Settings / Speech, always available because Supertonic voices the radio for a Google commander too). Its Test button publishes a
  `LocalVoiceAuditionEvent`, which Supertonic claims in either role and plays clean, and Google ignores. There is no global speech speed for Supertonic: `game_session.googleSpeechSpeed` drives Google alone.
- **The model folder is checked at load** (`validateModelAssets`): a missing `voice.bin` or `unicode_indexer.bin` is
  named, with the fix, rather than failing inside the native loader.

Selecting it is a stored setting like any other engine (`game_session.ttsProvider = SUPERTONIC`), written by the LOCAL segment of the AI services settings' speech switch. It is the shipped default and the fallback for an unreadable stored value.

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

Commas are not boundaries. Google's Chirp3-HD models handle longer sentences more naturally.

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

Gender is resolved from
`GoogleVoices.isMale()` on the current session voice. This means non-English users keep their chosen voice's gender but get a locale- appropriate model.

### `VocalisationSuccessfulEvent`

Published after each sentence via
`Class.forName(originType.getName())` reflection to reconstruct the original event class. Subscribers use this to track per-sentence TTS completion (e.g., for UI feedback).

---

## 5A. Microsoft Edge Read Aloud (removed)

The `edge/` backend was removed in v1.1.0026. It reached Microsoft's consumer Read Aloud service, which is built for the Edge browser and not offered to third parties, by presenting itself as that browser. Migration
`11007` moved its commanders to the local engine of the day (since replaced by Supertonic, see 12015). Do not bring back a mouth that calls a private service by pretending to be its client. A Microsoft voice belongs behind Azure Speech and the commander's own key.

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

Static utility. Applies a shortwave radio transmission effect in-place to a PCM-16 LE buffer at 24000 Hz mono. Called from the synthesis thread after sanitization and before volume control for radio requests. Eligible VEGA requests use `TransmissionAudio.degrade` when enhanced processing is enabled.

**Processing chain**:

1. Butterworth highpass biquad (fc=300 Hz, Q=0.707) - removes bass and voice fundamental
2. Butterworth lowpass biquad (fc=5500 Hz, Q=0.707) - retains sibilance and upper harmonics
3. GAIN=1.4 compensation for energy lost through the bandpass; no synthetic static

The biquad coefficients are precomputed constants (see class header for derivation). The filters use a direct-form I recurrence.

`TransmissionAudio` supplies gentle pre-filter saturation to give darker voices upper harmonics,
an independent 1200–7000 Hz voice band, 4 kHz presence lift, compression and 25% more
post-compression drive behind a soft ceiling, plus software-generated opening/closing tones. It replaces the baseline filter when
enhanced processing is selected; neither path generates static. Radio traffic keeps the baseline filter when enhanced processing is off. The VEGA speech gateway marks its own requests explicitly, and mission-critical callouts (also VEGA's voice) carry the same mark; only those requests can gain
the optional treatment while on foot or in an SRV. The first and last sentence of each request carry the
tones, so a multi-sentence transmission has one pair and cancellation uses the existing playback queue.
Supertonic 3 alone can apply 0–100% gain with a soft peak ceiling after the normal volume control.
The tone slider scales the channel tones independently of the voice level; both settings persist per installation.

The Audio Settings controls have two roles: **Radio beep at the start and end of each message** and **Radio
effect** select the sound; **Apply selected effects to radio chat and NPC messages** and **Apply selected
effects to VEGA on foot or in an SRV** select the recipients. The former `enhancedRadioEffect` database and session name (and the `settings.audio.transmission.degradation` bundle key) remains for compatibility even though the enhanced processor also applies to eligible VEGA speech.

| Speech route | Its recipient switch | Enhanced processing | Result (tones are independent) |
| --- | --- | --- | --- |
| Radio chat / NPC | Off | Either | Standard `RadioFilter`; no optional tones or enhanced processing |
| Radio chat / NPC | On | Off | Standard `RadioFilter`; optional tones if enabled |
| Radio chat / NPC | On | On | `TransmissionAudio.degrade` instead of `RadioFilter`; optional tones if enabled |
| VEGA aboard ship | Either | Either | Ordinary speech; no optional tones or radio processing |
| VEGA on foot / in SRV | Off | Either | Ordinary speech; no optional tones or radio processing |
| VEGA on foot / in SRV | On | Off | Ordinary speech; optional tones if enabled |
| VEGA on foot / in SRV | On | On | `TransmissionAudio.degrade`; optional tones if enabled |

The standard radio filter is part of the existing radio route and remains active when optional effects are
disabled. Both recipient switches off therefore disable optional tones and enhanced processing, but do not
make radio speech unfiltered.

---

## 7. Voice Catalogs

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
- `getRandomVoice()` - picks any `GoogleVoices` that is not the current session voice for requests explicitly
  marked random; radio transmissions use a local engine instead.
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
    - Call `AudioDeClicker.sanitize(pcm, fadeMs)` on each sentence chunk before write.
   - Snapshot `TransmissionAudio.forRequest(event)` at admission. Before volume, apply its enhanced processing where enabled; otherwise radio speech retains `RadioFilter`. The volume goes last because the enhanced processor compresses and would otherwise level a quiet slider setting back up. Attach optional tones to the first/last
      sentence, keeping the treatment out of ordinary narration.
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
| `sherpa/SherpaOnnxTTS` | The offline pipeline the local engine runs on: queues, threads, interrupts, line, roles |
| `sherpa/RadioVoiceDraw` | The radio voice draw, generic over a cast enum |
| `supertonic/SupertonicTTS` | Offline backend; sherpa-onnx Supertonic 3 (int8); main mouth and radio, every language |
| `supertonic/SupertonicVoices` | 45 Supertonic voice enum with sid values |
| `supertonic/stress/RussianStress` | Stress marks and ё on Russian text before Supertonic reads it (RUAccent port, `Accentuator`) |
| `RadioVoicing` | Which engine voices radio (Supertonic, for every client) and in which language |
| `TtsProvider` | The stored engine choice |
| `google/GoogleTTSImpl` | Cloud backend; Google Cloud TTS API |
| `google/GoogleVoices` | 11 Google voice enum with gender and Chirp3-HD character |
| `google/GoogleVoiceProvider` | Voice selection + non-EN language override |
| `google/VoiceProvider<T>` | Interface for voice provider implementations |
| `AudioDeClicker` | Fade-in + volume scaling on PCM-16 LE |
| `RadioFilter` | Existing radio bandpass without synthetic static |
| `TransmissionAudio` | Optional higher band, saturation, compression and channel tones |
| `VoiceTuning` / `VoiceEqualizer` | One local voice's speed, volume trim and six-band EQ, and the EQ itself |
| `supertonic/SupertonicBoost` | Supertonic 3's fixed extra gain (+60%), with a soft peak ceiling |
| `subscribers/events/VocalisationRequestEvent` | Normalised TTS event (origin, voice, flags, handle) |
| `subscribers/events/AiVoxResponseEvent` | LLM spoken answer; carries optional CompletableFuture |
| `subscribers/events/TTSInterruptEvent` | Global or request-id-targeted interrupt signal |
| `subscribers/events/VocalisationSuccessfulEvent` | Per-sentence completion event (Google) |

## Key Constants

| Constant | Value | Location |
|---|---|---|
| `SAMPLE_RATE` | `24000` Hz | `SherpaOnnxTTS`, `GoogleTTSImpl`, `AudioDeClicker` |
| Default Supertonic voice | `FR_SIWIS` (sid=38) | `SupertonicVoices.DEFAULT_VOICE` |
| Default Google voice | `JENNIFER` | `GoogleVoiceProvider` |
| `GAIN` | `1.4f` | `RadioFilter` |
| HP cutoff | `300 Hz` | `RadioFilter` |
| LP cutoff | `5500 Hz` | `RadioFilter` |
| Supertonic sentence split | `(?<=[.!?])\s+(?=\S)` | `SupertonicTTS.sentenceBoundary` |
| Google sentence split | `(?<=[.!?])\s+(?=\S)` | `GoogleTTSImpl` |
