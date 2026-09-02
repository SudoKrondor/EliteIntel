package elite.intel.jukebox;

/**
 * A growable byte queue of canonical PCM, filled by a {@link PcmResampler} and drained by the player.
 * <p>
 * It sits between the two because they work in different units and at different rates: the resampler
 * emits whole frames as it produces them, while the player asks for a fixed block of bytes however many
 * frames that turns out to span. A decoded frame is rarely the size of a playback block, so one of the
 * two has to keep a remainder, and it is this.
 * <p>
 * Shared by every decoder rather than owned by one, because the shape of the problem is the same
 * whatever the container was: a codec hands over audio in its own natural chunk and the output line
 * wants it in the line's.
 */
final class PcmBuffer implements PcmResampler.ByteSink {

    private byte[] bytes = new byte[MusicFormat.BLOCK_BYTES * 8];
    private int head;
    private int tail;

    @Override
    public void putFrame(short left, short right) {
        ensureRoom(MusicFormat.FRAME_BYTES);
        tail = writeSample(left, tail);
        tail = writeSample(right, tail);
    }

    int size() {
        return tail - head;
    }

    int drainInto(byte[] destination, int offset, int length) {
        int taken = Math.min(length, size());
        System.arraycopy(bytes, head, destination, offset, taken);
        head += taken;
        if (head == tail) {
            head = 0;
            tail = 0;
        }
        return taken;
    }

    private int writeSample(short sample, int at) {
        bytes[at] = (byte) (sample & 0xFF);
        bytes[at + 1] = (byte) ((sample >>> 8) & 0xFF);
        return at + 2;
    }

    private void ensureRoom(int needed) {
        if (tail + needed <= bytes.length) return;
        if (head > 0) {
            System.arraycopy(bytes, head, bytes, 0, size());
            tail -= head;
            head = 0;
        }
        if (tail + needed > bytes.length) {
            byte[] bigger = new byte[Math.max(bytes.length * 2, tail + needed)];
            System.arraycopy(bytes, 0, bigger, 0, tail);
            bytes = bigger;
        }
    }
}
