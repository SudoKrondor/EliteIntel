package elite.intel.gameapi;

import java.nio.file.Path;

/**
 * How far into which journal the live parser has read: the byte after the last complete line it handed on.
 * <p>
 * WHY it outlives the parser: a service restart (a settings change, a new LLM) builds a new
 * {@link JournalParser}, and one that started from byte 0 re-published every event written since the app
 * started, because the replay filter only knows the app's start time. Subscribers handled each of those events a
 * second time. Resuming from this mark hands on exactly the lines the stopped parser had not, including any
 * written while the services were down.
 *
 * @param file     the journal that was being read
 * @param position the byte offset to continue from
 */
record JournalReadMark(Path file, long position) {

    /**
     * Where to start reading {@code file}: from the mark when it is the same journal, from the top otherwise -
     * a journal the previous parser never read has nothing in it that was already handed on.
     */
    static long startPosition(JournalReadMark mark, Path file) {
        return mark != null && mark.file().equals(file) ? mark.position() : 0;
    }
}
