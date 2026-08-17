package elite.intel.ai.mouth.edge;

import dev.mccue.jlayer.decoder.Bitstream;
import dev.mccue.jlayer.decoder.Decoder;
import dev.mccue.jlayer.decoder.Header;
import dev.mccue.jlayer.decoder.JavaLayerException;
import dev.mccue.jlayer.decoder.Obuffer;
import dev.mccue.jlayer.decoder.SampleBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Direct pure-Java MP3 decoding; it does not depend on optional Java Sound MP3 service providers. */
final class EdgeMp3Decoder implements EdgeAudioDecoder {
    private static final int[] MPEG_1_LAYER_3_BITRATES =
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    private static final int[] MPEG_2_LAYER_3_BITRATES =
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0};
    private static final int[] MPEG_1_SAMPLE_RATES = {44_100, 48_000, 32_000};

    @Override
    public byte[] decode(byte[] compressedAudio) throws IOException {
        if (compressedAudio == null || compressedAudio.length == 0) {
            throw new IOException("Edge returned empty MP3 audio");
        }
        validateCompleteFrames(compressedAudio);
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(compressedAudio));
        Decoder decoder = new Decoder();
        ByteArrayOutputStream pcm = new ByteArrayOutputStream(compressedAudio.length * 8);
        IOException failure = null;
        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                try {
                    Obuffer decoded = decoder.decodeFrame(header, bitstream);
                    if (!(decoded instanceof SampleBuffer samples)) {
                        throw new IOException("Edge MP3 decoder returned an unexpected sample buffer");
                    }
                    appendSamples(samples, pcm);
                } finally {
                    bitstream.closeFrame();
                }
            }
        } catch (IOException e) {
            failure = e;
        } catch (JavaLayerException | RuntimeException e) {
            failure = new IOException("Could not decode Edge MP3 audio", e);
        } finally {
            try {
                bitstream.close();
            } catch (JavaLayerException e) {
                if (failure == null) {
                    failure = new IOException("Could not close Edge MP3 decoder", e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        if (pcm.size() == 0) {
            throw new IOException("Edge MP3 audio contained no decodable samples");
        }
        return pcm.toByteArray();
    }

    private static void appendSamples(SampleBuffer samples, ByteArrayOutputStream pcm) throws IOException {
        if (samples.getSampleFrequency() != EdgeProtocolConstants.SAMPLE_RATE
                || samples.getChannelCount() != 1) {
            throw new IOException("Edge MP3 decoded to " + samples.getSampleFrequency() + " Hz, "
                    + samples.getChannelCount() + " channels; expected 24000 Hz mono");
        }
        short[] buffer = samples.getBuffer();
        for (int i = 0; i < samples.getBufferLength(); i++) {
            short sample = buffer[i];
            pcm.write(sample & 0xFF);
            pcm.write((sample >>> 8) & 0xFF);
        }
    }

    private static void validateCompleteFrames(byte[] mp3) throws IOException {
        int offset = id3v2Length(mp3);
        int frameCount = 0;
        while (offset < mp3.length) {
            if (mp3.length - offset == 128 && startsWith(mp3, offset, 'T', 'A', 'G')) {
                offset += 128;
                break;
            }
            if (mp3.length - offset < 4) {
                throw new IOException("Edge MP3 ended inside an MPEG frame header");
            }
            int header = ((mp3[offset] & 0xFF) << 24)
                    | ((mp3[offset + 1] & 0xFF) << 16)
                    | ((mp3[offset + 2] & 0xFF) << 8)
                    | (mp3[offset + 3] & 0xFF);
            int frameLength = frameLength(header);
            if (offset + frameLength > mp3.length) {
                throw new IOException("Edge MP3 ended inside an MPEG audio frame");
            }
            offset += frameLength;
            frameCount++;
        }
        if (offset != mp3.length || frameCount == 0) {
            throw new IOException("Edge MP3 contains no complete MPEG audio frames");
        }
    }

    private static int id3v2Length(byte[] mp3) throws IOException {
        if (mp3.length < 3 || !startsWith(mp3, 0, 'I', 'D', '3')) {
            return 0;
        }
        if (mp3.length < 10) {
            throw new IOException("Edge MP3 contains a truncated ID3 header");
        }
        int size = 0;
        for (int i = 6; i < 10; i++) {
            if ((mp3[i] & 0x80) != 0) {
                throw new IOException("Edge MP3 contains an invalid ID3 size");
            }
            size = (size << 7) | (mp3[i] & 0x7F);
        }
        int total = 10 + size + (((mp3[5] & 0x10) != 0) ? 10 : 0);
        if (total > mp3.length) {
            throw new IOException("Edge MP3 contains a truncated ID3 tag");
        }
        return total;
    }

    private static int frameLength(int header) throws IOException {
        if ((header & 0xFFE0_0000) != 0xFFE0_0000) {
            throw new IOException("Edge MP3 contains an invalid MPEG frame sync");
        }
        int version = (header >>> 19) & 0x3;
        int layer = (header >>> 17) & 0x3;
        int bitrateIndex = (header >>> 12) & 0xF;
        int sampleRateIndex = (header >>> 10) & 0x3;
        int channelMode = (header >>> 6) & 0x3;
        if (version == 1 || layer != 1 || bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) {
            throw new IOException("Edge MP3 contains an unsupported MPEG frame header");
        }
        if (channelMode != 3) {
            throw new IOException("Edge MP3 is not mono");
        }
        int sampleRate = MPEG_1_SAMPLE_RATES[sampleRateIndex];
        if (version == 2) {
            sampleRate /= 2;
        } else if (version == 0) {
            sampleRate /= 4;
        }
        if (sampleRate != EdgeProtocolConstants.SAMPLE_RATE) {
            throw new IOException("Edge MP3 sample rate is " + sampleRate + " Hz; expected 24000 Hz");
        }
        int[] table = version == 3 ? MPEG_1_LAYER_3_BITRATES : MPEG_2_LAYER_3_BITRATES;
        int bitrate = table[bitrateIndex] * 1_000;
        int coefficient = version == 3 ? 144 : 72;
        int padding = (header >>> 9) & 1;
        return coefficient * bitrate / sampleRate + padding;
    }

    private static boolean startsWith(byte[] data, int offset, int first, int second, int third) {
        return data.length - offset >= 3
                && data[offset] == first && data[offset + 1] == second && data[offset + 2] == third;
    }
}
