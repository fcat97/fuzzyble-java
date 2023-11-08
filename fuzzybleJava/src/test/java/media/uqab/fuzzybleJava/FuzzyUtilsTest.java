package media.uqab.fuzzybleJava;

import media.uqab.fuzzybleJava.impl.ImmutableDatabase;
import media.uqab.fuzzybleJava.impl.MockDatabase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FuzzyUtilsTest {
    protected static FuzzyCursor mCursor;
    protected static FuzzyColumn mFuzzyColumn;
    protected static Connection mMutConnection, mImMutConnection;
    protected static Similarity mSimilarity;
    protected static Strategy mStrategy;

    private static ArrayList<String> demoText;

    @BeforeAll
    static void setup() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        mMutConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        mImMutConnection = DriverManager.getConnection("jdbc:sqlite::memory:");

        mSimilarity = new Levenshtein();
        mFuzzyColumn = new ColumnTrigrams("tab1", "c1");
        mStrategy = new Trigram(mSimilarity);

        demoText = new ArrayList<>();
        demoText.add("Fumarole minerals (or fumarolic minerals) are minerals which are deposited by fumarole exhalations. They form when gases and compounds desublimate or precipitate out of condensates, forming mineral deposits. They are mostly associated with volcanoes (as volcanic sublimate or fumarolic sublimate) following deposition from volcanic gas during an eruption or discharge from a volcanic vent or fumarole,[1] but have been encountered on burning coal deposits as well. They can be black or multicoloured and are often unstable upon exposure to the atmosphere.");
        demoText.add("Native sulfur is a common sublimate mineral and various halides, sulfides and sulfates occur in this environment associated with fumaroles and eruptions. A number of rare minerals are fumarole minerals, and at least 240 such minerals are known from Tolbachik volcano in Kamchatka, Russia. Other volcanoes where particular fumarole minerals have been discovered are Vulcano in Italy and Bezymyanny also in Russia.");

        Fuzzyble mMutableDb = new MockDatabase(mMutConnection);
        Fuzzyble mImMutableDb = new ImmutableDatabase(mImMutConnection);

//        mCursor = new FuzzyCursor(mMutableDb);
//        mCursor = new FuzzyCursor(mMutableDb, mStrategy);
//        mCursor = new FuzzyCursor(mImMutableDb, mMutableDb);
        mCursor = new FuzzyCursor(mImMutableDb, mMutableDb, mStrategy);
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        // Close the database connection
        if (mMutConnection != null && !mMutConnection.isClosed()) {
            mMutConnection.close();
            mMutConnection = null;
        }

        if (mImMutConnection != null && !mImMutConnection.isClosed()) {
            mImMutConnection.close();
            mImMutConnection = null;
        }

        mCursor = null;
        mFuzzyColumn = null;
    }

    @Test @Order(1)
    void shouldFailIfExecuteSqlRunOnImmutableDatabase() throws ClassNotFoundException, SQLException {
        // given
        String expectedMessage = "can't execute on immutable database";

        Class.forName("org.sqlite.JDBC");
        Connection mutCon = DriverManager.getConnection("jdbc:sqlite::memory:");
        Connection imMutCon = DriverManager.getConnection("jdbc:sqlite::memory:");

        Fuzzyble mutDb = new MockDatabase(mutCon);
        Fuzzyble imMutDb = new ImmutableDatabase(imMutCon);

        FuzzyColumn column = new ColumnTrigrams("tab1", "c1");
        FuzzyCursor cursor = new FuzzyCursor(mutDb, imMutDb);

        // when
        Executable test = () -> cursor.createFuzzyble(column, false);
        Exception exception = assertThrows(RuntimeException.class, test);
        String actualMessage = exception.getMessage();

        // then
        assertEquals(actualMessage, expectedMessage);
    }

    @Test @Order(2)
    void queryFailWhenColumnNotFuzzyble() throws IOException {
        // given
        String expectedMessage = mFuzzyColumn.column + " is not fuzzyble.";

        // when
        boolean isFuzzyble = mCursor.isFuzzyble(mFuzzyColumn);

        Executable test = () -> mCursor.getFuzzyWords(mFuzzyColumn, "someWords");
        Exception exception = assertThrows(RuntimeException.class, test);
        String actualMessage = exception.getMessage();

        // then
        assertFalse(isFuzzyble);
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test @Order(3)
    void shouldPassWhenColumIsMadeFuzzyble() throws IOException {
        // given
        // not fuzzyble column

        // when
        boolean before = mCursor.isFuzzyble(mFuzzyColumn);
        mCursor.createFuzzyble(mFuzzyColumn, false);
        boolean isFuzzyble = mCursor.isFuzzyble(mFuzzyColumn);

        // then
        assertFalse(before);
        assertTrue(isFuzzyble);
    }

    @Test @Order(4)
    void shouldFailsWhenColumnIsNotPopulated() throws IOException {
        // given
        // column is not populated yet

        // when
        boolean populated = mCursor.isPopulated(mFuzzyColumn);

        // then
        assertFalse(populated);
    }

    @Test @Order(5)
    void shouldPassWhenColumnIsPopulated() throws IOException {
        // given

        // when
        boolean before = mCursor.isPopulated(mFuzzyColumn);
        assertFalse(before);

        for (String t: demoText) mCursor.addToFuzzySearch(mFuzzyColumn, t);
        mCursor.markPopulated(mFuzzyColumn, true);

        // then
        boolean after = mCursor.isPopulated(mFuzzyColumn);
        assertTrue(after);
    }

    @Test @Order(6)
    void getWordSuggestionsWhenExactMatchFound() throws IOException {
        // given
        String word = "sulfides";

        // when
        String[] result = mCursor.getFuzzyWords(mFuzzyColumn, word);

        // then
        // check if all the suggestion are present in the word
        assertTrue(result.length > 0);
    }

    @Test @Order(7)
    void getWordSuggestionsWhenPartialMatchFound() throws IOException {
        // given
        String word = "Fumar";

        // when
        String[] result = mCursor.getFuzzyWords(mFuzzyColumn, word);

        // then
        assertTrue(result.length > 0);
    }

    @Test @Order(8)
    void getWordSuggestionsWhenFuzzyMatchFound() throws IOException {
        // given
        String original = "Fumarole";
        String word = "Fumarale";

        // when
        String[] result = mCursor.getFuzzyWords(mFuzzyColumn, word);

        // then
        boolean isSimilar = mSimilarity.isSimilar(original, word);
        int similarity = mSimilarity.similarityIndex(original, word);
        assertTrue(isSimilar);
        assertTrue(result.length > 0);
    }

    @Test @Order(9)
    void shouldPopulateMutableDatabase() throws SQLException, IOException {
        // given
        Connection cn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Fuzzyble db = new MockDatabase(cn);
        FuzzyColumn column = new ColumnTrigrams("tableA", "col1");

        String createTableSql = "CREATE TABLE " + column.table +
                "(id int primary key," +
                column.column + " TEXT)";
        db.onExecute(createTableSql, null);

        for (String s: demoText) {
            String insertSql = "INSERT INTO " + column.table + "(" + column.column + ") VALUES(?)";
            db.onExecute(insertSql, new String[]{s});
        }

        FuzzyCursor cursor = new FuzzyCursor(db);
        cursor.createFuzzyble(column, false);
        cursor.populate(column, false);

        // when
        String exactWord = "Fumarole";
        String partialWord = "Fumar";
        String fuzzyWord = "marole";

        boolean isFuzzyble = cursor.isFuzzyble(column);
        boolean isPopulated = cursor.isPopulated(column);
        String[] exactMatch = cursor.getFuzzyWords(column, exactWord);
        String[] partialMatch = cursor.getFuzzyWords(column, partialWord);
        String[] fuzzyMatch = cursor.getFuzzyWords(column, fuzzyWord);

        // then
        assertTrue(isFuzzyble);
        assertTrue(isPopulated);
        assertTrue(exactMatch.length > 0);
        assertTrue(partialMatch.length > 0);
        assertTrue(fuzzyMatch.length > 0);
    }

    @Test @Order(10)
    void shouldForcePopulateMutableDatabase() throws SQLException, IOException {
        // given
        Connection cn = DriverManager.getConnection("jdbc:sqlite::memory:");
        Fuzzyble db = new MockDatabase(cn);
        FuzzyColumn column = new ColumnTrigrams("tableA", "col1");

        String createTableSql = "CREATE TABLE " + column.table +
                "(id int primary key," +
                column.column + " TEXT)";
        db.onExecute(createTableSql, null);

        for (String s: demoText) {
            String insertSql = "INSERT INTO " + column.table + "(" + column.column + ") VALUES(?)";
            db.onExecute(insertSql, new String[]{s});
        }

        FuzzyCursor cursor = new FuzzyCursor(db);

        // when
        cursor.createFuzzyble(column, false);
        boolean isFuzzyble = cursor.isFuzzyble(column);

        cursor.createFuzzyble(column, true);
        boolean isReFuzzyble = cursor.isFuzzyble(column);

        cursor.populate(column, false);
        boolean isPopulated = cursor.isPopulated(column);

        cursor.markPopulated(column, false);
        boolean notPopulated = cursor.isPopulated(column);

        cursor.populate(column, true);
        boolean repopulated = cursor.isPopulated(column);

        String exactWord = "Fumarole";
        String partialWord = "Fumar";
        String fuzzyWord = "marole";

        String[] exactMatch = cursor.getFuzzyWords(column, exactWord);
        String[] partialMatch = cursor.getFuzzyWords(column, partialWord);
        String[] fuzzyMatch = cursor.getFuzzyWords(column, fuzzyWord);

        // then
        assertTrue(isFuzzyble);
        assertTrue(isReFuzzyble);
        assertTrue(isPopulated);
        assertFalse(notPopulated);
        assertTrue(repopulated);

        assertTrue(exactMatch.length > 0);
        assertTrue(partialMatch.length > 0);
        assertTrue(fuzzyMatch.length > 0);
    }
}
