package com.github.fCat97;


import com.github.fCat97.subcommand.Create;
import com.github.fCat97.subcommand.Info;
import com.github.fCat97.subcommand.Search;
import com.github.fCat97.subcommand.Set;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "dbCreatorCli",
        mixinStandardHelpOptions = true,
        version = "0.6.6",
        description = "cli tool to create fuzzyble database for fuzzyble-java",
        subcommands = {Info.class, Create.class, Set.class, Search.class}
)
public class DbCreatorCli implements Runnable {
    @Override
    public void run() {

    }

    public static void main(String[] args) {
        if (args.length == 0) args = "-h".split(" ");

        int exitCode = new CommandLine(new DbCreatorCli())
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute(args);
        System.exit(exitCode);
    }
}
