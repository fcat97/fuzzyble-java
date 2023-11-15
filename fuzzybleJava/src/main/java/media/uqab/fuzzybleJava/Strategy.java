package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.List;

public interface Strategy {
    boolean create(Fuzzyble database, FuzzyColumn column);

    boolean insert(Fuzzyble database, FuzzyColumn column, String text);

    boolean populate(Fuzzyble source, Fuzzyble sync, FuzzyColumn column, ProgressListener listener) throws IOException;

    /**
     * Get all the table name which will be created with {@linkplain Strategy#create} commands
     * @param column {@linkplain FuzzyColumn}
     * @return array of table names that are related to this {@linkplain FuzzyColumn}.
     */
    String[] getTables(FuzzyColumn column);

    /**
     * Get fuzzy matched word suggestions
     * @param database database which has implemented {@link Fuzzyble} interface
     * @param column {@link FuzzyColumn} to perform search on.
     * @param word searched word.
     * @return matched words by priority of Exact Match > Partial Match > Fuzzy Match
     */
    List<String> getSuggestions(Fuzzyble database, FuzzyColumn column, String word);
}
