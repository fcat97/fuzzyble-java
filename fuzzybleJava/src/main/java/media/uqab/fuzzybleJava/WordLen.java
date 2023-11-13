package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WordLen implements Strategy {
    private final Similarity similarity;

    public WordLen() {
        this.similarity = new Levenshtein();
    }

    public WordLen(Similarity similarity) {
        this.similarity = similarity;
    }

    @Override
    public boolean create(Fuzzyble database, FuzzyColumn column) {
        String sql = "CREATE TABLE IF NOT EXISTS " + column.getFuzzyTableName() + "(word TEXT, len INTEGER)";
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
        final String fuzzyTable = column.getFuzzyTableName();

        String insertSql = "INSERT INTO " + fuzzyTable + " (word, len) " +
                "SELECT ?, ? " +
                "WHERE NOT EXISTS (SELECT 1 FROM " + fuzzyTable + " WHERE word LIKE ? || '%' OR ? LIKE word || '%') " +
                "OR ? < (SELECT min(len) FROM " + fuzzyTable + " WHERE word LIKE ? || '%' OR ? LIKE word || '%')";

        // insert into (word,len) table
        boolean allInserted = true;
        for(String w: TextHelper.splitAndFilterText(text)) {
            try {
                String l = String.valueOf(w.length());
                String[] args = new String[]{w, l, w, w, l, w, w};
                database.onExecute(insertSql, args);
            } catch (Exception e) {
                e.printStackTrace();
                allInserted = false;
            }
        }

        return allInserted;
    }

    @Override
    public boolean populate(Fuzzyble source, Fuzzyble sync, FuzzyColumn column, ProgressListener listener) throws IOException {
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
        return performFuzzySearch(database, column, word, 3);
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

    private List<String> performFuzzySearch(
            Fuzzyble database,
            FuzzyColumn column,
            String word,
            int matchFirstNchar
    ) {
        // base condition for recursion to terminate
        if (matchFirstNchar < 1) return new ArrayList<>();

        // word length range {l-2, l-1, l, l+1, l+2}
        int l = word.length();
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(l + 2).append("',");
        sb.append("'").append(l + 1).append("',");
        sb.append("'").append(l).append("',");
        if (l > 3) sb.append("'").append(l - 1).append("',");
        if (l > 4) sb.append("'").append(l - 2).append("',");
        String length = sb.substring(0, sb.length() - 1);

        System.out.println(length);

        ArrayList<String> words = new ArrayList<>();
        try {
            SqlCursor cursor = database.onQuery("SELECT * FROM " + column.getFuzzyTableName() + " WHERE len IN (" + length + ")");
            while (cursor.moveToNext()) {
                String s = cursor.getString(0);
                words.add(s);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> fuzzyMatched = FuzzyUtils.filterFuzzyMatched(word, words, similarity);

        if (fuzzyMatched.size() > 0) {
            return fuzzyMatched;
        } else {
            return performFuzzySearch(database, column, word, matchFirstNchar - 1);
        }
    }
}
