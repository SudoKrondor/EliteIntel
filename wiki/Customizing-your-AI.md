# Customizing Elite Intel

Elite Intel functions as the ship's voice and AI brain. With Kokoro as the built-in default TTS engine and most users running fully offline (Kokoro TTS and a local LLM via Ollama), this guide explains how to customize voice and personality settings.

## Voices

Kokoro is the default TTS engine. It runs offline with no cloud dependency. Available voices:

- **American female**: Heart, Bella, Nicole (whispering), Sky, Anna
- **American male**: Michael
- **British female**: Isabella, Emma
- **British male**: George, Jason, Daniel

Google TTS (cloud) is also available when enabled. Its voice set is separate from Kokoro's.

**Note**: Starting with version 0351, ship voice, personality, and cadence can only be changed in the UI, not via voice command. The AI selects from the active engine (Kokoro by default).

## Ship Identity

The AI speaks as the ship. It uses "I," "my," and "me" when referring to itself.

Examples:
- "What is your loadout?" returns current ship modules.
- "What is your jump range?" returns the current laden and unladen range.
- "How much fuel do I have?" returns the ship's fuel level.

To query fleet carrier information instead, address the carrier explicitly:
- "What is the fleet carrier's range?"
- "Tell me about the carrier's jump range."

Otherwise, the AI assumes the query refers to the ship.

## Personalities, Profiles, and Cadence

**Offline mode (Kokoro + local LLM)** does not support custom personalities, faction profiles (Imperial/Federation/Alliance), or special cadence settings. The local model responds in its default trained style.

**Cloud LLM users** (Claude, Grok, OpenAI, etc.) have access to the full feature set:
- Professional / Friendly / Unhinged / Rogue personalities
- Imperial / Federation / Alliance profiles (affects tone and phrasing)
- Temperature control (low = fast and concise, high = creative and slower)

**Note**: Starting with version 0351, personality and cadence can only be changed in the UI.

Offline mode provides low latency at no API cost, with plain responses. Cloud LLMs provide additional personality options.

## Tips

- Start with Kokoro defaults. Try different voices to find a preferred option. British male voices (George, Jason, Daniel) complement an Imperial aesthetic even without profile support.
- Offline mode is fast and free. Cloud mode offers additional personality options but incurs API costs.
- Experimenting with phrasing can improve the experience with ship-as-speaker queries.

Report issues, unexpected behavior, or suggestions on Matrix.

Community 👉 [**Matrix**](https://matrix.to/#/#krondor:matrix.org) 👈
