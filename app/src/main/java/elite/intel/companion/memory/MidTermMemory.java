package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bounded retained history plus records waiting for transactional consolidation. */
final class MidTermMemory {

    private final Map<MemoryKind, List<MemoryRecord>> retainedByKind = new EnumMap<>(MemoryKind.class);
    private final Map<MemoryKind, List<MemoryRecord>> pendingByKind = new EnumMap<>(MemoryKind.class);

    /** Adds a record whose kind has retained-history storage, collapsing whole-record duplicates. */
    void add(MemoryRecord record) {
        if (!record.kind().movesToMidTerm()) {
            throw new IllegalArgumentException("No mid-term storage for " + record.kind());
        }
        List<MemoryRecord> pending = pendingByKind.getOrDefault(record.kind(), List.of());
        if (pending.stream().anyMatch(existing -> isDuplicate(existing, record))) {
            // Pending records belong to an already published consolidation transaction and must stay unchanged.
            return;
        }

        List<MemoryRecord> retained = retainedByKind.computeIfAbsent(record.kind(), ignored -> new ArrayList<>());
        MemoryRecord survivor = record;
        for (int i = retained.size() - 1; i >= 0; i--) {
            MemoryRecord existing = retained.get(i);
            if (isDuplicate(existing, record)) {
                retained.remove(i);
                if (existing.timestamp().isAfter(survivor.timestamp())) {
                    survivor = existing;
                }
            }
        }
        insertChronologically(retained, survivor);
    }

    /** Returns retained and pending records of one kind, oldest-to-newest. */
    List<MemoryRecord> records(MemoryKind kind) {
        List<MemoryRecord> records = new ArrayList<>(pendingByKind.getOrDefault(kind, List.of()));
        records.addAll(retainedByKind.getOrDefault(kind, List.of()));
        return List.copyOf(records);
    }

    /** Returns all retained and pending records in deterministic kind order. */
    List<MemoryRecord> allRecords() {
        List<MemoryRecord> all = new ArrayList<>();
        for (MemoryKind kind : MemoryKind.values()) {
            all.addAll(records(kind));
        }
        return List.copyOf(all);
    }

    /** Returns retained records that have not reached consolidation yet. */
    Map<MemoryKind, List<MemoryRecord>> retainedSnapshot() {
        return snapshot(retainedByKind);
    }

    /** Returns records that remain searchable until their summary is committed. */
    Map<MemoryKind, List<MemoryRecord>> pendingSnapshot() {
        return snapshot(pendingByKind);
    }

    private static Map<MemoryKind, List<MemoryRecord>> snapshot(
            Map<MemoryKind, List<MemoryRecord>> source
    ) {
        Map<MemoryKind, List<MemoryRecord>> copy = new EnumMap<>(MemoryKind.class);
        source.forEach((kind, records) -> copy.put(kind, List.copyOf(records)));
        return Map.copyOf(copy);
    }

    /** Moves oldest overflow into the pending area without making it disappear from recall. */
    List<MemoryRecord> stageOverflow() {
        List<MemoryRecord> staged = new ArrayList<>();
        for (MemoryKind kind : List.of(MemoryKind.DIALOGUE, MemoryKind.EVENT)) {
            List<MemoryRecord> records = retainedByKind.get(kind);
            if (records == null) {
                continue;
            }
            int limit = CompanionMemoryPolicy.midTermRecordLimit(kind);
            while (records.size() > limit) {
                MemoryRecord record = records.remove(0);
                insertChronologically(
                        pendingByKind.computeIfAbsent(kind, ignored -> new ArrayList<>()), record);
                staged.add(record);
            }
        }
        return List.copyOf(staged);
    }

    /** Removes exactly the records covered by a successfully committed summary. */
    void acknowledge(MemoryKind kind, List<MemoryRecord> batch) {
        requirePending(kind, batch);
        List<MemoryRecord> pending = pendingByKind.get(kind);
        List<MemoryRecord> remaining = new ArrayList<>(pending);
        for (MemoryRecord record : batch) {
            remaining.remove(record);
        }
        if (remaining.isEmpty()) {
            pendingByKind.remove(kind);
        } else {
            pendingByKind.put(kind, remaining);
        }
    }

    /** Validates a consolidation batch before its summary and acknowledgement are committed together. */
    void requirePending(MemoryKind kind, List<MemoryRecord> batch) {
        if (batch.isEmpty()) {
            throw new IllegalArgumentException("A consolidation batch must not be empty");
        }
        List<MemoryRecord> remaining = new ArrayList<>(pendingByKind.getOrDefault(kind, List.of()));
        for (MemoryRecord record : batch) {
            if (!remaining.remove(record)) {
                throw new IllegalStateException("Consolidation batch contains a non-pending " + kind + " record");
            }
        }
    }

    private static void insertChronologically(List<MemoryRecord> records, MemoryRecord record) {
        int index = records.size();
        while (index > 0 && records.get(index - 1).timestamp().isAfter(record.timestamp())) {
            index--;
        }
        records.add(index, record);
    }

    private static boolean isDuplicate(MemoryRecord first, MemoryRecord second) {
        if (first.kind() != second.kind()) {
            return false;
        }
        if (first.kind() == MemoryKind.EVENT) {
            return first.eventFact().equals(second.eventFact());
        }
        if (first.kind() != MemoryKind.DIALOGUE) {
            return false;
        }
        double floor = CompanionMemoryPolicy.semanticDedupFloor();
        for (int i = 0; i < first.entries().size(); i++) {
            if (!MemorySearch.sameMeaning(first.entries().get(i), second.entries().get(i), floor)) {
                return false;
            }
        }
        return true;
    }
}
