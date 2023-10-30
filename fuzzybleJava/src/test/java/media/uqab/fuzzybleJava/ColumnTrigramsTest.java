package media.uqab.fuzzybleJava;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnTrigramsTest {

    @Test
    void getFuzzyTableName_rightInput_pass() {
        // given
        FuzzyColumn columnTrigrams = new ColumnTrigrams("tableA", "column1");

        // when
        String result = columnTrigrams.getFuzzyTableName();

        // then
        assertEquals("fuzzyble_trigram_tableA_column1", result);
    }
}