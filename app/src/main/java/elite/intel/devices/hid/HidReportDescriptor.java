package elite.intel.devices.hid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Reads a raw HID report descriptor - the bytes Linux shows as {@code report_descriptor} - into the inputs it
 * declares, one per field, in descriptor order.
 * <p>
 * Only what naming the axes needs is read: usage pages, usages and usage ranges, delimiters, report counts, the
 * global stack, and Input items. An Input that is constant (padding) or an array (a button list or selector)
 * declares no axis and is skipped. The rest follows the kernel's {@code hid-core.c}, since on Linux it decides what
 * SDL sees: an Input gives as many fields as the larger of its report count and its usages, a field past the last
 * usage takes the last usage again, and of a delimited set of alternative usages only the first set is kept.
 * <p>
 * A short usage takes the usage page current when it is read. The kernel resolves it when the Input item is
 * read, which differs only for a descriptor changing the page between the two; no controller is known to.
 */
final class HidReportDescriptor {

    /** More fields than any controller declares in one item - past it, the bytes are not a descriptor. */
    private static final int MAX_FIELDS_PER_ITEM = 4096;

    private static final int LONG_ITEM = 0xFE;
    private static final int TYPE_MAIN = 0, TYPE_GLOBAL = 1, TYPE_LOCAL = 2;
    private static final int MAIN_INPUT = 0x8;
    private static final int GLOBAL_USAGE_PAGE = 0x0, GLOBAL_REPORT_COUNT = 0x9, GLOBAL_PUSH = 0xA, GLOBAL_POP = 0xB;
    private static final int LOCAL_USAGE = 0x0, LOCAL_USAGE_MINIMUM = 0x1, LOCAL_USAGE_MAXIMUM = 0x2;
    private static final int LOCAL_DELIMITER = 0xA;
    private static final int INPUT_CONSTANT = 0x1, INPUT_VARIABLE = 0x2;

    private int usagePage;
    private int reportCount;
    private final Deque<int[]> globalStack = new ArrayDeque<>();
    private final List<HidUsage> usages = new ArrayList<>();
    private HidUsage usageMinimum;
    private boolean inDelimiter;
    private int delimiterSets;
    private final List<HidUsage> inputs = new ArrayList<>();

    private HidReportDescriptor() {
    }

    /**
     * @throws IllegalArgumentException when the bytes end inside an item or declare an impossible field count
     */
    static List<HidUsage> inputs(byte[] descriptor) {
        HidReportDescriptor reader = new HidReportDescriptor();
        reader.read(descriptor);
        return List.copyOf(reader.inputs);
    }

    private void read(byte[] bytes) {
        int at = 0;
        while (at < bytes.length) {
            int prefix = bytes[at] & 0xFF;
            if (prefix == LONG_ITEM) {
                if (at + 1 >= bytes.length) throw new IllegalArgumentException("Long item cut short at byte " + at);
                at += 3 + (bytes[at + 1] & 0xFF);
                continue;
            }
            int size = (prefix & 0x3) == 3 ? 4 : prefix & 0x3;
            if (at + 1 + size > bytes.length) throw new IllegalArgumentException("Item cut short at byte " + at);
            long data = 0;
            for (int i = 0; i < size; i++) data |= (long) (bytes[at + 1 + i] & 0xFF) << (8 * i);
            item((prefix >> 2) & 0x3, prefix >> 4, data, size);
            at += 1 + size;
        }
    }

    private void item(int type, int tag, long data, int size) {
        switch (type) {
            case TYPE_MAIN -> {
                if (tag == MAIN_INPUT) input((int) data);
                usages.clear();
                usageMinimum = null;
                inDelimiter = false;
                delimiterSets = 0;
            }
            case TYPE_GLOBAL -> global(tag, data);
            case TYPE_LOCAL -> local(tag, data, size);
            default -> {
                // reserved item type: nothing to read
            }
        }
    }

    private void global(int tag, long data) {
        switch (tag) {
            case GLOBAL_USAGE_PAGE -> usagePage = (int) data;
            case GLOBAL_REPORT_COUNT -> {
                if (data > MAX_FIELDS_PER_ITEM) throw new IllegalArgumentException("Report count " + data);
                reportCount = (int) data;
            }
            case GLOBAL_PUSH -> globalStack.push(new int[]{usagePage, reportCount});
            case GLOBAL_POP -> {
                if (globalStack.isEmpty()) throw new IllegalArgumentException("Pop with nothing pushed");
                int[] saved = globalStack.pop();
                usagePage = saved[0];
                reportCount = saved[1];
            }
            default -> {
                // logical and physical ranges, units, report size and id: not needed to name an input
            }
        }
    }

    private void local(int tag, long data, int size) {
        if (tag == LOCAL_DELIMITER) {
            delimiter(data != 0);
            return;
        }
        if (delimiterSets > 1) return;  // an alternative to a usage already read
        switch (tag) {
            case LOCAL_USAGE -> usages.add(usage(data, size));
            case LOCAL_USAGE_MINIMUM -> usageMinimum = usage(data, size);
            case LOCAL_USAGE_MAXIMUM -> {
                if (usageMinimum == null) return;
                HidUsage maximum = usage(data, size);
                if (maximum.id() - usageMinimum.id() >= MAX_FIELDS_PER_ITEM) {
                    throw new IllegalArgumentException("Usage range " + usageMinimum + " to " + maximum);
                }
                for (int id = usageMinimum.id(); id <= maximum.id(); id++) usages.add(new HidUsage(usageMinimum.page(), id));
                usageMinimum = null;
            }
            default -> {
                // designators and strings: not needed to name an input
            }
        }
    }

    /** Opening a set counts it, so every set after the first is skipped; sets do not nest. */
    private void delimiter(boolean open) {
        if (open == inDelimiter) {
            throw new IllegalArgumentException(open ? "Delimiter opened inside another" : "Delimiter closed unopened");
        }
        inDelimiter = open;
        if (open) delimiterSets++;
    }

    /** A four-byte usage carries its own page in the high half; a shorter one takes the current page. */
    private HidUsage usage(long data, int size) {
        if (size == 4) return new HidUsage((int) (data >>> 16), (int) (data & 0xFFFF));
        return new HidUsage(usagePage, (int) data);
    }

    private void input(int flags) {
        if ((flags & INPUT_CONSTANT) != 0 || (flags & INPUT_VARIABLE) == 0 || usages.isEmpty()) return;
        int fields = Math.max(reportCount, usages.size());
        for (int field = 0; field < fields; field++) {
            inputs.add(usages.get(Math.min(field, usages.size() - 1)));
        }
    }
}
