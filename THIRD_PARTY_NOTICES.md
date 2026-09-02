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

## jaudiotagger 3.0.1

- Maven coordinates: `net.jthink:jaudiotagger:3.0.1`
- Purpose: reading title, artist, album, track number and duration from the commander's own music files for the Jukebox playlist. It reads metadata only and never decodes or writes audio.
- License: GNU Lesser General Public License, version 2.1 or later.
- Source: [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger/src/master/).

The dependency is unmodified and is included in `elite_intel.jar` by the Gradle Shadow plugin. The LGPL text is packaged under `META-INF/licenses/`, and EliteIntel's source and Gradle build scripts provide what is needed to rebuild the combined JAR against an interface-compatible version of the library.

## jFLAC 1.5.2

- Maven coordinates: `org.jflac:jflac-codec:1.5.2`
- Purpose: pure-Java decoding of the commander's own FLAC files into PCM for the Jukebox. Decoding only; nothing is encoded or written.
- License: GNU Library General Public License, version 2 or later, per the copyright headers carried by the source files (a Java port of Josh Coalson's libFLAC). The published module POM declares no license, so the source headers are the authority.
- Source: [jFLAC](https://github.com/nokiaguy/jFLAC).

The dependency is unmodified and is included in `elite_intel.jar` by the Gradle Shadow plugin. EliteIntel's source and Gradle build scripts provide what is needed to rebuild the combined JAR against an interface-compatible version of the library.

## radiorecorder-aac 1.11.1

- Maven coordinates: `de.sfuhrm:radiorecorder-aac:1.11.1`
- Purpose: pure-Java decoding of AAC audio in MP4 containers (`.m4a`, `.m4b`) into PCM for the Jukebox. Decoding only.
- License: Apache License, Version 2.0.
- Provenance: a patched build of JAAD carried by the
  [radiorecorder](https://github.com/sfuhrm/radiorecorder) project. Preferred over the otherwise equivalent
  `com.tianscar.javasound:javasound-aac`, whose upstream repository is no longer reachable and which registers Java Sound SPI providers globally.

The dependency is unmodified and is included in `elite_intel.jar` by the Gradle Shadow plugin.

## JOrbis 0.0.17.4

- Maven coordinates: `com.googlecode.soundlibs:jorbis:0.0.17.4`
- Purpose: pure-Java decoding of the commander's own Ogg Vorbis files into PCM for the Jukebox. Decoding only.
- License: GNU Library General Public License, version 2 or later, per the copyright headers carried by the source files (JCraft/ymnk, based on Xiph.Org's Vorbis codec).
- Provenance: a Maven repackaging, by the soundlibs project, of
  [JCraft's JOrbis](http://www.jcraft.com/jorbis/). The artifact carries `com.jcraft.jogg` alongside
  `com.jcraft.jorbis`, so the Ogg container support comes from the same dependency.

The dependency is unmodified and is included in `elite_intel.jar` by the Gradle Shadow plugin. EliteIntel's source and Gradle build scripts provide what is needed to rebuild the combined JAR against an interface-compatible version of the library.

## Jukebox audio test fixtures

`app/src/test/resources/jukebox/` holds short synthetic MP3, FLAC, M4A/M4B and Ogg Vorbis files - a few seconds at most of a plain sine tone, generated with FFmpeg for this repository. They carry no third-party audio and exist so the decoders, the resampler and the tag reader are tested against real files of each format rather than against mocks. Two of them hold a codec this build deliberately cannot decode, so that refusing such a file is tested too: `lossless-alac.m4a` is Apple Lossless rather than AAC, and `opus-in-ogg.ogg` is Opus rather than Vorbis.

## MP3 decoder test fixture

`EdgeMp3DecoderTest` contains a short excerpt of Espressif's 24 kHz mono MP3 test sample, “Furious Freak”
by Kevin MacLeod. It is used only by the offline test suite and is attributed under
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/) with its
[original source](https://dl.espressif.com/dl/audio/ff-16b-1c-24000hz.mp3).
