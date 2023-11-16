package media.uqab.fuzzybleJavaExt;

import media.uqab.fuzzybleJava.FuzzyColumn;
import media.uqab.fuzzybleJava.Fuzzyble;
import media.uqab.fuzzybleJava.Trigram;

import java.util.ArrayList;
import java.util.List;

public class StrategyBn extends Trigram {
    private static String scapeChars = "";
    @Override
    public boolean insert(Fuzzyble database, FuzzyColumn column, String text) {
        String insertSql = "INSERT OR IGNORE INTO " + getAssociatedTables(column)[0] + " (trigram, word) VALUES (?, ?)";

        for (String word: splitToWords(text)) {
            for (String trigram: getTrigrams(word)) {

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


    private String[] splitToWords(String sentence) {
        return sentence.split(" ");
    }

    private List<String> getTrigrams(String word) {
        return new ArrayList<>();
    }
}
