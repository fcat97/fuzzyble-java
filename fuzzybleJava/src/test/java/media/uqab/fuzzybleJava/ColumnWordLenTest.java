package media.uqab.fuzzybleJava;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnWordLenTest {

    @Test
    void getFuzzyTableName_rightInput_pass() {
        // given
        FuzzyColumn columnWordLen = new ColumnWordLen("tableA", "column1");

        // when
        String result = columnWordLen.getFuzzyTableName();

        // then
        assertEquals("fuzzyble_wordlen_tableA_column1", result);
    }
}