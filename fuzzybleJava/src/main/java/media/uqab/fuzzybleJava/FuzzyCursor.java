package media.uqab.fuzzybleJava;

import java.io.IOException;

/**
 * Class to get manipulate fuzzy tables.
 *
 * <p>
 * To make a column fuzzy searchable (fuzzyble),
 * <ul>
 * <li>call {@link FuzzyCursor#createFuzzyble} to create a fuzzy searchable column.</li>
 * <li>call {@linkplain FuzzyCursor#populate} to insert data automatically from source database
 * to fuzzy database </li>
 * <li>call {@linkplain FuzzyCursor#addToFuzzySearch} to manually add data into fuzzy search</li>
 *</ul>
 * <p>
 * <b>Remember to call on background thread or it may cause ANR.</b>
 *
 * @author github/fCat97
 */
public class FuzzyCursor {
    /**
     * Main database on which actual search will be performed.
     */
    private final Fuzzyble sourceDatabase;

    /**
     * Support database if the {@linkplain FuzzyCursor#sourceDatabase} is immutable.
     *
     * <p>
     * i.e. modification on source database is not permissible. So this database
     * is used to create fuzzy words tables and populated with necessary data.
     * Fuzzy suggestion is also provided from this database.
     *
     * <p>
     * But if the {@linkplain FuzzyCursor#sourceDatabase} is mutable, this is the
     * same object i.e. the {@linkplain FuzzyCursor#sourceDatabase}.
     */
    private final Fuzzyble syncDatabase;

    private final DatabaseUtil databaseUtil;

    private final FuzzyUtils fuzzyUtils;


    /**
     * Constructor for {@link FuzzyCursor}.
     * @param database Database that inherited {@link Fuzzyble} interface.
     */
    public FuzzyCursor(Fuzzyble database) {
        this(database, new Levenshtein());
    }

    /**
     * Constructor for {@linkplain FuzzyCursor}
     * @param database a mutable database to store fuzzy data
     * @param similarity custom {@linkplain Similarity} checker.
     */
    public FuzzyCursor(Fuzzyble database, Similarity similarity) {
        this(database, database, similarity);
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
        this(immutableDatabase, mutableDatabase, new Levenshtein());
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
     * @param similarity {@link Similarity} method to check similarity
     */
    public FuzzyCursor(Fuzzyble immutableDatabase, Fuzzyble mutableDatabase, Similarity similarity) {
        this.sourceDatabase = immutableDatabase;
        this.syncDatabase = mutableDatabase;
        this.databaseUtil = new DatabaseUtil(sourceDatabase, syncDatabase);
        this.fuzzyUtils = new FuzzyUtils(similarity);
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
     * @param isPopulated flag to mark
     */
    public void markPopulated(FuzzyColumn column, boolean isPopulated) {
        databaseUtil.markPopulated(column, isPopulated);
    }

    /**
     * Initiate fuzzy search on a {@link FuzzyColumn}.
     *
     * <p>
     * This will create a table in mutable database.
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
     * This will populate data from immutable database into mutable one.
     *
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param force if `true`, ignore previous data and redo the process.
     * @throws IOException if any error occur
     * @throws RuntimeException If the column is not enabled for fuzzy search.
     * To enable use {@linkplain FuzzyCursor#createFuzzyble}.
     */
    public void populate(FuzzyColumn column, boolean force) throws IOException, RuntimeException {
        if (!isFuzzyble(column)) {
            throw new RuntimeException(column.column + " is not fuzzyble. use `createFuzzyble()` to enable.");
        }
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
        if (!isFuzzyble(column)) throw new RuntimeException(column.column + " is not fuzzyble");
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
        if (isFuzzyble(column)) {
            return fuzzyUtils.getWordSuggestions(syncDatabase, column, word);
        } else {
            throw new RuntimeException("fuzzy search not enabled on column " + column.column);
        }
    }
}
