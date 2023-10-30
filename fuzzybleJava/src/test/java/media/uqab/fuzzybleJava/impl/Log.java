package media.uqab.fuzzybleJava.impl;

public class Log {
    public static void log(String s) {
        try {
            StackTraceElement trace = Thread.currentThread().getStackTrace()[3];
            String[] cName = trace.getClassName().split("\\.");
            String cls = cName[cName.length - 1];
            StringBuilder tag = new StringBuilder(cls + "#" + trace.getMethodName() + "(" + trace.getLineNumber() + "):");
            for (int i = tag.length(); i < 50; i ++) {
                tag.append(" ");
            }

            System.out.println(tag + s);
        } catch (Exception ignored) {
            System.out.println("log: " + s);
        }
    }
}
