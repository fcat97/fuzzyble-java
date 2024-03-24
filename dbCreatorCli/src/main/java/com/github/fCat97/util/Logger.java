package com.github.fCat97.util;

public class Logger {

    private static void print(String text) {
        print(text, Color.RESET);
    }

    private static void print(String text, Color color) {
        System.out.println("::".repeat(30));
        System.out.println(color.getColor() + text + Color.RESET.getColor());
    }

    /**
     * Info
     * @param s text
     */
    public static void i(String s) {
        print(s);
    }

    /**
     * Success
     * @param s text
     */
    public static void s(String s) {
        print(s, Color.GREEN);
    }

    /**
     * Warning
     * @param s warning
     */
    public static void w(String s) {
        print(s, Color.YELLOW);
    }

    /**
     * Error
     * @param s cause
     */
    public static void e(String s) {
        print(s, Color.RED);
    }
}
