# EliteIntel Privacy Policy

This policy explains what data is handled, how it is used, and what choices are available.

*Last Updated: October 25, 2025*

## Overview

EliteIntel is an open-source application available on [GitHub](https://github.com/stone-alex/EliteIntel). It uses speech-to-text (STT), text-to-speech (TTS), and large language models (LLM) to process game data.

The app can run completely offline using local STT (NVIDIA Parakeet), local LLM (Ollama), and local TTS (Kokoro). No data leaves the machine in this configuration. When cloud services are used, data is transmitted as described below.

## What Data Is Handled?

No personal information is collected, including names, addresses, or location data. The following data types are handled:

- **API Keys**: Used to authenticate requests to cloud TTS and LLM services. Stored encrypted in a local SQLite database. Transmitted only in request headers to the relevant services (Google for TTS; xAI, OpenAI, or Anthropic for LLM).

- **Text Data (TTS)**: When using Google TTS, response text is sent to Google. When using Kokoro TTS, no data leaves the machine.

- **Game Data (LLM)**: Relevant game data (mission details, market data, scan results, etc.) is sent to the configured LLM. The Commander name is never transmitted. The AI addresses you by your configured title, honorific, or nickname.

## How Is This Data Used?

- **API Keys**: Retained in the local database and used only to authenticate requests to third-party services.

- **Audio and Text**: Sent to Google for TTS processing only. Google processes the data according to its own privacy policy. Data retention for service improvement is off by default in standard API usage.

- **Game Data**: The app does not stream all game events to the LLM. It collects and stores relevant data locally, then sends targeted excerpts when a command or query is issued. The LLM has no persistent access to game data.

## Where Does Data Go?

- **Google (TTS)**: Text is sent to Google Cloud services. Governed by [Google's Privacy Policy](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)**: Game data excerpts are sent to whichever cloud LLM is configured. Governed by their respective privacy policies.

- **Nowhere else**: No data is stored externally, sold, or shared with third parties. The full source code is available at [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel).

## Rights and Choices

- **Inspect the Code**: The full source code is available on GitHub.
- **Go Fully Offline**: Use Parakeet, Ollama or LM Studio, and Kokoro. No data is transmitted in this configuration.
- **Configure Providers**: Select which cloud LLM to use, if any. API keys are managed in the local database.
- **Delete Your Data**: No data is stored externally. For data held by Google, xAI, OpenAI, or Anthropic, refer to their respective policies.

## Security

API keys are stored encrypted in a local database and transmitted only in request headers. The app complies with Elite Dangerous Terms of Service. It does not read game memory or use overlays. The open-source codebase allows community review.

## Changes to This Policy

Policy updates are noted in the GitHub repository and may appear in-app. Monitor [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) for changes.

## Questions

Open an issue on GitHub or contact via Matrix.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
