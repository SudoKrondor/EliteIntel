# `elite.intel.ai.ears` - Developer Reference

The ears package owns everything from the microphone to the
`UserInputEvent`. It captures raw PCM, runs voice activity detection, preprocesses audio, and dispatches transcribed text onto the EventBus.

---

## Pipeline Overview

```
Microphone (any format)
    │
    ▼
[AudioFormatDetector]       probe device → pick best supported format
    │
    ▼
[AudioCalibrator]           measure noise floor + speech RMS → thresholds in SystemSession
    │
    ▼
[captureLoop]               100ms frames; PTT: button = window, else RMS gate + pre-roll
    │
    ├─ every frame ──▶ AudioMonitorEvent  (waveform visualizer, via AudioMonitorBus)
    │
    ▼  (on utterance end)
[toPCM16Mono]               any format → 16-bit LE mono
    │
[AntiAliasingFilter]        2nd-order Butterworth LPF (only when resampling)
    │
[Resampler]                 linear interpolation → 16 kHz (only when source ≠ 16 kHz)
    │
[SpectralNoiseReducer]      FFT spectral subtraction (if noise reduction enabled)
    │
[trimLeadingLowEnergy]      strip pre-speech silence frames
    │
[Amplifier]                 peak-normalize to -3 dBFS
    │
[padAudio]                  pad short utterances to 1500 ms minimum
    │
    ▼
[ParakeetSTTImpl.transcribeAndDispatch]
    │   sherpa-onnx NeMo Transducer offline inference, 4s watchdog
    │
[stripTrashPrefix]          remove filler tokens prepended by model
    │
[MicrophoneGate.decide]     PTT: discarded unless the button was held
    │                       asleep: discarded unless WakeBypass admits it
    │
    ▼
UserInputEvent (transcript, "computer" stripped)
```

---

## Device Initialization

### `AudioDeviceEnumerator`

Static utility. Enumerates system mixers and filters by `TargetDataLine` (input) or
`SourceDataLine` (output) support.
`resolveInputDevice(name)` matches by mixer name against the user's saved preference; falls back to
`null` (system default) with a warning.
`openInputLine` / `openOutputLine` open the requested mixer with the same fallback pattern.

### `AudioFormatDetector`

Probes a device (or the system default) for the best supported capture format. Tried in this order of preference:

| Priority | Format | Notes |
|---|---|---|
| 1 | 16-bit mono | Ideal - no conversion needed |
| 2 | 24-bit stereo | e.g. Kraken Chat headset |
| 3 | 16-bit stereo | downmix to mono |
| 4 | 24-bit mono | bit-depth shift only |

Sample rates probed: 48000, 44100, 96000, 192000 Hz (first match wins). Returns a `Format` record
`(sampleRate, bufferSize, captureFormat)` where `bufferSize`
corresponds to 100ms of audio.

`toPCM16Mono(input, inputLen, captureFormat)` converts captured bytes to 16-bit signed LE mono by averaging channels (downmix) and shifting bit depth. Returns the input array unchanged when already 16-bit mono.

### `AudioCalibrator`

Two-phase calibration over ~13 seconds total. Uses TTS to guide the user (localized prompts via
`StringUtls.localizedSpeech`):

1. **Noise phase
   ** (5s silence): records 100ms RMS samples, takes the 75th percentile as the noise floor. The 75th percentile is robust to transient peaks (e.g. background music) without needing a hardcoded ceiling.
2. **Speech phase** (5s counting aloud): records frames whose RMS exceeds
   `noiseFloor × 1.3`, averages them.

VAD trigger (`AudioCalibrator.gateOpenLevel`) =
`GATE_BELOW_SPEECH_DB` under average speech, but never nearer the ambient floor than
`MIN_GATE_ABOVE_NOISE_DB`. Both bounds are **ratios in dB
**, so the gate tracks the room rather than any hardcoded amplitude. Speech is the anchor because it is the stable reference; interpolating between floor and speech instead makes the gate a hostage to the noise floor, landing ~6 dB under speech in a noisy room and 20+ dB under it in a treated one.

Room quality is judged **solely** by `separationDb(noiseFloor, avgSpeechRMS)` against
`MIN_SPEECH_TO_NOISE_DB` (the sum of the two bounds — below it they cross and the gate has nowhere legal to sit). Never against an absolute amplitude: a quiet room with a good microphone legitimately produces a low noise floor
**and** a low gate, and that is a healthy calibration. `ParakeetSTTImpl`'s startup warning reuses
`gateClearsNoiseFloor` for exactly this reason, so it cannot drift from the calibration that produced the value.
`DEGENERATE_GATE_FALLBACK` applies only to a failed measurement
(muted mic, or speech that never rose above ambient).

Results are stored in `SystemSession` (`rmsThresholdHigh`, `rmsThresholdLow`) and returned as
`RmsTupple<Double, Double>`.

---

## The STT Extension Point

```java
public interface EarsInterface extends ManagedService {
    void start();

    void stop();
}
```

There is currently one implementation: `parakeet/ParakeetSTTImpl`.

### `parakeet/ParakeetSTTImpl`

Implements `EarsInterface`. Registered on the EventBus in its constructor.

**Startup sequence** (`start()`):

1. Resolve input device (`AudioDeviceEnumerator.resolveInputDevice`).
2. Detect format (`AudioFormatDetector.detectSupportedFormat`).
3. Load calibrated thresholds from `SystemSession`; run `AudioCalibrator` if not yet set.
4. Load the sherpa-onnx NeMo Transducer model (`buildRecognizer`).
5. Start single-thread `transcriptionExecutor` + `captureLoop` thread.

**Model loading** (`buildRecognizer`):
Loads `encoder.int8.onnx`, `decoder.int8.onnx`, `joiner.int8.onnx`, `tokens.txt`
from `AppPaths.getParakeetModelDir()`. Uses `greedy_search` decoding with
`maxActivePaths=50` and `blankPenalty=-2.0` (suppresses trash on short utterances). Thread count is clamped to
`[1, availableProcessors]` from `SystemSession.getSttThreads()`.

On Windows, the bundled
`onnxruntime.dll` is preloaded to win the DLL-resolution race against other apps (e.g. LM Studio) that may have installed a different version into
`System32`.

---

## VAD State Machine

The capture loop reads 100ms frames continuously. Each frame goes through the format-conversion and optional resampling chain (see pipeline above) before VAD. The state machine below is the
**hands-free** capture window; under push-to-talk the button replaces it entirely (see below).

| Constant | Value | Meaning |
|---|---|---|
| `ENTER_VOICE_FRAMES` | 1 | Consecutive loud frames to open the gate |
| `EXIT_SILENCE_FRAMES` | 6 | Consecutive quiet frames to close the gate |
| `PRE_ROLL_FRAMES` | 2 | Frames prepended to utterance before gate opened |
| `MAX_UTTERANCE_MS` | 8000 | Hard cap; gate forced closed if exceeded |
| `MIN_AUDIO_MS` | 1500 | Utterance padded to this length before inference |
| `INFERENCE_TIMEOUT_SEC` | 4 | Watchdog timeout; executor replaced on hang |

**Push-to-talk (
PTT):** while `SystemSession.isPushToTalkEnabled()`, the table above does not apply - the mapped controller button
*is* the capture window and the VAD is not consulted at all:

- with the button up, the frame is dropped: nothing is written to the collector and nothing enters the pre-roll, so a remark made before the press cannot be swept into the next capture;
- the press opens the window (`PttButtonStateEvent` sets `pttHeld`, and `pttPressed` latches the edge so a press shorter than one frame is not missed), and every frame from then on is collected;
- the release closes it - the frame the release lands in is collected first, so the last word is not clipped - and the capture goes straight to inference, marked `capturedWithPttHeld`;
- `MAX_UTTERANCE_MS` still caps one capture; a button still held simply starts the next one.

The button state is recorded here as a level the capture loop samples, never acted on in the subscriber:
the gate is timed by the thread that owns the capture window, not by the thread that reads the button.

Because a closed PTT window is not silence, `SpectralNoiseReducer.accumulateNoise` only takes frames at or below `RMS_THRESHOLD_LOW` while PTT is armed - otherwise speech made with the button up would teach the noise profile that the room sounds like a commander.

**Frame monitor:** every processed frame publishes an `AudioMonitorEvent` (via
`AudioMonitorBus`, not the main `EventBusManager`) containing raw PCM + RMS + thresholds. The waveform visualizer (
`AudioWaveformPanel`) subscribes to this.

---

## Audio Preprocessing (Pre-Inference)

### `AntiAliasingFilter`

2nd-order Butterworth IIR low-pass filter. Applied only when the capture rate differs from 16 kHz (e.g. 48000 → 16000 Hz). Cutoff is set to 90% of the target Nyquist
(7200 Hz for 16 kHz output) to provide a clean transition band. Filter state (`w1`,
`w2`) is carried across calls so frame boundaries are seamless.

### `Resampler`

Stateful linear interpolation downsampler (e.g. 48000 → 16000 Hz). Carries a fractional-sample
`phase` across calls to avoid discontinuities at buffer boundaries. Always paired with
`AntiAliasingFilter` in the capture loop.

### `SpectralNoiseReducer` (singleton)

FFT-based overlap-add spectral subtraction. 512-sample FFT (~32ms at 16kHz), 50% hop, Hanning window.

**Noise learning:**
`accumulateNoise(pcm, len)` is called for every VAD-inactive frame. After 20 silence frames (~2s) the initial mean-power spectrum is committed. From then on an EMA (α=0.90) updates the estimate every 10 silence frames.

**Denoising:** `denoise(pcm, strengthIdx)` applies spectral subtraction with three strength levels:

| Level | Over-subtraction α | Spectral floor β |
|---|---|---|
| 0 Low | 1.5 | 0.10 |
| 1 Medium | 2.0 | 0.05 |
| 2 High | 3.0 | 0.02 |

Returns input unchanged if no noise spectrum has been estimated yet. Call
`reset()` when restarting STT so the reducer re-learns the new environment.

The noise spectrum reference is `volatile`; `accumulateNoise` is
`synchronized`. This is the only cross-thread contract - capture thread writes, transcription thread reads.

### `Amplifier`

Two-pass peak normalizer. Pass 1 finds the peak sample magnitude; Pass 2 scales to -3 dBFS (
`≈ 23197` linear) with hard clip safety. No-ops if peak is below 100 or already at/above target. Applied to the full utterance before PCM-to-float conversion.

### `StreamNormalizer`

Per-frame AGC with separate attack (0.40) and release (0.98) smoothing coefficients. Targets -18 dBFS RMS. Not used by
`ParakeetSTTImpl` (which processes complete utterances); available for future streaming STT backends that need live level control.

---

## Post-Transcription Dispatch

### Trash filtering (`stripTrashPrefix`)

Parakeet occasionally prepends filler tokens (`mm-hmm`, `okay`, etc.) to real utterances.
`stripTrashPrefix` strips any leading sequence matching `Reducer.trashSttWords`
(punctuation-tolerant), then removes trailing punctuation from the remainder. Transcripts that are entirely trash are silently discarded.

### Microphone gating (`MicrophoneGate`)

Two gates, and `MicrophoneGate.decide(capturedWithPttHeld, pushToTalkEnabled, sleeping)` says which one is in force for this transcript. Push-to-talk supersedes Sleep/Wake, so a commander who deliberately held the button is never refused by a sleep flag left over from a hands-free session.

| Verdict | When | What happens |
|---|---|---|
| `OPEN_PUSH_TO_TALK` | the button was held while the utterance was captured (`capturedWithPttHeld`) | dispatched as a PTT capture |
| `CLOSED_PUSH_TO_TALK` | `SystemSession.isPushToTalkEnabled()` and the button was not held - a backstop, since the capture window under PTT *is* the button | dropped as room noise |
| `CLOSED_ASLEEP` | hands-free and `SystemSession.isSleeping()` | handed to `WakeBypass`, below |
| `OPEN_HANDS_FREE` | neither gate is closed | dispatched normally |

Anything dropped dies here, before it costs an AI round trip.

### Sleep-mode gating (`WakeBypass`)

While the Sleep/Wake gate is closed, `WakeBypass.admit(transcript)` returns what to route, or `null`:

- an exact **wake
  phrase** (`AiActionLocalizations.wakeBypassPhrases()`) is routed whole, so the model can send it to `WakeupCommand` and reopen the gate;
- a **listen-bypass
  prefix** (`AiActionLocalizations.listenBypassPrefixes()`) at the start, followed by real content, is stripped and the remainder routed - "listen, open the galaxy map" is an order to open the map.

The prefix must be at the start, which is what stops "do not listen, open the map" from talking its way past a gate the commander deliberately closed. Longest prefix first, so "listen up …" is not first reduced by the shorter "listen" and left with a stray "up".

### TTS gate (`IsSpeakingEvent`)

While the app is speaking (`isSpeaking == true`):

- PTT-captured transcripts: interrupt TTS (`TTSInterruptEvent`) and dispatch normally.
- Transcripts matching a localized interrupt phrase: interrupt TTS, discard transcript.
- All other transcripts: silently dropped.

### Final dispatch

`"computer"` is stripped from the transcript (it is a common STT artifact from the game context), then
`UserInputEvent(transcript)` is published to the EventBus.

---

## Adding a New STT Backend

1. Create a class (or sub-package) and implement `EarsInterface`.
2. Call `EventBusManager.register(this)` in the constructor.
3. In `start()`:
    - Resolve the mixer with `AudioDeviceEnumerator.resolveInputDevice(systemSession.getAudioInputDevice())`.
    - Detect format with `AudioFormatDetector.detectSupportedFormat(mixerInfo)`.
    - Load or run calibration with `AudioCalibrator.calibrateRMS(format, mixerInfo)`.
4. Publish `AudioMonitorEvent` on each captured frame (via `AudioMonitorBus.publish`)
   so the waveform visualizer stays live.
5. Publish `UserInputEvent(transcript)` when transcription completes.
6. Subscribe to `IsSpeakingEvent` to gate transcripts while TTS is speaking.
7. Use `StreamNormalizer` if your backend processes frames in real time rather than complete utterances.
8. Wire the new implementation in `AppController` / `ApiFactory` alongside the existing provider selection logic.

---

## Key Classes - Quick Reference

| Class | Role |
|---|---|
| `EarsInterface` | Extension point for STT backends |
| `parakeet/ParakeetSTTImpl` | Only current backend; sherpa-onnx NeMo Transducer |
| `AudioFormatDetector` | Device format probing + PCM conversion |
| `AudioCalibrator` | Two-phase RMS threshold calibration |
| `AudioDeviceEnumerator` | Enumerate/resolve/open mixers |
| `AntiAliasingFilter` | Butterworth IIR LPF before resampling |
| `Resampler` | Stateful linear interpolation downsampler |
| `Amplifier` | Two-pass peak normalizer (-3 dBFS) |
| `StreamNormalizer` | Per-frame AGC for streaming backends |
| `SpectralNoiseReducer` | FFT spectral subtraction noise gate |
| `parakeet/HotwordEncoder` | BPE-encode hotwords to sherpa-onnx token format |
| `AudioMonitorEvent` | Per-frame PCM + RMS snapshot for waveform UI |
| `IsSpeakingEvent` | TTS playback gate for STT |
| `DumpAudioForTesting` | WAV file dumper (disabled; toggle `if (true) return`) |

## Key Constants

| Constant | Value | Location |
|---|---|---|
| `SAMPLE_RATE` | `16000` Hz | `ParakeetSTTImpl` |
| `ENTER_VOICE_FRAMES` | `1` frame | `ParakeetSTTImpl` |
| `EXIT_SILENCE_FRAMES` | `6` frames | `ParakeetSTTImpl` |
| `PRE_ROLL_FRAMES` | `2` frames | `ParakeetSTTImpl` |
| `MAX_UTTERANCE_MS` | `8000` ms | `ParakeetSTTImpl` |
| `MIN_AUDIO_MS` | `1500` ms | `ParakeetSTTImpl` |
| `INFERENCE_TIMEOUT_SEC` | `4` s | `ParakeetSTTImpl` |
| `NOISE_PERCENTILE` | `0.75` | `AudioCalibrator` |
| `GATE_BELOW_SPEECH_DB` | `12.0` dB (gate anchor, under average speech) | `AudioCalibrator` |
| `MIN_GATE_ABOVE_NOISE_DB` | `6.0` dB (gate never nearer the ambient floor) | `AudioCalibrator` |
| `MIN_SPEECH_TO_NOISE_DB` | `18.0` dB (derived: sum of the two above) | `AudioCalibrator` |
| `DEGENERATE_GATE_FALLBACK` | `120.0` (failed measurement only) | `AudioCalibrator` |
| `FFT_SIZE` | `512` (~32ms) | `SpectralNoiseReducer` |
| `TARGET_DBFS` (Amplifier) | `-3.0` dBFS | `Amplifier` |
| `TARGET_DBFS` (StreamNormalizer) | `-18.0` dBFS | `StreamNormalizer` |