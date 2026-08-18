package elite.intel.ai.mouth.edge;

import java.io.IOException;

/** Decodes Edge's compressed response to 24 kHz, signed PCM-16 mono little-endian audio. */
interface EdgeAudioDecoder {
    byte[] decode(byte[] compressedAudio) throws IOException;
}
