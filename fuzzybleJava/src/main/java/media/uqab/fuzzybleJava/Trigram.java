package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trigram implements Strategy {
    private final Similarity similarity;

    public Trigram() {
        this.similarity = new Levenshtein();
    }

    public Trigram(Similarity similarity) {
        this.similarity = similarity;
    }

    @Override
    public boolean create(Fuzzyble database, FuzzyColumn column) {
        String sql = "CREATE TABLE IF NOT EXISTS " + column.getFuzzyTableName() + "(" +
                "trigram VARCHAR(10) NOT NULL, " +
                "word VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY(trigram, word)" +
                ")";
        database.onExecute(sql, null);
        return true;
    }

    @Override
    public boolean delete(Fuzzyble database, FuzzyColumn column) {
        String sql = "DROP TABLE IF EXISTS " + column.getFuzzyTableName();
        database.onExecute(sql, null);
        return true;
    }

    @Override
    public boolean insert(Fuzzyble database, FuzzyColumn column, String text) {
        String insertSql = "INSERT OR IGNORE INTO " + column.getFuzzyTableName() + " (trigram, word) VALUES (?, ?)";

        for (String word: TextHelper.splitAndFilterText(text)) {
            for (String trigram: TextHelper.splitAndGetTrigrams(word)) {

                if (Thread.currentThread().isInterrupted()) return false;

                try {
                    String[] args = new String[]{trigram, word};
                    database.onExecute(insertSql, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    @Override
    public boolean populate(Fuzzyble source, Fuzzyble sync, FuzzyColumn column) throws IOException {
        String dataQuery = "SELECT " + column.column + " FROM " + column.table;

        // get text of that columns
        SqlCursor textCursor = source.onQuery(dataQuery);
        if (textCursor == null) return false;

        while (textCursor.moveToNext()) {
            String text = textCursor.getString(0);
            insert(sync, column, text);
        }

        textCursor.close();

        return true;
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

        try {
            SqlCursor exactQuery = database.onQuery("SELECT * FROM " + column.getFuzzyTableName() + " WHERE word = ?", new String[]{word});
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

        try {
            SqlCursor partialQuery = database.onQuery("SELECT * FROM " + column.getFuzzyTableName() + " WHERE word LIKE ? || '%' OR ? LIKE word || '%'", new String[]{word, word});
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

        StringBuilder sb = new StringBuilder();
        for (String t: trigrams) sb.append("'").append(t).append("',");
        String tr = sb.toString();
        if (!tr.isEmpty()) tr = tr.substring(0, tr.length() - 1);

        final String query = "SELECT * FROM " + column.getFuzzyTableName() + " WHERE trigram IN (" + tr + ")";

        List<String> suggestions = new ArrayList<>();
        try {
            SqlCursor cursor = database.onQuery(query, null);

            while (cursor.moveToNext()) {
                String w = cursor.getString(1);
                suggestions.add(w);
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FuzzyUtils.filterFuzzyMatched(word, suggestions, similarity);
    }
}
