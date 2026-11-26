# Customizing Elite Intel

Elite Intel functions as the ship's voice and AI brain. This guide explains how to customize its voice and personality. Each ship in your fleet can have its own, set on the [Commander tab](UI-Commander-Tab).

## Voices

**Supertonic 3** is the built-in voice engine. It runs on your PC with no cloud dependency, and every voice speaks every language Elite Intel supports. There are 45 voices, including:

- **Female**: Lily (the default), Heart, Bella, Nicole, Sky, Emma, Isabella, Astra, Lyra
- **Male**: George, Daniel, Michael, Lewis, Fable, Orion, Atlas, Draven

Your pick also decides whether Vega speaks of itself as *he* or *she* on that ship.

If a voice is a little too quiet, too fast, or too bright for your ear, adjust it with [Voice Tuning](UI-Settings-Tab): speed, volume and a six-band equalizer for each voice.

Google TTS (cloud) is also available if you add an API key. Its voice set is separate.

**Note**: Ship voice and personality can only be changed in the UI, not via voice command.

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

## Personalities

Each ship has a personality: Professional, Casual, Friendly, Unhinged, Seven of Nine, F.R.I.D.A.Y., Mouthy Merc, Your Ex-Boyfriend, Your Ex-Girlfriend or Rogue. How creative the replies are depends on the personality you pick and on the capabilities of your language model.

A local model has no API cost and low latency. A cloud model can cost money, depending on the provider, and is usually more inventive.

## Tips

- Try several voices on the Commander tab. Changing a ship's voice plays a line in it straight away.
- Experimenting with phrasing can improve the experience with ship-as-speaker queries.

Report issues, unexpected behavior, or suggestions on Matrix.

Community 👉 [**Matrix**](https://app.element.io/#/room/#krondor:matrix.org) 👈
