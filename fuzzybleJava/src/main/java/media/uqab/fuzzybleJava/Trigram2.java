package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trigram2 implements Strategy {
    private final Similarity similarity;

    public Trigram2() {
        this.similarity = new Levenshtein();
    }

    public Trigram2(Similarity similarity) {
        this.similarity = similarity;
    }

    @Override
    public String getStrategyName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean create(Fuzzyble database, FuzzyColumn column) {
        String[] tables = getAssociatedTables(column);
        String trigramTable = tables[0];
        String wordsTable = tables[1];
        String relationTable = tables[2];

        String createTrigram = "CREATE TABLE IF NOT EXISTS " + trigramTable + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "trigram VARCHAR(10) NOT NULL UNIQUE" +
                ")";
        String createWord = "CREATE TABLE IF NOT EXISTS " + wordsTable + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "word VARCHAR(255) NOT NULL UNIQUE" +
                ")";
        String createRelation = "CREATE TABLE IF NOT EXISTS " + relationTable + "(" +
                "tId INTEGER, " +
                "wId INTEGER, " +
                "FOREIGN KEY (tId) REFERENCES " + trigramTable + "(id) ON DELETE CASCADE," +
                "FOREIGN KEY (wId) REFERENCES " + wordsTable + "(id) ON DELETE CASCADE" +
                ")";
        database.onExecute(createTrigram, null);
        database.onExecute(createWord, null);
        database.onExecute(createRelation, null);
        return true;
    }

    @Override
    public boolean insert(Fuzzyble database, FuzzyColumn column, String text) {
        InsertWordPair inserter = new InsertWordPair();
        for (String word: TextHelper.splitAndFilterText(text)) {
            for (String trigram: TextHelper.splitAndGetTrigrams(word)) {

                if (Thread.currentThread().isInterrupted()) return false;

                try {
                    inserter.insert(database, column, trigram, word);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        return true;
    }

    @Override
    public boolean populate(Fuzzyble source, Fuzzyble sync, FuzzyColumn column, ProgressListener listener) throws IOException {
        String dataQuery = "SELECT " + column.column + " FROM " + column.table;

        // get text of that columns
        SqlCursor textCursor = source.onQuery(dataQuery);
        if (textCursor == null) return false;

        int total = textCursor.count();
        float step = 1f / total;
        float current = 0f;

        while (textCursor.moveToNext()) {
            String text = textCursor.getString(0);
            insert(sync, column, text);

            current += step;
            listener.onProgress(current);
        }

        textCursor.close();

        return true;
    }

    @Override
    public String[] getAssociatedTables(FuzzyColumn column) {
        String baseName = "fuzzyble_" + getStrategyName();
        String trigramTable = baseName + "_tri";
        String wordsTable = baseName + "_word";
        String relationTable = baseName + "_rel";

        return new String[]{trigramTable, wordsTable, relationTable};
    }

    @Override
    public List<String> getSuggestions(Fuzzyble database, FuzzyColumn column, String word) {
        // return exact matched if found
        List<String> exact = performExactSearch(database, column, word);
        if (!exact.isEmpty()) return exact;

        // return partial match if found
        List<String> partial = performPartialSearch(database, column, word);
        if (!partial.isEmpty()) return partial;

        // now try fuzzy
        return performFuzzySearch(database, column, word);
    }

    private List<String> performExactSearch(Fuzzyble database, FuzzyColumn column, String word) {
        ArrayList<String> exact = new ArrayList<>();
        final String wordTable = getAssociatedTables(column)[1];

        try {
            SqlCursor exactQuery = database.onQuery("SELECT * FROM " + wordTable + " WHERE word = ?", new String[]{word});
            while (exactQuery.moveToNext()) {
                String s = exactQuery.getString(0);
                exact.add(s);
            }
            exactQuery.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exact;
    }

    private List<String> performPartialSearch(Fuzzyble database, FuzzyColumn column, String word) {
        ArrayList<String> partial = new ArrayList<>();
        final String wordTable = getAssociatedTables(column)[1];

        try {
            SqlCursor partialQuery = database.onQuery("SELECT * FROM " + wordTable + " WHERE word LIKE ? || '%' OR ? LIKE word || '%'", new String[]{word, word});
            while (partialQuery.moveToNext()) {
                String s = partialQuery.getString(0);
                partial.add(s);
            }

            partialQuery.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return partial;
    }

    private List<String> performFuzzySearch(Fuzzyble database, FuzzyColumn column, String word) {
        List<String> trigrams = TextHelper.splitAndGetTrigrams(word);
        if (trigrams.isEmpty()) return new ArrayList<>();

        final String[] tables = getAssociatedTables(column);
        final String trigramTable = tables[0];
        final String wordsTable = tables[1];
        final String relationTable = tables[2];

        List<String> suggestions = new ArrayList<>();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT DISTINCT w.word FROM ")
                .append(trigramTable).append(" t ")
                .append("JOIN ").append(relationTable).append(" r ON t.id = r.tId ")
                .append("JOIN ").append(wordsTable).append(" w ON r.wId = w.id ")
                .append("WHERE t.trigram IN (");

        for (int i = 0; i < trigrams.size(); i++) {
            queryBuilder.append("?");
            if (i < trigrams.size() - 1) {
                queryBuilder.append(", ");
            }
        }

        queryBuilder.append(")");

        try {
            SqlCursor cursor = database.onQuery(queryBuilder.toString(), trigrams.toArray(new String[]{}));
            while (cursor.moveToNext()) {
                String s = cursor.getString(0);
                suggestions.add(s);
            }

            cursor.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return FuzzyUtils.filterFuzzyMatched(word, suggestions, similarity);
    }

    // insert mechanism ------------------------------------------------------------
    private class InsertWordPair {
        void insert(Fuzzyble database, FuzzyColumn column, String trigram, String word) {
            try {
                // Insert trigram into trigramTable
                int trigramId = insertTrigram(database, column, trigram);

                // Insert word into wordsTable
                int wordId = insertWord(database, column, word);

                // Insert relationship into relationTable
                insertRelationship(database, column, trigramId, wordId);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        private int insertTrigram(Fuzzyble database, FuzzyColumn column, String trigram) {
            final String trigramTable = getAssociatedTables(column)[0];

            String insertTrigramQuery = "INSERT OR IGNORE INTO " + trigramTable + "(trigram) VALUES (?)";
            database.onExecute(insertTrigramQuery, new String[]{trigram});

            // Retrieve the ID of the inserted trigram
            String selectTrigramIdQuery = "SELECT id FROM " + trigramTable + " WHERE trigram = ?";

            try (SqlCursor cursor = database.onQuery(selectTrigramIdQuery, new String[]{trigram})) {
                if (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    return Integer.parseInt(id);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            // Return -1 if the trigram ID couldn't be retrieved
            return -1;
        }

        private int insertWord(Fuzzyble database, FuzzyColumn column, String word) {
            final String wordsTable = getAssociatedTables(column)[1];

            String insertWordQuery = "INSERT OR IGNORE INTO " + wordsTable + "(word) VALUES (?)";
            try {
                database.onExecute(insertWordQuery, new String[]{word});
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            // Retrieve the ID of the inserted word
            String selectWordIdQuery = "SELECT id FROM " + wordsTable + " WHERE word = ?";
            try (SqlCursor cursor = database.onQuery(selectWordIdQuery, new String[]{word})) {
                if (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    return Integer.parseInt(id);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            // Return -1 if the word ID couldn't be retrieved
            return -1;
        }

        private void insertRelationship(Fuzzyble database, FuzzyColumn column, int trigramId, int wordId) {
            if (trigramId == -1 || wordId == -1) return;

            final String relationTable = getAssociatedTables(column)[2];

            String insertRelationshipQuery = "INSERT INTO " + relationTable + "(tId, wId) VALUES (?, ?)";

            try {
                database.onExecute(insertRelationshipQuery, new String[]{String.valueOf(trigramId), String.valueOf(wordId)});
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
