package elite.intel.gameapi.data;

import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent.BioData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks {@link BioForms}' payout table against what Vista Genomics actually paid.
 *
 * <p>WHY this exists at all: exobiology payouts are fixed game constants, but the journal never states
 * one until the data is sold, so every figure spoken between sampling and selling comes out of a table
 * curated by hand. A hand-curated table drifts silently - the 2026-08-30 audit found 15 wrong rows in
 * 117, three of them the constant 16,777,215 ({@code 0xFFFFFF}, a 24-bit clamp inherited from whatever
 * produced the original data). Nothing in the app would ever have noticed. A sale is the one moment
 * the truth arrives, and it was being used for the credit total and then discarded.
 *
 * <p>This is a read-only observer: it writes nothing, corrects nothing, and never speaks. A mismatch is
 * a defect report for a future maintainer, delivered through the log a commander already sends with a
 * diagnostics bundle. Correcting the table from a live sale would be worse than useless - it would make
 * every install disagree with every other, and the figure it "learned" would be gone at the next
 * reinstall.
 *
 * <p>Cost is a handful of lines per SALE, not per sample - a sale happens a few times a session at most.
 * The logger for this class is deliberately louder than {@code elite.intel} in {@code log4j2.xml};
 * without that entry every line here would be discarded and the audit would do nothing at all.
 */
public final class BioFormsValueAudit {

    private static final Logger log = LogManager.getLogger(BioFormsValueAudit.class);

    /**
     * Vista Genomics pays 5x in total for a first log, so the bonus is 4x the base. Measured, not
     * assumed: 182 bonus-bearing rows across two months of journals gave a ratio of exactly 4.000.
     */
    private static final long FIRST_LOGGED_MULTIPLIER = 4;

    private BioFormsValueAudit() {
    }

    /**
     * What one sold row says about our table.
     */
    enum Verdict {
        /**
         * The table's figure equals what was paid.
         */
        MATCH,
        /**
         * The table holds this species and is wrong about it.
         */
        WRONG_VALUE,
        /**
         * The organism is not in the table - new content, or a stem we never captured.
         */
        UNKNOWN_SPECIES,
        /**
         * A first-logged bonus that is not 4x the row's own value, which would break the whole model.
         */
        UNEXPECTED_BONUS
    }

    record Finding(Verdict verdict, String symbol, String name, long paidValue, long paidBonus, long tableValue) {
        long delta() {
            return paidValue - tableValue;
        }
    }

    /**
     * Compares every row of a sale against the table and logs whatever disagrees.
     */
    public static void audit(SellOrganicDataEvent event) {
        List<Finding> findings = findings(event == null ? null : event.getBioData());
        if (findings.isEmpty()) return;

        long matched = findings.stream().filter(f -> f.verdict() == Verdict.MATCH).count();
        log.info("BioForms audit: {} sold rows, {} matched the table, {} did not",
                findings.size(), matched, findings.size() - matched);

        for (Finding f : findings) {
            switch (f.verdict()) {
                case MATCH -> { /* the expected case; the summary line above already counted it */ }
                case WRONG_VALUE -> log.warn(
                        "BioForms value WRONG for {} ({}): Vista paid {}, table says {}, delta {}. "
                                + "Correct the row to {} with bonus {}.",
                        f.symbol(), f.name(), f.paidValue(), f.tableValue(), f.delta(),
                        f.paidValue(), f.paidValue() * FIRST_LOGGED_MULTIPLIER);
                case UNKNOWN_SPECIES -> log.warn(
                        "BioForms has NO ROW for {} ({}): Vista paid {}. Add it with bonus {}.",
                        f.symbol(), f.name(), f.paidValue(), f.paidValue() * FIRST_LOGGED_MULTIPLIER);
                case UNEXPECTED_BONUS -> log.warn(
                        "BioForms bonus model BROKEN for {} ({}): paid value {} with bonus {}, "
                                + "which is not {}x. The whole table assumes that multiplier.",
                        f.symbol(), f.name(), f.paidValue(), f.paidBonus(), FIRST_LOGGED_MULTIPLIER);
            }
        }
    }

    /**
     * The verdict on each sold row, in sale order. Pure, so the comparison can be tested against real
     * journal rows without a bus, a database or a log.
     *
     * <p>A row is judged on its value first and its bonus only as a fallback: a wrong table value is the
     * common defect and the useful message, while a broken multiplier would invalidate every row at once
     * and is worth saying on its own.
     */
    static List<Finding> findings(List<BioData> rows) {
        List<Finding> findings = new ArrayList<>();
        if (rows == null) return findings;
        for (BioData row : rows) {
            if (row == null || row.getValue() <= 0) continue; // nothing sold, nothing to check
            String symbol = BioForms.normalizeSpecies(row.getSpecies());
            String name = row.getSpeciesLocalised() != null ? row.getSpeciesLocalised() : symbol;
            BioForms.BioDetails details = BioForms.getDetails(row.getSpecies());
            if (details == null) {
                findings.add(new Finding(Verdict.UNKNOWN_SPECIES, symbol, name, row.getValue(), row.getBonus(), 0));
            } else if (details.creditValue() != row.getValue()) {
                findings.add(new Finding(Verdict.WRONG_VALUE, symbol, name, row.getValue(), row.getBonus(), details.creditValue()));
            } else if (row.getBonus() > 0 && row.getBonus() != row.getValue() * FIRST_LOGGED_MULTIPLIER) {
                findings.add(new Finding(Verdict.UNEXPECTED_BONUS, symbol, name, row.getValue(), row.getBonus(), details.creditValue()));
            } else {
                findings.add(new Finding(Verdict.MATCH, symbol, name, row.getValue(), row.getBonus(), details.creditValue()));
            }
        }
        return findings;
    }
}
