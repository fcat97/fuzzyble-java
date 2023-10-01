package media.uqab.fuzzybleJava;

import java.io.Serializable;

/**
 * Schema of table and column on which fuzzy search will be enabled
 *
 * <p>
 * Not mandatory to include all the columns rather specify only
 * those columns on which fuzzy search will be performed.
 *
 * <p>
 * For now, there are two supported types:
 * {@linkplain ColumnTrigrams} and {@linkplain ColumnWordLen}
 *
 */
public abstract class FuzzyColumn implements Serializable {
    public final String table;
    public final String column;

    protected FuzzyColumn(String table, String column) {
        this.table = table;
        this.column = column;
    }

    abstract String getFuzzyTableName();
}
