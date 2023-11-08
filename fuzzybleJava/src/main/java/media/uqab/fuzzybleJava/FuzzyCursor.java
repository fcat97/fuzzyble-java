package media.uqab.fuzzybleJava;

import java.io.IOException;

/**
 * Class to get manipulate fuzzy tables.
 *
 * <p>
 * <b>Usage</b>
 * <hr>
 * To make a column fuzzy searchable (fuzzyble),
 * <ul>
 * <li>call {@link FuzzyCursor#createFuzzyble} to create a fuzzy searchable column.</li>
 * <li>call {@linkplain FuzzyCursor#populate} to insert data automatically from source database
 * to fuzzy database </li>
 * <li>call {@linkplain FuzzyCursor#addToFuzzySearch} to manually add data into fuzzy search</li>
 * </ul>
 *
 * <br>
 * <b>Remember to call on background thread or it may cause ANR.</b>
 *
 * <p></p>
 * <b>Terminology</b>
 * <hr>
 *
 * <b>Immutable Database</b>: The database which doesn't allow to modify externally
 * or require special mechanism to do so. For example, in android Room database
 * do not support runtime creation of tables.
 * <br>
 *
 * <b>Mutable Database</b>:
 * Supporting database if the source database is immutable.
 * <br>
 * i.e. modification on source database is not permissible. So this database
 * will be used instead to create necessary tables and hold required data.
 *
 * @author github/fCat97
 */
public class FuzzyCursor {
    private final DatabaseUtil2 databaseUtil;


    /**
     * Constructor for {@link FuzzyCursor}.
     * @param database Database that inherited {@link Fuzzyble} interface and also mutable for creating tables.
     */
    public FuzzyCursor(Fuzzyble database) {
        this(database, new Trigram());
    }

    /**
     * Constructor for {@linkplain FuzzyCursor}
     * @param database a mutable database where creating tables and inserting data is permitted.
     * @param strategy {@link Strategy} method to perform fuzzy operations.
     * Two default implementations {@linkplain Trigram} &amp; {@linkplain WordLen} provided.
     */
    public FuzzyCursor(Fuzzyble database, Strategy strategy) {
        this(database, database, strategy);
    }

    /**
     * Constructor for {@link FuzzyCursor}.
     *
     * <p>
     * This constructor is used in a special case.
     * If modifying the main database is not permissible,
     * this constructor can be used to create the fuzzy words list
     * into a separate database.
     *
     * @param immutableDatabase {@link Fuzzyble} database but modification is not allowed
     * @param mutableDatabase {@link Fuzzyble} database to store fuzzy words
     *
     */
    public FuzzyCursor(Fuzzyble immutableDatabase, Fuzzyble mutableDatabase) {
        this(immutableDatabase, mutableDatabase, new Trigram());
    }

    /**
     * Constructor for {@link FuzzyCursor}.
     *
     * <p>
     * This constructor is used in a special case.
     * If modifying the main database is not permissible,
     * this constructor can be used to create the fuzzy words list
     * into a separate database.
     *
     * @param immutableDatabase {@link Fuzzyble} database but modification is not allowed
     * @param mutableDatabase {@link Fuzzyble} database to store fuzzy words.
     * @param strategy {@link Strategy} method to perform fuzzy operations.
     * Two default implementations {@linkplain Trigram} &amp; {@linkplain WordLen} provided.
     */
    public FuzzyCursor(Fuzzyble immutableDatabase, Fuzzyble mutableDatabase, Strategy strategy) {
        this.databaseUtil = new DatabaseUtil2(immutableDatabase, mutableDatabase, strategy);
    }

    /**
     * Check if a column has fuzzy search enabled
     * @param column {@link FuzzyColumn}
     * @return `true` if enabled
     */
    public boolean isFuzzyble(FuzzyColumn column) throws IOException {
        return databaseUtil.isFuzzyEnabled(column);
    }

    /**
     * If a column is populated or not
     * @param column {@linkplain FuzzyColumn}
     * @return true if populated, false otherwise
     * @throws IOException if error occur
     */
    public boolean isPopulated(FuzzyColumn column) throws IOException {
        return databaseUtil.isPopulated(column);
    }

    /**
     * Mark a column as populated
     * @param column {@linkplain FuzzyColumn}
     * @param isPopulated flag to mark.
     * @throws IOException if any io error occur
     * @throws RuntimeException if the {@linkplain FuzzyColumn} is not fuzzyble.
     */
    public void markPopulated(FuzzyColumn column, boolean isPopulated) throws IOException, RuntimeException {
        throwIfNotFuzzyble(column);
        databaseUtil.markPopulated(column, isPopulated);
    }

    /**
     * Initiate fuzzy search on a {@link FuzzyColumn}.
     *
     * <p>
     * This will create a table in mutable database but <b><u>fuzzy is not enabled yet.</u></b>
     * To enable fuzzy on a column, either use {@linkplain FuzzyCursor#populate}
     * for entire column, or use {@linkplain FuzzyCursor#addToFuzzySearch} to add
     * data manually.
     *
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param deletePrevious delete previous data for this column.
     */
    public void createFuzzyble(FuzzyColumn column, boolean deletePrevious) throws IOException {
        databaseUtil.createTable(column, deletePrevious);
    }

    /**
     * Populate with fuzzy search data for {@link FuzzyColumn}.
     *
     * <p>
     * This will populate data from immutable (source) database into mutable (sink) one.
     *
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param force if `true`, ignore previous data and redo the process.
     * @throws IOException if any error occur
     * @throws RuntimeException If the column is not enabled for fuzzy search.
     * To enable use {@linkplain FuzzyCursor#createFuzzyble}.
     */
    public void populate(FuzzyColumn column, boolean force) throws IOException, RuntimeException {
        throwIfNotFuzzyble(column);
        databaseUtil.populateTable(column, force);
    }

    /**
     * Manually add text for fuzzy search
     * @param column {@linkplain FuzzyColumn} to which the data will be added.
     * @param text {@linkplain String} text to add.
     * @throws IOException if any error occur
     * @throws RuntimeException if the column is not fuzzyble
     */
    public void addToFuzzySearch(FuzzyColumn column, String text) throws IOException, RuntimeException {
        throwIfNotFuzzyble(column);
        databaseUtil.addToFuzzySearch(column, text);
    }

    /**
     * Get fuzzy searched word suggestion
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param word searched word
     * @throws IOException if error occur when querying.
     * @throws RuntimeException if the column has not fuzzy search enabled.
     * @return Fuzzy suggestions.
     */
    public String[] getFuzzyWords(FuzzyColumn column, String word) throws IOException, RuntimeException {
        throwIfNotFuzzyble(column);
        return databaseUtil.getWordSuggestion(column, word);
    }

    /**
     * Check if fuzzyble or not, if not throw an exception
     * @param column {@link FuzzyColumn} column to check
     * @throws IOException if any io exception.
     * @throws RuntimeException if column is not fuzzyble
     */
    private void throwIfNotFuzzyble(FuzzyColumn column) throws IOException, RuntimeException {
        if (! isFuzzyble(column)) throw new RuntimeException(column.column + " is not fuzzyble. use `createFuzzyble()` to enable.");
    }
}
