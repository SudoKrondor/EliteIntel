package elite.intel.db.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the statement classification that decides when a "no such column" error may be tolerated.
 * <p>
 * This matters because the tolerance is a deliberate piece of graceful degradation with a narrow
 * remit: an {@code ALTER TABLE ... DROP COLUMN} whose column is already gone on a drifted tester
 * database has had its intent fulfilled. Applying the same tolerance to any other statement would
 * turn a genuine defect, such as a misspelled column in an UPDATE, into a warning while still
 * recording the migration as applied.
 */
class DatabaseMigratorTest {

    @Test
    void plainDropColumnIsRecognised() {
        assertTrue(DatabaseMigrator.isDropColumn("ALTER TABLE game_session DROP COLUMN aiPersonality"));
    }

    @Test
    void dropColumnSplitAcrossLinesIsRecognised() {
        // Migrations in this project indent the DROP onto its own line.
        assertTrue(DatabaseMigrator.isDropColumn("ALTER TABLE player\n    DROP COLUMN useVm"));
    }

    @Test
    void dropColumnPrecededByCommentsIsRecognised() {
        // Statements are split on ';' and only whitespace-trimmed, so each one carries the comment
        // block written above it. Half the drops in 01018 look like this.
        String statement = """
                -- Superseded by per-ship personality (ship.personality).
                ALTER TABLE game_session
                    DROP COLUMN aiPersonality""";
        assertTrue(DatabaseMigrator.isDropColumn(statement));
    }

    @Test
    void dropColumnAfterACommentBlockSeparatedByABlankLineIsRecognised() {
        // A section banner sits above its statement with an empty line between, so the run of
        // non-SQL lines to skip is comments and blanks mixed, not just contiguous comments.
        String statement = """
                -- === ship ==========================================================
                
                -- Speech cadence was folded into the ship personality.
                ALTER TABLE ship DROP COLUMN cadence""";
        assertTrue(DatabaseMigrator.isDropColumn(statement));
    }

    @Test
    void dropColumnWithoutTheOptionalColumnKeywordIsRecognised() {
        assertTrue(DatabaseMigrator.isDropColumn("ALTER TABLE ship DROP cadence"));
    }

    @Test
    void addColumnIsNotADrop() {
        assertFalse(DatabaseMigrator.isDropColumn("ALTER TABLE commodities ADD COLUMN commodity_pt TEXT"));
    }

    @Test
    void updateIsNotADrop() {
        assertFalse(DatabaseMigrator.isDropColumn(
                "UPDATE commodities SET commodity_pt = 'Ouro' WHERE LOWER(symbol) = LOWER('Gold')"));
    }

    @Test
    void dropTableIsNotADropColumn() {
        // DROP TABLE cannot raise "no such column", and IF EXISTS already makes it idempotent.
        assertFalse(DatabaseMigrator.isDropColumn("DROP TABLE IF EXISTS materials"));
    }

    @Test
    void aCommentMentioningADropDoesNotExcuseTheStatementBelowIt() {
        // The guard must key off the statement, not off prose that happens to describe a drop.
        String statement = """
                -- ALTER TABLE commodities DROP COLUMN commodity_uk was considered here.
                UPDATE commodities
                SET commodity_uk = 'Золото'
                WHERE LOWER(symbol) = LOWER('Gold')""";
        assertFalse(DatabaseMigrator.isDropColumn(statement));
    }
}
