package media.uqab.fuzzybleJava;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for fuzzy match
 *
 * @author github/fCat97
 */
class FuzzyUtils {

    private final Similarity similarity;

    public FuzzyUtils(Similarity similarity) {
        this.similarity = similarity;
    }

    /**
     * Get fuzzy matched word suggestions
     * @param database database which has implemented {@link Fuzzyble} interface
     * @param column {@link FuzzyColumn} to perform search on.
     * @param word searched word.
     * @return matched words by priority of Exact Match > Partial Match > Fuzzy Match
     */
    String[] getWordSuggestions(
            Fuzzyble database,
            FuzzyColumn column,
            String word) {

        // return exact matched if found
        try {
            ArrayList<String> exact = new ArrayList<>();
            SqlCursor exactQuery = database.onQuery("SELECT * FROM " + column.getFuzzyTableName() + " WHERE word = ?", new String[]{word});
            while (exactQuery.moveToNext()) {
                String s = exactQuery.getString(0);
                exact.add(s);
            }
            exactQuery.close();
            if (exact.size() > 0) return exact.toArray(new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }

        // return partial match if found
        try {
            SqlCursor partialQuery = database.onQuery("SELECT * FROM " + column.getFuzzyTableName() + " WHERE word LIKE ? || '%' OR ? LIKE word || '%'", new String[]{word, word});
            ArrayList<String> partial = new ArrayList<>();
            while (partialQuery.moveToNext()) {
                String s = partialQuery.getString(0);
                partial.add(s);
            }

            partialQuery.close();
            if (partial.size() > 0) return partial.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // now try fuzzy
        if (column instanceof ColumnTrigrams) {
            return trigramFuzzy(database, column, word);
        } else {
            return wordLenFuzzy(database, column, word, 3);
        }
    }

    private String[] trigramFuzzy(
            Fuzzyble database,
            FuzzyColumn column,
            final String word
    ) {
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

        return filterFuzzyMatched(word, suggestions).toArray(new String[]{});
    }

    /**
     * Perform fuzzy search
     * @param database database which implemented {@link Fuzzyble} interface
     * @param column column to perform search on
     * @param word searched word
     * @param matchFirstNchar returned words should match first 'n' characters
     * @return fuzzy matched words
     */
    private String[] wordLenFuzzy(
            Fuzzyble database,
            FuzzyColumn column,
            final String word,
            int matchFirstNchar) {

        // base condition for recursion to terminate
        if (matchFirstNchar < 1) return new String[0];

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

        List<String> fuzzyMatched = filterFuzzyMatched(word, words);

        if (fuzzyMatched.size() > 0) {
            return fuzzyMatched.toArray(new String[0]);
        } else {
            return wordLenFuzzy(database, column, word, matchFirstNchar - 1);
        }
    }

    /**
     * Filter words based on levenshtein distance
     * @param word searched word
     * @param words fuzzy matched words
     * @return filtered words
     */
    private List<String> filterFuzzyMatched(final String word, List<String> words) {
        List<String> fuzzyWords = MyStream.of(words)
                .filter(fuzzyWord -> similarity.isSimilar(word, fuzzyWord))
                .toList();

        fuzzyWords.sort(similarity::similarityIndex);

        return fuzzyWords;
    }
}
