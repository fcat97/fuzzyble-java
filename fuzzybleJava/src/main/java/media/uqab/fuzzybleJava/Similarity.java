package media.uqab.fuzzybleJava;

/**
 * Class represents how the {@linkplain Fuzzyble} will check similarity between two words.
 */
public interface Similarity {
    /**
     * Are these two words similar or not
     * @param w1 First word
     * @param w2 Second word
     * @return true if similar.
     */
    boolean isSimilar(String w1, String w2);

    /**
     * Method to check how similar these two words are
     * @param w1 First word
     * @param w2 Second word
     * @return similarity between these two words. Larger value indicates more similar.
     */
    int similarityIndex(String w1, String w2);
}
