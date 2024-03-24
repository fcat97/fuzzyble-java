package com.github.fCat97.subcommand;

import com.github.fCat97.ProjectConfig;
import com.github.fCat97.TextUtils;
import com.github.fCat97.database.Database;
import com.github.fCat97.util.Logger;
import media.uqab.fuzzybleJava.FuzzyColumn;
import media.uqab.fuzzybleJava.FuzzyCursor;
import media.uqab.fuzzybleJava.Trigram;
import media.uqab.fuzzybleJava.WordLen;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;

@CommandLine.Command(name = "search", description = "get fuzzy suggestion from populated table", mixinStandardHelpOptions = true)
public class Search implements Runnable {

    @CommandLine.Option(names = {"-t", "--table"}, description = "table name to search on", required = true)
    String table;

    @CommandLine.Option(names = {"-c", "--column"}, description = "column name to search on", required = true)
    String column;

    @CommandLine.Option(names = {"-s", "--search"}, description = "word(s) to search", required = true)
    String search;

    @Override
    public void run() {
        var project = ProjectConfig.get();
        if (project == null) return;

        var srcPath = Paths.get(project.srcDb);
        var sinkPath = Paths.get(project.sinkDb);

        if (Files.notExists(srcPath)) {
            Logger.e(srcPath + " doesn't exist");
            return;
        }

        if (!srcPath.equals(sinkPath)) {
            if (Files.notExists(sinkPath)) {
                Logger.e(sinkPath + " doesn't exist");
                return;
            }
        }

        try {
            Database srcDb = Database.getInstance(srcPath);
            Database sinkDb = Database.getInstance(sinkPath);

            FuzzyCursor cursorTri = new FuzzyCursor(srcDb, sinkDb, new Trigram());
            FuzzyCursor cursorWord = new FuzzyCursor(srcDb, sinkDb, new WordLen());

            var fColumn = new FuzzyColumn(table, column);

            if (cursorTri.isFuzzyble(fColumn) && cursorTri.isPopulated(fColumn)) {
                var words = cursorTri.getFuzzyWords(fColumn, search);
                if (words.length > 0) {
                    Logger.i(TextUtils.formatAsTable(80, "Suggestion (trigram)", Arrays.asList(words), ""));
                } else {
                    Logger.w("no suggestion found(trigram)");
                }
            }

            if (cursorWord.isFuzzyble(fColumn) && cursorWord.isPopulated(fColumn)) {
                var words = cursorWord.getFuzzyWords(fColumn, search);
                if (words.length > 0) {
                    Logger.i(TextUtils.formatAsTable(80, "Suggestion (wordLen)", Arrays.asList(words), ""));
                } else {
                    Logger.w("no suggestion found(wordLen)");
                }
            }
        } catch (SQLException | ClassNotFoundException | IOException e) {
            Logger.e(e.getLocalizedMessage());
        }

    }

    public static void main(String[] args) {
        if (args.length == 0) args = "-t tafsir -c content -s জিবন".split(" ");

        new CommandLine(new Search())
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute(args);
    }
}
