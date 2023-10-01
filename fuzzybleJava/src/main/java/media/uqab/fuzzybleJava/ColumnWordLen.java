package media.uqab.fuzzybleJava;

public class ColumnWordLen extends FuzzyColumn {
    public ColumnWordLen(String table, String column) {
        super(table, column);
    }

    String getFuzzyTableName() {
        return "fuzzyble_" + table + "_" + column;
    }
}
