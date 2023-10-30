package media.uqab.fuzzybleJava;

import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;

public class FuzzyUtilWithWordLenTest extends FuzzyUtilsTest {
    @BeforeAll
    static void setup() throws SQLException, ClassNotFoundException {
        FuzzyUtilsTest.setup();
        mFuzzyColumn = new ColumnWordLen("tab1", "c1");
    }
}
