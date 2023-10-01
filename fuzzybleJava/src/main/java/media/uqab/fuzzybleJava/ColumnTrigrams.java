package media.uqab.fuzzybleJava;

public class ColumnTrigrams extends FuzzyColumn {
    public ColumnTrigrams(String table, String column) {
        super(table, column);
    }

    @Override
    String getFuzzyTableName() {
        return "fuzzyble_trigram_" + table + "_" + column;
    }
}
