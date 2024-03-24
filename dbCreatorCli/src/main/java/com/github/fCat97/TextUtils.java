package com.github.fCat97;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {

    public static String formatAsTable(int col, String header, List<String> body, String footer, boolean multiline) {
        var sb = new StringBuilder();
        sb.append("|>").append("=".repeat(col - 4)).append("<|\n");
        for(String h: formatMultiline(header, col, "| ", " |")) {
            sb.append(h).append("\n");
        }

        sb.append("|").append("-".repeat(col - 2)).append("|\n");
        for(String s: body) {
            for (String b: formatMultiline(s, col, "| ", " ")) {
                sb.append(b).append("\n");
            }
            sb.append("|").append("-".repeat(col - 2)).append("|\n");
        }
        return sb.toString();
    }

    public static String formatAsTable(int col, String header, List<String> body, String footer) {
        return formatAsTable(col, header, body, footer, false);
    }

    public static String getRPadded(String text, int lineLen, char pad) {
        var rPadded = new StringBuilder(text);
        while (rPadded.length() < lineLen) {
            rPadded.append(pad);
        }
        return rPadded.toString();
    }

    public static List<String> formatMultiline(String longText, int lineWidth, String padStart, String padEnd) {
        var lines = new ArrayList<String>();
        int windowLen = lineWidth - padStart.length() - padEnd.length();
        int padWidth = padStart.length() + padEnd.length();

        if (longText.length() < windowLen) {
            lines.add(padStart + getRPadded(longText, lineWidth - padWidth, ' ') + padEnd);
            return lines;
        }

        var words = longText.split(" ");
        var line = new StringBuilder();
        int i = 0;
        var w = words[i];

        while (w != null) {
            if (w.length() + " ".length() <= windowLen - line.length()) {
                line.append(w).append(" ");
                if (++i < words.length) {
                    w = words[i];
                } else {
                    w = null;
                }
            } else {
                if (!line.isEmpty()) {
                    lines.add(padStart + getRPadded(line.toString(), lineWidth - padWidth, ' ') + padEnd);
                    line = new StringBuilder();
                }

                if (w.length() > windowLen) {
                    line.append(w, 0, windowLen - 1);
                    line.append("-");
                    lines.add(padStart + line + padEnd);

                    w = w.substring(windowLen - 2);
                    line = new StringBuilder();
                } else if (w.length() == windowLen) {
                    lines.add(padStart + w + padEnd);
                    if (++i < words.length) {
                        w = words[i];
                    } else {
                        w = null;
                    }
                }
            }
        }

        return lines;
    }
}
