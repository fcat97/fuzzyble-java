package media.uqab.fuzzybleJava;

import java.util.List;

/**
 * Helper class for fuzzy match
 *
 * @author github/fCat97
 */
class FuzzyUtils {

    /**
     * Filter words based on {@linkplain Similarity} index.
     * @param word searched word
     * @param words fuzzy matched words
     * @param similarity {@linkplain Similarity} to check match
     * @return filtered words
     */
    public static List<String> filterFuzzyMatched(final String word, List<String> words, Similarity similarity) {
        List<String> fuzzyWords = MyStream.of(words)
                .filter(fuzzyWord -> similarity.isSimilar(word, fuzzyWord))
                .toList();

        fuzzyWords.sort(similarity::similarityIndex);

        return fuzzyWords;
    }
}
