package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.List;

/**
 * Internal class to manage fuzzy tables
 *
 * @author gitHub/fCat97
 */
class DatabaseUtil2 {
    private final Fuzzyble sourceDatabase;
    private final Fuzzyble syncDatabase;
    private final Strategy strategy;

    DatabaseUtil2(Fuzzyble sourceDatabase, Fuzzyble syncDatabase, Strategy strategy) {
        this.sourceDatabase = sourceDatabase;
        this.syncDatabase = syncDatabase;
        this.strategy = strategy;
    }

    /**
     * Create fuzzy table. This won't populate with data
     * @param column {@linkplain FuzzyColumn} to create
     * @param deletePrevious if true, delete the previous table
     */
    void createTable(FuzzyColumn column, boolean deletePrevious) throws IOException {
        if (deletePrevious) strategy.delete(syncDatabase, column);

        strategy.create(syncDatabase, column);

        // if not already exists add the entity
        createMetaTable();
        if (!isPopulated(column)) markPopulated(column, false);
    }

    /**
     * Populate with data for specified column
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param force use the previously populated table if `false`. If `true` or not populated with data
     *             populate it. This won't delete the previous table; to do so, use {@linkplain DatabaseUtil2#createTable}
     * @throws IOException thrown if error occur.
     */
    void populateTable(FuzzyColumn column, boolean force) throws IOException {
        // if data exists(populated) or not forced, return
        if (isPopulated(column) && !force) return;

        strategy.populate(sourceDatabase, syncDatabase, column);

        // when done, mark as populated
        markPopulated(column, true);
    }

    /**
     * Add data to fuzzy search for specific column
     * @param column {@linkplain FuzzyColumn} to insert in.
     * @param text {@linkplain String} to process and insert.
     */
    void addToFuzzySearch(FuzzyColumn column, String text) {
        strategy.insert(syncDatabase, column, text);
    }

    boolean isFuzzyEnabled(FuzzyColumn column) throws IOException {
        String query = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + column.getFuzzyTableName() + "'";
        SqlCursor cursor = syncDatabase.onQuery(query);
        int count = cursor.count();
        cursor.close();
        return count > 0;
    }

    /**
     * If data is already created for fuzzy search or not
     * @param column {@linkplain FuzzyColumn} to check for
     * @return `true` if already populated, false otherwise
     * @throws IOException if error occur
     */
    boolean isPopulated(FuzzyColumn column) throws IOException {
        String query = "SELECT * FROM fuzzyble_meta_data WHERE table_name = ?";
        String[] args = new String[]{column.getFuzzyTableName()};
        SqlCursor cursor = syncDatabase.onQuery(query, args);

        if (cursor.moveToNext()) {
            String s = cursor.getString(2);
            return Integer.parseInt(s) == 1;
        }

        cursor.close();
        return false;
    }

    /**
     * Mark a {@linkplain FuzzyColumn} as populated or not
     * @param column {@linkplain FuzzyColumn} to mark
     * @param isPopulated flag denoting if populated or not
     */
    void markPopulated(FuzzyColumn column, boolean isPopulated) {
        int i = 0;
        if (isPopulated) i = 1;

        String[] args = new String[]{
                column.getFuzzyTableName(),
                String.valueOf(i),
                String.valueOf(System.currentTimeMillis())
        };

        String markPopulatedSql = "INSERT OR REPLACE INTO fuzzyble_meta_data(table_name, populated, last_update) VALUES(?, ?, ?)";
        syncDatabase.onExecute(markPopulatedSql, args);
    }

    String[] getWordSuggestion(FuzzyColumn column, String word) {
        List<String> suggestion = strategy.getSuggestions(syncDatabase, column, word);
        return suggestion.toArray(new String[]{});
    }

    private void createMetaTable() {
        // create meta table if not exists
        String sql = "CREATE TABLE IF NOT EXISTS fuzzyble_meta_data(" +
                "id INTEGER PRIMARY KEY, " +
                "table_name VARCHAR(255) NOT NULL UNIQUE, " +
                "populated INTEGER NOT NULL DEFAULT 0, " +
                "last_update INTEGER" +
                ");";
        syncDatabase.onExecute(sql, null);
    }
}
