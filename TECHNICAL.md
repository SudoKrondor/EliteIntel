# EliteIntel - Technical Architecture

> This document is intended for developers and engineers interested in the architecture and
> design decisions behind EliteIntel. For user documentation, see the [Wiki](https://github.com/stone-alex/EliteIntel/wiki).

---

## Overview

EliteIntel is a Java 21 desktop application providing real-time AI-assisted voice interaction
for the game *Elite Dangerous*. It ingests live game telemetry, routes natural-language voice
commands through an LLM inference pipeline, and executes keyboard-mapped ship controls - all
while remaining fully compliant with Frontier Developments' Terms of Service.

The application runs on Linux and Windows, supports fully offline operation, and has real users
in a growing community.

---

## Core Architecture

### Event-Driven, Decoupled Design

The application is built around a central event registry using **reflection-based handler
discovery**. Components register interest in event types at startup; at runtime they communicate
exclusively via events with no direct inter-component dependencies.

This means the journal parser, AI inference pipeline, voice I/O layer, and ship control
dispatcher are entirely decoupled. Adding a new capability - a new command type, a new data
source, a new provider - requires no changes to existing components.

### Multithreaded Pipeline

Audio capture, STT transcription, LLM inference, TTS synthesis, and game control dispatch
run on independent threads. Cloud STT is optimized for response and performance, the audio is
streamed to provider from the moment a mic gate opens to the moment it closes the server
begins processing the audio before the user finishes the utterance.

In Professional mode (LLM temperature 0.1), round-trip latency for a simple ship command
such as `deploy_landing_gear` is approximately 750ms over a typical cloud connection.

---

## AI Provider Abstraction

### Pluggable Provider Pattern

All LLM providers implement a common interface. The active provider is selected at runtime
from configuration with no changes to the inference pipeline or command handling logic.

Supported providers:

| Provider         | Type            | Notes                                |
|------------------|-----------------|--------------------------------------|
| LMStudio         | Local / offline | Fast. Best choice                    |
| Ollama           | Local / offline | Slow, unless you have the extra hardware |
| Anthropic Claude | Cloud           | Sonnet / Haiku                       |
| xAI Grok         | Cloud           | Fast non-reasoning model             |
| OpenAI ChatGPT   | Cloud           | GPT-4 class                          |
| Gemini           | Cloud           | gemini-3.1-flash-lite-preview        |

### Dual-Model Local Inference

When running against Ollama, the application uses **two separate models** for different
cognitive loads:

- **Action model** (e.g. `Tulu-3.1-8B-SuperNova-Q4_K_M`) - intent classification ship control, data analysis and queries.

### Temperature as a Design Parameter

(Cloud LLMs only)
LLM temperature is a first-class configuration value tied to the user-selectable personality mode, not a hidden constant:

| Personality  | Temperature | Behaviour                                   |
|--------------|-------------|---------------------------------------------|
| Professional | 0.1         | Deterministic, low-latency, concise         |
| Friendly     | 0.5         | Conversational, moderate creativity         |
| Unhinged     | 0.8         | High creativity, tangential responses       |
| Rogue        | 1.0         | Maximum creativity, slowest, most expensive |

---

## Voice Pipeline

### STT (Speech-to-Text)

Two STT backends are supported behind a common interface:

- **Parakeet** (local, offline) - bundled with the installer, no API key required


### TTS (Text-to-Speech)

- **Kokoro** (local, offline) - JNI Invocation 0 network traffic - bundled with the installer, no API key required

- **Google Cloud TTS** (cloud) - 14 voices across British, American, and Australian accents,
  selected at runtime via voice command.

- **Microsoft Edge Read
  Aloud** (cloud) - an unofficial integration, selected with the MICROSOFT EDGE control on the AI Services settings tab and stored as `game_session.ttsProvider`. No API key is involved. It is not supported or endorsed by Microsoft. It uses native Java HTTP/WebSocket transport and an in-process MP3 decoder; no Azure credentials or browser automation are involved. This is a private consumer protocol and may require maintenance if Microsoft changes its endpoints, headers, token, or audio format.

---

## Game Data Pipeline

### Journal File Parsing

*Elite Dangerous* exposes game state via a structured JSON journal file that is appended to
in real time. EliteIntel tails this file and parses events as they are written, maintaining
a session state model that the LLM can query.

No memory reading, no overlay injection, no game client modification. All data access is
through the officially documented third-party API.

### Session State Model

The application maintains an in-memory session state across the following domains:

- Current star system, body, and coordinates
- FSS / DSS scan results for the current system
- Exobiology codex entries and sample locations (persisted across system exits and returns)
- Active missions (pirate massacre kill counts, target factions)
- Fleet carrier status (fuel, credits, jump destination)
- Cargo manifest
- Ship loadout

The LLM receives only the subset of session state relevant to the current query, not the
full session context, keeping token usage minimal.

### EDSM Integration

For data not available in the local journal (historical market prices, third-party system
data), the application queries the EDSM API. Results are cached in the session to avoid
redundant network calls.

---

## Security and Privacy

### API Key Storage

API keys for cloud providers are stored **encrypted in a local SQLite database**. Keys are
never logged, never transmitted except in request headers to the configured provider
endpoint, and never leave the user's machine in any other form.

### Microphone Gating

Two ways in, chosen on the SETTINGS / PUSH TO TALK tab, and never both at once:

- Built-in gate (the default) - the app listens continuously and its own noise gate decides what counts as speech. There is also a check box on the Player tab which, when UNCHECKED, makes the app ignore anything it cannot map to an action, so you can talk during play and
  **for the most part** it will only respond to a clear command or an implemented query.
- Push to talk - map a controller button, and the microphone is open only while that button is held. Anything captured without it is discarded before it reaches the AI.

#### Sleep / Wake

On top of the built-in gate sits Sleep/Wake, driven by the SLEEP / WAKE UP button on the AI tab or by voice ("go to sleep", "wake up"). Asleep, the app ignores you completely - the transcript is discarded before it reaches the AI - with two exceptions:

- a **wake phrase** ("wake", "wake up", "listen") wakes her again, and
- **"listen
  up"** followed by a request is a one-time bypass: the prefix is stripped and the request runs, without waking her for good.

The state is remembered between runs, so if you shut down asleep the app says so out loud on the next start rather than sitting there silently ignoring you. Sleep/Wake is unavailable while push to talk is on:
the mapped button already gates the microphone, so there is nothing left for it to gate.

### No Game Memory Access

The application does not read game client memory, inject into the game process, or use
overlays. All data is sourced from the journal file API or EDSM. This is a hard architectural
constraint, no in-game memory reading.

---

## Build and Distribution

- **Language**: Java 21
- **Build**: Gradle 9 with Gradle Wrapper
- **Packaging**: Fat JAR (`app-shadow.tar`) via Shadow plugin
- **Linux installer**: Bash script, no `sudo` required, installs to `~/.var/app/elite.intel.app`
- **Windows installer**: NSIS-based, standard next-next-done
- **Build from source**: `sh gradlew clean build` - requires Java 21 and Gradle Wrapper 8.7+

Local STT (Whisper) is bundled with the installer. Local TTS (Piper) and local LLM (Ollama)
are optional components installed separately via documented procedures.

---

## Design Principles

**DRY and SRP throughout.** New provider implementations follow the existing pattern - the
abstraction layer was designed from the first provider with extension in mind, not retrofitted
after the fact.

**Offline-first.** Every cloud dependency has a local alternative. A user with sufficient
hardware can run the entire stack - STT, LLM, TTS - with no internet connection and no
ongoing API cost.

**No in-game memory reading.** The architecture was designed around what the
official API exposes, not around what would be technically possible with memory access.
This is not a limitation - it is what makes the application distributable and trustworthy.

**Minimal UI surface.** Three API key fields, a handful of buttons, everything else via
voice. The interface does not compete with the game for screen real estate.

---

## Repository

[github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel)

Community: [Matrix #krondor:matrix.org](https://matrix.to/#/#krondor:matrix.org)
