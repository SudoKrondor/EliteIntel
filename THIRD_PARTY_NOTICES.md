# Third-Party Notices

EliteIntel is distributed under the repository's `LICENSE`. The following component retains its own license.

## jlayer-decoder 2024.04.19

- Maven coordinates: `dev.mccue:jlayer-decoder:2024.04.19`
- Purpose: pure-Java decoding of Microsoft Edge Read Aloud's MPEG audio into PCM.
- Provenance: a repackaged and modernized subset of JavaLayer from the
  [java-audio-stack project](https://github.com/bowbahdoe/java-audio-stack/tree/v2024.04.19/jlayer-decoder).
- Published module POM license metadata: GNU Lesser General Public License, version 2.1. The source tag's
  top-level `LICENSE` contains LGPL-3.0; both texts are included because the upstream metadata is inconsistent.
- Source: [java-audio-stack v2024.04.19](https://github.com/bowbahdoe/java-audio-stack/tree/v2024.04.19).

The dependency is unmodified and is included in `elite_intel.jar` by the Gradle Shadow plugin. The complete
LGPL texts are packaged under `META-INF/licenses/`. EliteIntel's source and Gradle build scripts provide the
application code needed to rebuild the combined JAR with an interface-compatible version of the library.

## MP3 decoder test fixture

`EdgeMp3DecoderTest` contains a short excerpt of Espressif's 24 kHz mono MP3 test sample, “Furious Freak”
by Kevin MacLeod. It is used only by the offline test suite and is attributed under
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) with its
[original source](https://dl.espressif.com/dl/audio/ff-16b-1c-24000hz.mp3).
