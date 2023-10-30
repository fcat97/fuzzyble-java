package media.uqab.fuzzybleJava;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevenshteinTest {
    private Levenshtein levenshtein;
    private Levenshtein defaultLev;

    @BeforeEach
    void setup() {
        defaultLev = new Levenshtein();

        int threshold = 2;
        levenshtein = new Levenshtein(threshold);
    }

    @Test
    void isSimilar_success_when_default_value_used() {
        // given

        // when
        boolean result = defaultLev.isSimilar("banana", "anna");

        // then
        assertTrue(result);
    }

    @Test
    void isSimilar_success_when_similar_input() {
        // given

        // when
        boolean result = levenshtein.isSimilar("banana", "anna");

        // then
        assertTrue(result);
    }

    @Test
    void isSimilar_fails_when_different_input() {
        // given

        // when
        boolean result = levenshtein.isSimilar("banana", "bamboo");

        // then
        assertFalse(result);
    }

    @Test
    void similarityIndex() {
        // given

        // when
        int result = levenshtein.similarityIndex("banana", "anna");

        // then
        assertEquals(-2, result);
    }
}