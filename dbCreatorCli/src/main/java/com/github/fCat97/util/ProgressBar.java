package com.github.fCat97.util;

import java.util.stream.Stream;

/**
 * @author <a href="https://medium.com/javarevisited/how-to-display-progressbar-on-the-standard-console-using-java-18f01d52b30e">Courtesy</a>
 */
public class ProgressBar {
    private static char incomplete = '░'; // U+2591 Unicode Character
    private static char complete = '█'; // U+2588 Unicode Character

    private final StringBuilder builder = new StringBuilder();
    private final int length;
    public ProgressBar(int length) {
        this.length = length;
        Stream.generate(() -> incomplete).limit(length).forEach(builder::append);
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void showProgress(int progress) {
        int i = progress - 1; // progress Index
        if (i >= 0) builder.replace(i, i + 1, String.valueOf(complete));
        String progressBar = "\r" + builder;
        System.out.print(progressBar);
    }

    public static void main(String[] args) {
        var p = new ProgressBar(100);
        p.printMessage("Loading");
        for (int i = 0; i <= 100; i++) {
            try {
                Thread.sleep(80);
            } catch (InterruptedException ignored) {

            }
            p.showProgress(i);
        }
    }
}