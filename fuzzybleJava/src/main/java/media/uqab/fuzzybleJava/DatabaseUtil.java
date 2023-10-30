package media.uqab.fuzzybleJava;

import java.io.IOException;

/**
 * Internal class to manage fuzzy tables
 *
 * @author gitHub/fCat97
 */
class DatabaseUtil {
    private final Fuzzyble sourceDatabase;
    private final Fuzzyble syncDatabase;

    DatabaseUtil(Fuzzyble sourceDatabase, Fuzzyble syncDatabase) {
        this.sourceDatabase = sourceDatabase;
        this.syncDatabase = syncDatabase;
    }

    /**
     * Create fuzzy table. This won't populate with data
     * @param column {@linkplain FuzzyColumn} to create
     * @param deletePrevious if true, delete the previous table
     */
    void createTable(FuzzyColumn column, boolean deletePrevious) throws IOException {
        if (deletePrevious) syncDatabase.onExecute(dropTable(column), null);

        syncDatabase.onExecute(createTableQuery(column), null);

        // if not already exists add the entity
        createMetaTable();
        if (!isPopulated(column)) markPopulated(column, false);
    }

    /**
     * Populate with data for specified column
     * @param column {@link FuzzyColumn} on which fuzzy search will be performed
     * @param force use the previously populated table if `false`. If `true` or not populated with data
     *             populate it. This won't delete the previous table; to do so, use {@linkplain DatabaseUtil#createTable}
     * @throws IOException thrown if error occur.
     */
    void populateTable(FuzzyColumn column, boolean force) throws IOException {
        // if data exists(populated) or not forced, return
        if (isPopulated(column) && !force) return;

        // get text of that columns
        SqlCursor textCursor = sourceDatabase.onQuery(getDataQuery(column));
        if (textCursor == null) return;

        // process, filter and insert words
        while (textCursor.moveToNext()) {
            String text = textCursor.getString(0);
            addToFuzzySearch(column, text);
        }

        textCursor.close();

        // when done, mark as populated
        markPopulated(column, true);
    }

    /**
     * Add data to fuzzy search for specific column
     * @param column {@linkplain FuzzyColumn} to insert in.
     * @param text {@linkplain String} to process and insert.
     */
    void addToFuzzySearch(FuzzyColumn column, String text) {
        if (column instanceof ColumnTrigrams) {
            String query = getTrigramInsertQuery(column);

            for (String word: TextHelper.splitAndFilterText(text)) {
                for (String trigram: TextHelper.splitAndGetTrigrams(word)) {
                    try {
                        String[] args = new String[]{trigram, word};
                        syncDatabase.onExecute(query, args);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            String query = getUniqueInsertQuery(column);

            // insert into (word,len) table
            for(String w: TextHelper.splitAndFilterText(text)) {
                try {
                    String l = String.valueOf(w.length());
                    String[] args = new String[]{w, l, w, w, l, w, w};
                    syncDatabase.onExecute(query, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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

    void createMetaTable() {
        // create meta table if not exists
        String sql = "CREATE TABLE IF NOT EXISTS fuzzyble_meta_data(" +
                "id INTEGER PRIMARY KEY, " +
                "table_name VARCHAR(255) NOT NULL UNIQUE, " +
                "populated INTEGER NOT NULL DEFAULT 0, " +
                "last_update INTEGER" +
                ");";
        syncDatabase.onExecute(sql, null);
    }

    private String createTableQuery(FuzzyColumn column) {
        if (column instanceof ColumnTrigrams) {
            return "CREATE TABLE IF NOT EXISTS " + column.getFuzzyTableName() + "(" +
                    "trigram VARCHAR(10) NOT NULL, " +
                    "word VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY(trigram, word)" +
                    ")";
        } else {
            return "CREATE TABLE IF NOT EXISTS " + column.getFuzzyTableName() + "(word TEXT, len INTEGER)";

        }
    }

    private String dropTable(FuzzyColumn column) {
        return "DROP TABLE IF EXISTS " + column.getFuzzyTableName();
    }

    private String getDataQuery(FuzzyColumn column) {
        return "SELECT " + column.column + " FROM " + column.table;
    }

    private String getUniqueInsertQuery(FuzzyColumn column) {
        final String fuzzyTable = column.getFuzzyTableName();

        return "INSERT INTO " + fuzzyTable + " (word, len) " +
                "SELECT ?, ? " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + fuzzyTable + " WHERE word LIKE ? || '%' OR ? LIKE word || '%') " +
                "OR ? < (SELECT min(len) FROM " + fuzzyTable + " WHERE word LIKE ? || '%' OR ? LIKE word || '%')";
    }

    private String getTrigramInsertQuery(FuzzyColumn column) {
        return "INSERT OR IGNORE INTO " + column.getFuzzyTableName() + " (trigram, word) VALUES (?, ?)";
    }
}
