package media.uqab.fuzzybleJava;

/**
 * Levenshtein Distance class.
 *
 * <p>
 * <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">See here</a>
 */
class Levenshtein implements Similarity {
    private final int thresholdDistance;

    public Levenshtein() {
        this(2);
    }

    public Levenshtein(int thresholdDistance) {
        this.thresholdDistance = thresholdDistance;
    }

    @Override
    public boolean isSimilar(String w1, String w2) {
        return distance(w1, w2) <= thresholdDistance;
    }

    @Override
    public int similarityIndex(String w1, String w2) {
        return -1 * distance(w1, w2);
    }

    private static String tail(String s) {
        return s.substring(1);
    }

    private static int distance(final String w1, final String w2) {
        if (w1.length() == 0) return w2.length();
        if (w2.length() == 0) return w1.length();

        if (w1.charAt(0) == w2.charAt(0)) {
            return distance(tail(w1), tail(w2));
        } else {
            int _1 = distance(tail(w1), w2);
            int _2 = distance(w1, tail(w2));
            int _3 = distance(tail(w1), tail(w2));
            return 1 + Math.min(_1, Math.min(_2, _3));
        }
    }
}
