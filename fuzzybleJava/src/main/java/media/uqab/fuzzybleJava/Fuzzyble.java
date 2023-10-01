package media.uqab.fuzzybleJava;

/**
 * Implement this interface to enable fuzzy search on a database
 */
public interface Fuzzyble {
    /**
     * Perform Query on Database to create fuzzy tables.
     * @param query query to perform
     */
    SqlCursor onQuery(final String query);

    SqlCursor onQuery(final String query, String[] args);

    void onExecute(final String sql, String[] args);
}
