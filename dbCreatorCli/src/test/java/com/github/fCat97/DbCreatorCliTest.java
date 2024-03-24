package com.github.fCat97;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbCreatorCliTest {
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    public void setUpStreams() {
        out.reset();
        err.reset();
//        System.setOut(new PrintStream(out));
//        System.setErr(new PrintStream(err));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    void testMainWithNoArg() {
        final var cmd = new DbCreatorCli();
        final var args = new String[]{""};

        new CommandLine(cmd)
                .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.ON))
                .execute();

//        assertEquals(true, out.toString().contains("-h"));
//        assertEquals(true, out.toString().contains("--help"));
//        assertEquals(true, out.toString().contains("Show this help message and exit."));
//
//        assertEquals(true, out.toString().contains("-V"));
//        assertEquals(true, out.toString().contains("--version"));
//        assertEquals(true, out.toString().contains("Print version information and exit."));
    }
}