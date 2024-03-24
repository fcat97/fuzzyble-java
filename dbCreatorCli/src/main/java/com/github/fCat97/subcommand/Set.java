package com.github.fCat97.subcommand;

import com.github.fCat97.Const;
import com.github.fCat97.ProjectConfig;
import com.github.fCat97.util.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


@CommandLine.Command(
        name = "set",
        description = "set/update project config",
        mixinStandardHelpOptions = true
)
public class Set implements Runnable {

    @CommandLine.ArgGroup(heading = "Set either or all info%n", exclusive = false, multiplicity = "1")
    SetInfo setInfo;

    static class SetInfo {
        @CommandLine.Option(names = {"-n", "--name"}, description = "Set name of the project")
        String name;

        @CommandLine.Option(names = {"-r", "--src-db"}, description = "Set src db path")
        String srcDb;

        @CommandLine.Option(names = {"-y", "--sink-db"}, description = "Set sink db path")
        String sinkDb;
    }

    @Override
    public void run() {
        ProjectConfig p = ProjectConfig.get();
        if (p == null) return;

        if (setInfo.name != null) p.name = setInfo.name;
        if (setInfo.srcDb != null) p.srcDb = setInfo.srcDb;
        if (setInfo.sinkDb != null) p.sinkDb = setInfo.sinkDb;

        Path path = Paths.get(Const.PROJECT_INFO_FILE);
        try {
            p.writeTo(path);
            Logger.s("project updated successfully");
            Logger.i(p.toString());
        } catch (IOException e) {
            Logger.e("failed to save!");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) args = "-h".split(" ");

        new CommandLine(new Set())
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute(args);
    }
}
