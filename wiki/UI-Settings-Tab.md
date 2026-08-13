# Settings Tab

<img src="images/settings.png" class="inline" height="20" alt="Settings"> The plumbing. A
**Common** strip that applies everywhere, then three sub-tabs: **AI Services**, **Audio**, and
**Push To Talk**.

---

## Common

Shown above the sub-tabs, because it applies to all of them.

**Language** — the language of both your voice commands and the app's own interface. Choosing
one re-renders the whole window immediately and Vega announces the change out loud.

Supported: English, Spanish, French, German, Italian, Portuguese, Brazilian Portuguese,
Ukrainian, Russian.

**Journal Directory** — where Elite Dangerous writes its journal files. Optional: leave it
blank and the standard location for your platform is used. This is how Elite Intel knows what
is happening around your ship, so if it is wrong the app is effectively blind, and it will say
so at startup.

---

## AI Services

![AI services](images/ui-tab-settings-ai.png)

**Rewritten in V1.1.** The old scattered "Use" checkboxes are gone. There are now two switches
— one for the language model, one for speech — and the unused side of each is dimmed so it is
obvious which one is live.

This is also the one tab in the app that works on a **draft**. Nothing is written until you
press **Save**, and trying to leave with unsaved edits asks you to *Save*, *Discard*, or
*Keep editing*.

### Language Model (LLM)

Switch between **Local Setup** and **Cloud Setup**.

**Local Setup**

| Field | Notes |
|-------|-------|
| **Host** | `Ollama` or `LM Studio`. Each keeps its own address and model, so you can switch back and forth without retyping |
| **Address** | Defaults to that host's usual URL. Point it at another machine's IP if inference runs elsewhere on your LAN |
| **Model** | The model name. **One field** — V1.1 uses a single model for both commands and queries |

The default and recommended local model is **`google/gemma-4-e4b`**. Elite Intel warns you at
startup if your local model is something else; other models may work poorly or not at all.

Setup guides: [Ollama on Linux](Install-Ollama-Local-LLM-Linux) ·
[Ollama on Windows](Install-Ollama-Local-LLM-Windows) ·
[LM Studio on Linux](Install-LM-Studio-Linux) ·
[LM Studio on Windows](Install-LM-Studio-Windows) ·
[AMD RX series](AMD-RX-7800XT-LLM-Setup)

**Cloud Setup**

One field: your **API Key**, with a **Locked** checkbox beside it so a saved key cannot be
edited by accident. Uncheck Locked to change it.

Supported providers: **Gemini, Grok, OpenAI, Claude, DeepSeek, Mistral.**

> You no longer pick a model. Elite Intel recognises the provider from the shape of your key
> and selects the right model itself.

Mistral has a free tier and is the easiest way to start.
See [Cloud LLM options](cloud-llm-options) for how to obtain a key from each provider.

### Speech (TTS)

Switch between **Local · Kokoro** and **Cloud · Google**.

- **Local · Kokoro** has no configuration at all. 53 voices, built in, no key, no download.
- **Cloud · Google** needs a **Google TTS Key**, with the same Locked checkbox.

> Switching engines resets every ship's voice to the new engine's default voice. Ship
> personalities are kept. You are asked to confirm before it happens.

### Footer

**Restore Defaults** resets the language-model configuration to local LM Studio with the
default model, and saves immediately. **Save** commits everything else; it is greyed out until
something actually changes, and an **Unsaved changes** hint appears next to it when it does.

Saving restarts only what it has to — changing the model restarts the brain, changing the
speech key restarts the mouth.

---

## Audio

![Audio settings](images/ui-tab-settings-audio.png)

### Audio Devices

**Mic** and **Speaker** dropdowns, or *(System Default)*. The same pickers are available from
the **Audio Devices** button on the Vega tab.

> Device changes take effect on the **next service start**.

**Enable Noise Reduction** with a **Low / Medium / High** strength. Start at Medium. High is
for genuinely noisy rooms — it is aggressive, and over-filtering can cost you transcription
accuracy.

### Audio Levels

| Slider | What it does |
|--------|--------------|
| **Speech Volume** | How loud Vega speaks |
| **TTS Voice Speed** | How fast Vega speaks |
| **Beep Volume** | The confirmation beep — it fires when speech-to-text has finished and the language model has your input |
| **STT Threads** | CPU threads for transcription (4–11). A minimum request, not a reservation: the app asks for this many, uses what the processor gives it, and releases them when the work is done |

### Microphone Monitor

A live meter down the right side. Three things to read on it:

- **FLOOR** — your noise level when you are *not* speaking.
- **GATE** — the threshold. Audio above the gate is streamed for transcription; when it drops
  below, what was captured is transcribed and sent to the language model.
- **CLIP** — you are overdriving the microphone. Anything above this line transcribes badly.

You want a clear gap between FLOOR and your speaking level, and nothing touching CLIP. If that
is not what you see, run **CALIBRATE AUDIO** on the Vega tab — it sets the gate for you, and
warns you if the speech-to-noise gap is too small to work with.

---

## Push To Talk

![Push to talk](images/ui-tab-settings-push-to-talk.png)

Push-to-talk works with a **game controller or HOTAS button**, not a keyboard. You give up one
button and you gain a microphone that is closed except when you want it open.

| Control | Notes |
|---------|-------|
| **Enable Push to Talk** | The master switch. Everything else is disabled until this is on |
| **Controller** | Any connected controller Elite Intel can see. It re-selects your saved controller automatically when it reconnects |
| **Button** | Which button on it |

Two modes:

- **Toggle to sleep / wake** — the button flips Vega between sleeping and listening. While
  asleep, Vega ignores everything except `Wake up!`, and the `listen` / `listen up` bypass word
  still gets a single command through: *"Listen up — lower the landing gear."*
- **Push to Talk** — Vega ignores everything by default. Hold the button, hear a beep, speak,
  release. A second beep confirms your input is being processed.

While push-to-talk is active, the **SLEEP / WAKE UP** button on the Vega tab is disabled — the
controller button is the gate.

The button works whether or not you ever open this tab.

---

## Where the settings live

All settings and data are stored in a SQLite database:

- **Linux:** `~/.local/share/elite-intel/elite-intel/db/`
- **Windows:** `%APPDATA%\elite-intel\db\`

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
