package media.uqab.fuzzybleJava;

import java.io.IOException;
import java.util.List;

public interface Strategy {
    String getStrategyName();

    boolean create(Fuzzyble database, FuzzyColumn column);

    boolean insert(Fuzzyble database, FuzzyColumn column, String text);

    boolean populate(Fuzzyble source, Fuzzyble sync, FuzzyColumn column, ProgressListener listener) throws IOException;

    /**
     * Returns all the table names associated with this {@linkplain FuzzyColumn}
     * which will be created during {@linkplain Strategy#create} or need to be
     * deleted for removing fuzzyble data of this column.
     * @param column {@linkplain FuzzyColumn}
     * @return array of table names that are related to this {@linkplain FuzzyColumn}.
     */
    String[] getAssociatedTables(FuzzyColumn column);

    /**
     * Get fuzzy matched word suggestions
     * @param database database which has implemented {@link Fuzzyble} interface
     * @param column {@link FuzzyColumn} to perform search on.
     * @param word searched word.
     * @return matched words by priority of Exact Match > Partial Match > Fuzzy Match
     */
    List<String> getSuggestions(Fuzzyble database, FuzzyColumn column, String word);
}
