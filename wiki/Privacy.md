# EliteIntel Privacy Policy

This policy explains what data is handled, how it is used, and what choices are available.

*Last Updated: November 14, 2026*

## Overview

EliteIntel is an open-source application available on [GitHub](https://github.com/stone-alex/EliteIntel). It uses speech-to-text (STT), text-to-speech (TTS), and large language models (LLM) to process game data.

The app can run completely offline using local STT (NVIDIA Parakeet), local LLM (LM Studio), and local TTS (Supertonic). In this configuration nothing leaves the machine except the daily install count described below. When cloud services are used, data is transmitted as described below.

## What Data Is Handled?

No personal information is collected, including names, addresses, or location data. The following data types are handled:

- **API Keys**: Used to authenticate requests to cloud TTS and LLM services. Stored encrypted in a local SQLite database. Transmitted only in request headers to the relevant services (Google for TTS; xAI, OpenAI, or Anthropic for LLM).

- **Text Data (TTS)**: When using Google TTS, response text is sent to Google. When using Supertonic TTS, no data leaves the machine.

- **Game Data (LLM)**: Relevant game data (mission details, market data, scan results, etc.) is sent to the configured LLM. The Commander name is never transmitted. The AI addresses you by your configured title, honorific, or nickname.

- **Install Statistics**: Once a day the app sends its version number and operating system (Windows or Linux) to Google Firebase, under a random anonymous ID created for this install. Nothing else is sent: no commander name, no game data, no settings. It is used only to count how many installs are active, and on which version and operating system.

- **Massacre Sets (opt-in)**: Only while sharing is turned on (Commander tab, Massacre Sets page, off by default), the app sends the pirate massacre sets it has learned from the journals: star system, faction and station names, resource site counts, and the day a set was last seen. No commander name, mission ID, reward or anything else that identifies a player is sent. Sets from all installs are checked and published at [elite-intel.org/data/massacre-sets.json](https://www.elite-intel.org/data/massacre-sets.json), without saying who reported them.

## How Is This Data Used?

- **API Keys**: Retained in the local database and used only to authenticate requests to third-party services.

- **Audio and Text**: Sent to Google for TTS processing only. Google processes the data according to its own privacy policy. Data retention for service improvement is off by default in standard API usage.

- **Game Data**: The app does not stream all game events to the LLM. It collects and stores relevant data locally, then sends targeted excerpts when a command or query is issued. The LLM has no persistent access to game data.

## Where Does Data Go?

- **Google (TTS)**: Text is sent to Google Cloud services. Governed by [Google's Privacy Policy](https://policies.google.com/privacy).

- **xAI / OpenAI / Anthropic (LLM)**: Game data excerpts are sent to whichever cloud LLM is configured. Governed by their respective privacy policies.

- **Google Firebase (install statistics and massacre sets)**: Stored in Google Cloud Firestore under the anonymous install ID. Each install can write only its own records, and nothing can be read back through the app. A nightly job run by the EliteIntel maintainers reads them to publish the combined counts and the checked sets. As with any internet request, Google sees the connecting IP address, governed by [Google's Privacy Policy](https://policies.google.com/privacy). EliteIntel does not store it.

- **Nowhere else**: No data is sold or shared with third parties. The full source code is available at [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel).

## Rights and Choices

- **Inspect the Code**: The full source code is available on GitHub.
- **Go Fully Offline**: Use Parakeet, LM Studio, and Supertonic. Apart from the daily install count, no data is transmitted in this configuration.
- **Configure Providers**: Select which cloud LLM to use, if any. API keys are managed in the local database.
- **Delete Your Data**: Turning off massacre set sharing deletes this install's sets from Firebase. Install statistics hold no personal data. For data held by Google, xAI, OpenAI, or Anthropic, refer to their respective policies.

## Security

API keys are stored encrypted in a local database and transmitted only in request headers. The app complies with Elite Dangerous Terms of Service. It does not read game memory or use overlays. The open-source codebase allows community review.

## Changes to This Policy

Policy updates are noted in the GitHub repository and may appear in-app. Monitor [github.com/stone-alex/EliteIntel](https://github.com/stone-alex/EliteIntel) for changes.

## Questions

Open an issue on GitHub or contact via Matrix.

----
Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
