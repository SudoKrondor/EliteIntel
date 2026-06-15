# Elite Intel
## AI Assistant for Elite Dangerous

![EliteIntel app](images/app.png)

This page provides an overview of Elite Intel: its capabilities, limitations, and core components.

## Overview

Elite Intel is an AI assistant for Elite Dangerous. It understands natural language and does not require memorizing command lists. It interprets intent from spoken input. Natural phrasing is accepted. Formal command syntax is not required.

Clear, unambiguous phrasing produces more reliable results.

The full stack (Speech-to-Text, LLM, and Text-to-Speech) can run completely offline on local hardware. No cloud connection, subscription, or data transmission is required in offline mode. Components can also be mixed: local STT with a cloud LLM, or cloud TTS with local inference.

## Speech-to-Text (STT)

STT converts microphone input into text for the AI to process. Microphone quality significantly affects recognition accuracy.

The app auto-detects audio settings. Run **Recalibrate Audio** on the AI tab before the first session. This calibrates the engine to room noise and microphone characteristics.

**Parakeet STT**: Elite Intel includes the NVIDIA Parakeet speech recognition engine on both Windows and Linux. No additional downloads are required. Enable it by checking the **Built In Speech Recognition ☑ Use** checkbox on the Settings tab. No API keys or cloud connection are required.

STT accuracy is not guaranteed. Some Elite Dangerous commodity and system names are difficult for STT to recognize accurately. If recurring errors occur, add corrections to the dictionary file in the EliteIntel install directory. Format: `"grande"="grandidierite"`. Corrections are applied automatically. Share new corrections on Matrix for inclusion in the default dictionary.

Once converted to text, the AI classifies input as a command, query, or conversation. For example, "lower the landing gear" triggers the corresponding key binding. Vague input such as "prepare for landing" may result in a verbal acknowledgment without action. The AI interprets intent. Clear phrasing produces more reliable results.

## Text-to-Speech (TTS)

Two TTS options are available:

**Google TTS (cloud)**: 14 voices to choose from, full personality and profile support. Requires a Google Cloud API key and internet connection.

**Kokoro TTS (built-in, offline)**: Runs locally with no setup, no extra downloads, and no external service. It supports the full voice and personality system. Enable it via the **☑ Use** checkbox on the Settings tab.

Short commands such as "deploy hardpoints" typically produce a brief acknowledgment and immediate action. Detailed queries such as "What is the security status of our next jump?" return a full response, provided a system is targeted in the nav panel.

## Large Language Model (LLM)

The LLM provides reasoning, intent interpretation, and response generation. It can run locally via Ollama (free, offline) or through a cloud provider such as Mistral (Free with hourly limit) or Paid such as xAI, Gemini, OpenAI, or Anthropic.

The LLM does more than trigger key bindings. It applies domain knowledge to provide analysis. For example, asking to analyze the ship's loadout returns a breakdown of strengths and weaknesses. These assessments are informational and based on general knowledge.

Queries such as "Analyze local signals" or "What is on the scanners?" draw from in-app session data including auto-scans and FSS results. Not all in-game information is accessible. If data is visible on in-game panels but not present in journal files or EDSM, the AI will report "No data available." Elite Intel complies with the game's Terms of Service and does not read in-game memory. Some information visible to the player may not be accessible to the AI.

## User Interface

The interface is minimal. It contains fields for API keys (required for cloud services only), buttons to start and stop services, and no other configuration panels. All other interaction occurs through voice.

Elite Intel does not fly the ship autonomously. Maintain direct control over critical systems: speed, heading, FTL, countermeasures, and weapons. Network latency, STT errors, unsupported commands, or software bugs may cause unexpected behavior.

## Community

To report bugs, suggest features, or share feedback: the project community is active on Matrix. Bug reports and pull requests are welcome. Contributions improve the experience for all users.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

----
Experiment with queries and commands to discover available capabilities. Maintain manual control of critical ship systems at all times.
