package com.github.fCat97;

import com.github.fCat97.subcommand.Create;
import com.github.fCat97.util.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectConfig {
    public String name;
    public String srcDb;
    public String sinkDb;
    public String strategy;

    public ProjectConfig() {
    }

    public void writeTo(Path path) throws IOException {
        var data = new StringBuilder();
        data.append("name:");
        if (name != null) data.append(name);
        data.append("\n");

        data.append("srcDb:");
        if (srcDb != null) data.append(srcDb);
        data.append("\n");

        data.append("sinkDb:");
        if (sinkDb != null) data.append(sinkDb);
        data.append("\n");

        data.append("strategy:");
        if (strategy != null) data.append(strategy);
        data.append("\n");

        Files.writeString(path, data.toString());
    }

    public static ProjectConfig get() {
        return get(true);
    }

    public static ProjectConfig get(boolean showInfo) {
        return getFrom(Paths.get(Const.PROJECT_INFO_FILE), showInfo);
    }

    public static ProjectConfig getFrom(Path path) {
        return getFrom(path, true);
    }

    public static ProjectConfig getFrom(Path path, boolean showError) {
        if (!Files.exists(path) && showError) {
            Logger.w("Directory isn't recognized as a dbCreatorCli project");
            Logger.w("create a project first. run: \n");
            new CommandLine(new Create()).execute("-h");
            return null;
        }

        try {
            final String data = Files.readString(path, StandardCharsets.UTF_8);
            final var p = new ProjectConfig();
            data.lines().forEach(s -> {
                var x = s.split(":");
                var x0 = x[0];
                var x1 = x.length > 1 ? x[1] : null;
                switch (x0) {
                    case "name":
                        p.name = x1;
                        break;
                    case "srcDb":
                        p.srcDb = x1;
                        break;
                    case "sinkDb":
                        p.sinkDb = x1;
                        break;
                    case "strategy":
                        p.strategy = x1;
                        break;
                }
            });
            return p;
        } catch (IOException e) {
            Logger.e("failed to read project: " + path.toAbsolutePath());
        }

        return null;
    }

    @Override
    public String toString() {
        return "Project{" +
                "name='" + name + '\'' +
                ", srcDb='" + srcDb + '\'' +
                ", sinkDb='" + sinkDb + '\'' +
                ", strategy='" + strategy + '\'' +
                '}';
    }

    public static void main(String[] args) {
        System.out.println(ProjectConfig.get());
    }
}
