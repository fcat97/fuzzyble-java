package media.uqab.fuzzybleJava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class TextHelper {
    private static final String specialChars = "[!@#$%^&*()_\\-+={}\\[\\]:;\"'<>,.?/\\\\|`~]";

    /**
     * Split the text and get words also remove all special characters
     *
     * @param text input string
     * @return special char filtered out words
     */
    static String[] splitAndFilterText(String text) {
        String[] words = text.split(" ");

        return MyStream.of(Arrays.asList(words))
                .flatMap(TextHelper::splitBySpecialChar)
                .map(TextHelper::removeAsciiSpecialChar)
                .filter(s -> s.length() >= 3)
                .toList()
                .toArray(new String[0]);
    }

    static List<String> splitAndGetTrigrams(String word) {
        return MyStream.of(Collections.singletonList(word))
                .map(TextHelper::removeAsciiSpecialChar)
                .flatMap(TextHelper::generateTrigrams)
                .filter(s -> s.length() >= 3)
                .toList();
    }

    private static String[] splitBySpecialChar(String word) {
        Pattern regex = Pattern.compile(specialChars);
        return regex.split(word.replace("\n", " "));
    }

    private static String removeAsciiSpecialChar(String word) {
        Pattern regex = Pattern.compile(specialChars);
        Matcher matcher = regex.matcher(word.replace("\n", " "));

        return matcher.replaceAll("");
    }

    public static String[] generateTrigrams(String word) {
        List<String> trigrams = new ArrayList<>();
        for (int i = 0; i < word.length() - 2; i++) {
            trigrams.add(word.substring(i, i + 3));
        }
        return trigrams.toArray(new String[0]);
    }
}
