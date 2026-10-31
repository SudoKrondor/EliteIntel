package elite.intel.gameapi.journalimport;

import elite.intel.gameapi.JournalFiles.StartStamp;

import java.io.IOException;
import java.io.InputStream;

/**
 * One journal the collector found, wherever it was: a file in a folder, or an archive entry copied out to the
 * staging folder.
 *
 * @param name   the journal's own file name, which has already passed the journal name pattern
 * @param stamp  when the game opened it, read from the name - the order the import reads in
 * @param size   its length in bytes, uncompressed
 * @param origin where it came from, for the log
 */
record FoundJournal(String name, StartStamp stamp, long size, Source source, String origin) {

    @FunctionalInterface
    interface Source {
        InputStream open() throws IOException;
    }

    InputStream open() throws IOException {
        return source.open();
    }
}
