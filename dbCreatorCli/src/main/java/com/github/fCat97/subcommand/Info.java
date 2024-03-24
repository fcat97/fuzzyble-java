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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;


@CommandLine.Command(
        name = "info",
        description = "Show current project info",
        mixinStandardHelpOptions = true
)
public class Info implements Runnable {

    @CommandLine.ArgGroup(multiplicity = "1")
    Prj project;

    static class Prj {
        @CommandLine.Option(names = {"-p", "--project"}, description = "show project config info")
        boolean project;

        @CommandLine.ArgGroup(heading = "show information about database%n", exclusive = false)
        Db database;
    }

    static class Db {
        @CommandLine.ArgGroup(heading = "show information of either src or sink database%n", multiplicity = "1")
        SrcSink srcSink;

        @CommandLine.ArgGroup(heading = "show information about a table%n", exclusive = false)
        Table table;
    }

    static class SrcSink {
        @CommandLine.Option(
                names = {"-r", "--src"},
                description = "show description about source database",
                required = true
        )
        boolean src;

        @CommandLine.Option(
                names = {"-k", "--sink"},
                description = "show description about sink database",
                required = true
        )
        boolean sink;
    }

    static class Table {
        @CommandLine.Option(
                names = {"-t", "--table"},
                description = "show description about table",
                required = true
        )
        String table;

        @CommandLine.Option(
                names = {"-c", "--column"},
                description = "show description about column"
        )
        String column;
    }

    @Override
    public void run() {
        final var p = ProjectConfig.get();
        if (p == null) return;

        if (project.database != null) {
            if (project.database.srcSink.src) {
                showDbInfo(p.srcDb);
            } else if (project.database.srcSink.sink) {
                showDbInfo(p.sinkDb);
            }
        } else if (project.project) {
            showProjectInfo(p);
        }
    }

    private void showProjectInfo(ProjectConfig p) {
        Logger.s(p.toString());

        var src = Paths.get(p.srcDb);
        var sink = Paths.get(p.sinkDb);
        if (Files.exists(src) && Files.exists(sink)) {
            try {
                Database srcDb = Database.getInstance(src);
                Database sinkDb = Database.getInstance(sink);
                FuzzyCursor cursorTri = new FuzzyCursor(srcDb, sinkDb, new Trigram());
                FuzzyCursor cursorWord = new FuzzyCursor(srcDb, sinkDb, new WordLen());

                ArrayList<String> populated = new ArrayList<>();
                for (String t : srcDb.getTables()) {
                    for (String c : srcDb.getColumns(t)) {
                        FuzzyColumn column = new FuzzyColumn(t, c);

                        if (cursorTri.isPopulated(column))
                            populated.add(column.table + "/" + column.column + " is populated with trigram");
                        if (cursorWord.isPopulated(column))
                            populated.add(column.table + "/" + column.column + " is populated with trigram");
                    }
                }

                if (!populated.isEmpty()) {
                    var out = TextUtils.formatAsTable(80, "Ready table/colum to fuzzy search", populated, "");
                    Logger.s(out);
                }
            } catch (SQLException | ClassNotFoundException e) {
                Logger.e(e.getLocalizedMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void showDbInfo(String dbpath) {
        if (dbpath == null) {
            Logger.w("Please specify the database using 'set' command");
            new CommandLine(new Set()).execute("-h");
            return;
        }

        var dbPath = Paths.get(dbpath);
        if (!Files.exists(dbPath)) {
            Logger.e("file not found: " + dbPath.toAbsolutePath());
            if (project.database.srcSink.sink) {
                var p = ProjectConfig.get(false);

                if (!Objects.equals(p.srcDb, p.sinkDb)) {
                    Logger.w("specified sink db (" + p.sinkDb + ") " + "does not exist.");
                    Logger.i("It will automatically be created once you create a fuzzy column. Or you can use an existing database, just paste the db file in this location.");
                }
            }
            return;
        }

        if (project.database.table != null) {
            showTableInfo(dbPath, project.database.table);
        } else {
            var db = getDatabase(dbPath);
            if (db == null) return;

            var tables = db.getTables();
            String table = TextUtils.formatAsTable(48, "Tables in " + dbpath, tables, "");
            Logger.i(table);

            db.close();
        }
    }

    private void showTableInfo(Path dbPath, Table table) {
        if (table.column != null) {
            showColumnInfo(dbPath, table);
        } else {
            var db = getDatabase(dbPath);
            if (db == null) return;

            var columns = db.getColumns(table.table);
            var columnFormatted = TextUtils.formatAsTable(48, "Columns in " + dbPath + "/" + table.table, columns, "");
            Logger.i(columnFormatted);

            db.close();
        }
    }

    private void showColumnInfo(Path dbPath, Table table) {
        var db = getDatabase(dbPath);
        if (db == null) return;

        var header = "First 10 items of " + table.table + "/" + table.column;
        var rows = db.getData(table.table, table.column, 10);
        var itemFormatted = TextUtils.formatAsTable(120, header, rows, "");
        Logger.i(itemFormatted);

        db.close();
    }

    private Database getDatabase(Path dbPath) {
        try {
            return com.github.fCat97.database.Database.getInstance(dbPath);
        } catch (SQLException | ClassNotFoundException e) {
            Logger.e("Failed to load database " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) args = "-p".split(" ");

        new CommandLine(new Info())
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute(args);
    }
}
