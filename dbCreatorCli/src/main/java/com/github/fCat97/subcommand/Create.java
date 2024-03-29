package com.github.fCat97.subcommand;

import com.github.fCat97.Const;
import com.github.fCat97.ProjectConfig;
import com.github.fCat97.database.Database;
import com.github.fCat97.util.Logger;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import media.uqab.fuzzybleJava.*;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;


@CommandLine.Command(name = "create", description = "create a new project in the current directory", mixinStandardHelpOptions = true)
public class Create implements Runnable {
    @CommandLine.ArgGroup(multiplicity = "1")
    What what;

    static class What {
        @CommandLine.ArgGroup(heading = "Create new project%n", exclusive = false)
        Prj project;

        @CommandLine.ArgGroup(heading = "Create new fuzzy database%n", exclusive = false)
        Db database;

    }

    static class Prj {
        @CommandLine.Option(names = {"-f", "--force"}, description = "force create new project")
        boolean force;

        @CommandLine.Option(names = {"-n", "--name"}, required = true, description = "project name")
        String name;

        @CommandLine.Option(names = {"-r", "--src-db"}, required = true, description = "Src database path")
        String src;

        @CommandLine.Option(names = {"-k", "--sink-db"}, required = true, description = "Sink database path")
        String sink;
    }

    static class Db {
        @CommandLine.Option(names = {"-t", "--table-name"}, required = true, description = "specify table name of src database")
        String table;

        @CommandLine.Option(names = {"-c", "--column-name"}, required = true, description = "specify table's column name of src database")
        String column;

        @CommandLine.ArgGroup(heading = "specify strategy to create fuzzy column%n", multiplicity = "1")
        St strategy;

        @CommandLine.Option(names = {"-d", "--lav-distance"}, description = "(not ready yet) specify Levenshtein distance of words to calculate similarity")
        int distance;

        @CommandLine.Option(names = {"-o", "--override"}, description = "override already populated tables")
        boolean override;
    }

    static class St {
        @CommandLine.Option(names = {"--trigram"}, description = "Use trigram strategy to create fuzzy db", required = true)
        boolean trigram;

        @CommandLine.Option(names = {"--wordlen"}, description = "Use wordLen strategy to create fuzzy db", required = true)
        boolean worLen;
    }

    @Override
    public void run() {
        if (what.project != null) {
            createProject(what.project);
        } else if (what.database != null) {
            createDatabase(what.database);
        }
    }

    private void createProject(Prj p) {
        if (p.force) {
            try {
                createProjectConfig(p.name, p.src, p.sink);
            } catch (IOException e) {
                Logger.e(e.getLocalizedMessage());
            }
        } else {
            Path path = Paths.get(Const.PROJECT_INFO_FILE);
            try {
                var config = ProjectConfig.getFrom(path, false);
                if (config == null) {
                    createProjectConfig(p.name, p.src, p.sink);
                } else {
                    Logger.e("an existing project[" + config.name + "] found in this directory\n" +
                            "use -f to recreate project\n");
                    new CommandLine(new Create()).execute("-h");
                }
            } catch (IOException e) {
                Logger.e("Something bad happened: " + e.getMessage());
            }
        }
    }

    private void createProjectConfig(String name, String srcDb, String sinkDb) throws IOException {
        var path = Paths.get(Const.PROJECT_INFO_FILE);
        if (Files.exists(path)) {
            Files.delete(path);
        }

        var p = new ProjectConfig();
        p.name = name;
        p.srcDb = srcDb;
        p.sinkDb = sinkDb;
        p.writeTo(path);

        Logger.s("Project created @ " + path.toAbsolutePath() + "\n");
        new CommandLine(new Info()).execute("-p");
    }

    private void createDatabase(Db d) {
        var p = ProjectConfig.get();
        if (p == null) return;

        if (p.srcDb == null) {
            Logger.e("src database must be specified first");
            new CommandLine(new Set()).execute("-h");
            return;
        }

        if (!Files.exists(Paths.get(p.srcDb))) {
            Logger.e("src db file not found in " + p.srcDb);
            return;
        }

        if (p.sinkDb == null) {
            Logger.e("sink database must be specified first");
            new CommandLine(new Set()).execute("-h");
            return;
        }

        try {
            final var srcDb = Database.getInstance(Paths.get(p.srcDb));
            Database sinkDb;
            if (Objects.equals(p.srcDb, p.sinkDb)) {
                sinkDb = srcDb;
            } else {
                if (!Files.exists(Paths.get(p.sinkDb))) {
                    Logger.w("sink not found. A new one will be created");
                }
                sinkDb = Database.getInstance(Paths.get(p.sinkDb), true);
            }

            Strategy strategy = null;
            if (d.strategy != null) {
                if (d.strategy.trigram) {
                    strategy = new Trigram();
                }
                if (d.strategy.worLen) {
                    strategy = new WordLen();
                }
            } else {
                strategy = new Trigram();
            }

            FuzzyCursor cursor = new FuzzyCursor(srcDb, sinkDb, strategy);
            FuzzyColumn column = new FuzzyColumn(d.table, d.column);

            if (d.override || !cursor.isFuzzyble(column)) {
                Logger.i("initializing fuzzyble on " + d.table + "/" + d.column);
                cursor.createFuzzyble(column, d.override);
            } else {
                Logger.i("column already initialized. Skipping...");
            }

            if (d.override || !cursor.isPopulated(column)) {
                Logger.w("populating data. please wait...");

                final var progressBar = ProgressBar.builder()
                        .setStyle(ProgressBarStyle.ASCII)
                        .clearDisplayOnFinish()
                        .setTaskName("Populating")
                        .setInitialMax(100)
                        .build();
                ProgressListener listener = v -> progressBar.stepTo((int) (v * 100));

                cursor.populate(column, d.override, listener);

                if (srcDb != sinkDb) sinkDb.close();
                srcDb.close();
            } else {
                Logger.i("column is already populated. Skipping...");
            }

            Logger.s("\nData successfully populated on " + d.table + "/" + d.column);
        } catch (SQLException | ClassNotFoundException | IOException e) {
            Logger.e(e.getLocalizedMessage());
        }
    }

    public static void main(String[] args) {
//        if (args.length == 0) args = "-h".split(" ");
//        if (args.length == 0) args = "-f -n tawjeehul-quran -r tafsir_tawzihul_quran.db -k tafsir_tawzihul_quran_fuzzy.db".split(" ");
        if (args.length == 0) args = "-t tafsir -c content --trigram".split(" ");

        new CommandLine(new Create())
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute(args);
    }
}
