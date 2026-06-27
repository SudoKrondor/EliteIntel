package elite.intel.companion.prompt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit test for {@link CompanionWordMatch}: Russian word forms taken from the action training phrases
 * must count as the same word across declension endings, while genuinely different words must not.
 */
class CompanionWordMatchTest {

    /** Same word, different Russian ending - must match (these break exact word-overlap). */
    @ParameterizedTest(name = "{0} ~ {1}")
    @CsvSource({
            "навигация,навигации",
            "навигация,навигацию",
            "ведомый,ведомого",
            "двигатели,двигатель",
            "двигатели,двигателя",
            "контакты,контакт",
            "контакты,контактов",
            "объявления,объявлений",
            "инвентарь,инвентаря",
            "авианосцем,авианосец",
            "управление,управления",
            "выпусти,выпустить",
            "распределитель,распределителя",
            "жизнеобеспечение,жизнеобеспечения",
            "грузозаборник,грузозаборника",
    })
    void matchesInflectedForms(String a, String b) {
        assertTrue(CompanionWordMatch.similar(a, b), a + " should match " + b);
        assertTrue(CompanionWordMatch.similar(b, a), b + " should match " + a + " (symmetry)");
    }

    /** Different words - must NOT match, even when they share some letters. */
    @ParameterizedTest(name = "{0} !~ {1}")
    @CsvSource({
            "стоп,стол",
            "цель,щель",
            "торт,порт",
            "двигатели,движение",
            "ракета,работа",
            "навигация,навигатор",
            "контакты,контейнер",
    })
    void rejectsDifferentWords(String a, String b) {
        assertFalse(CompanionWordMatch.similar(a, b), a + " should NOT match " + b);
    }

    @Test
    void shortWordsRequireExactMatch() {
        assertTrue(CompanionWordMatch.similar("стоп", "стоп"));
        assertFalse(CompanionWordMatch.similar("бой", "бои")); // 3 letters: only exact
    }
}
