package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.RewrittenXml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Moves bindings from one device name to another in binds files held in memory - every {@code Device="..."} on
 * {@code <Primary>}, {@code <Secondary>}, {@code <Binding>} and {@code <Modifier>}, in every binds file, since the game
 * checks them all (alias-designer.md, <em>An edit {@code .binds} depends on updates {@code .binds}</em>). First-time
 * startup's merge and the edits on screen both use it. Pure: nothing is read or written here.
 */
public final class BindsFiles {

    private BindsFiles() {
    }

    /**
     * The binds files with every rewrite made, one after another, and how many bindings moved. A file no rewrite
     * changes keeps its bytes.
     *
     * @param bindings every binds and preset file, by file name - the preset file is left as it is
     * @throws IOException if a binds file cannot be rewritten - the message names it
     */
    public static Rewritten rewrite(Map<String, byte[]> bindings, List<DeviceReferenceRewrite> rewrites)
            throws IOException {
        Map<String, byte[]> result = new TreeMap<>(bindings);
        if (rewrites.isEmpty()) return new Rewritten(result, 0);
        BindingsWriter writer = new BindingsWriter();
        int moved = 0;
        for (Map.Entry<String, byte[]> file : result.entrySet()) {
            if (!Backups.isBindsFile(file.getKey())) continue;
            Encoded text = Encoded.of(file.getValue());
            String xml = text.xml();
            int inFile = 0;
            try {
                for (DeviceReferenceRewrite rewrite : rewrites) {
                    RewrittenXml rewritten = writer.rewriteDeviceReferences(xml, rewrite);
                    xml = rewritten.xml();
                    inFile += rewritten.references();
                }
            } catch (IllegalArgumentException e) {
                throw new IOException(file.getKey() + " could not be rewritten: " + e.getMessage(), e);
            }
            if (inFile == 0) continue;
            file.setValue(text.with(xml));
            moved += inFile;
        }
        return new Rewritten(result, moved);
    }

    /**
     * @param bindings every binds and preset file afterwards
     * @param moved    how many bindings moved, across every binds file
     */
    public record Rewritten(Map<String, byte[]> bindings, int moved) {
    }

    /** A binds file's text, and whether it began with a UTF-8 byte-order mark, which is written back. */
    private record Encoded(String xml, boolean bom) {

        private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        static Encoded of(byte[] content) {
            boolean bom = content.length >= 3 && content[0] == BOM[0] && content[1] == BOM[1] && content[2] == BOM[2];
            int start = bom ? 3 : 0;
            return new Encoded(new String(content, start, content.length - start, StandardCharsets.UTF_8), bom);
        }

        byte[] with(String xml) {
            byte[] text = xml.getBytes(StandardCharsets.UTF_8);
            if (!bom) return text;
            byte[] content = new byte[BOM.length + text.length];
            System.arraycopy(BOM, 0, content, 0, BOM.length);
            System.arraycopy(text, 0, content, BOM.length, text.length);
            return content;
        }
    }
}
