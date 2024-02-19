package com.github.fCat97;


import picocli.CommandLine;

@CommandLine.Command(
        name = "dbCreatorCli",
        mixinStandardHelpOptions = true,
        version = "0.6.6",
        description = "utility to create fuzzyble database for fuzzyble-java"
)
public class DbCreatorCli implements Runnable {



    @Override
    public void run() {

    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DbCreatorCli()).execute(args);
        System.exit(exitCode);
    }
}
