package elite.intel.ai.mouth.supertonic.stress;

import ai.onnxruntime.OrtException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A Java port of RUAccent's "tiny" mode (v1.5.8.3, the {@code tiny2.1} homograph model): puts the stress on
 * the words of one Russian sentence, and the ё back where the writer left it out.
 * <p>
 * The steps run in RUAccent's order on RUAccent's words: ё first, then homographs - the words whose stress
 * changes with the meaning ({@code з+амок} castle / {@code зам+ок} lock), judged against the whole sentence -
 * and last the stress of every other word, from the dictionary or, for a word it lacks, the character model.
 * <p>
 * Where this differs from RUAccent, on purpose:
 * <ul>
 *   <li>It only adds marks and ё. RUAccent deletes characters it does not know and reflows the spacing; the
 *       commander would hear that.</li>
 *   <li>A homograph is looked up in lower case and keeps its capitals. RUAccent looks the word up as written,
 *       and every key is lower case, so a sentence that starts with one ("Замок на двери сломан") skipped the
 *       homograph step.</li>
 *   <li>The ё model's words are matched to ours by offset, not by position in the list (see
 *       {@link YoHomographModel#yoWordStarts}).</li>
 * </ul>
 * {@link #accentuate} may be called from any thread: the ONNX sessions and the tokenizers are safe to share.
 */
final class Accentuator implements AutoCloseable {

    private static final String ASCII_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private final StressDictionaries dictionaries;
    private final YoHomographModel yoModel;
    private final HomographModel homographModel;
    private final CharAccentModel accentModel;

    Accentuator(Path modelDir) throws IOException, OrtException {
        this.dictionaries = StressDictionaries.load(modelDir.resolve("dictionary"));
        this.yoModel = new YoHomographModel(modelDir.resolve("nn/nn_yo_homograph_resolver"));
        this.homographModel = new HomographModel(modelDir.resolve("nn/nn_omograph/tiny2.1"));
        this.accentModel = new CharAccentModel(modelDir.resolve("nn/nn_accent"));
    }

    /**
     * The sentence with an acute after each stressed vowel and ё restored; everything between words untouched.
     */
    String accentuate(String sentence) throws OrtException {
        SentenceWords sentenceWords = SentenceWords.of(sentence);
        if (sentenceWords.spans().isEmpty()) return sentence;
        List<String> words = restoreYo(sentenceWords);
        words = resolveHomographs(words, sentenceWords.contextWords(words));
        words = stressTheRest(words);
        // WHY per word: the + notation is the port's own; a + in the commander's text is not a stress mark.
        return sentenceWords.rebuild(words.stream().map(StressMarks::fromPlusNotation).toList());
    }

    private List<String> restoreYo(SentenceWords sentenceWords) throws OrtException {
        List<String> words = new ArrayList<>(sentenceWords.words());
        String lowerSentence = sentenceWords.lowerCaseContext();
        // WHY: RUAccent asks the model only when there is an е it could mean as ё.
        Set<Integer> yoStarts = lowerSentence.indexOf('е') >= 0 ? yoModel.yoWordStarts(lowerSentence) : Set.of();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            String lower = SentenceWords.lowerCaseKeepingLength(word);
            String spelled = dictionaries.yoWords().getOrDefault(lower, lower);
            if (yoStarts.contains(sentenceWords.spans().get(i).start())) {
                spelled = dictionaries.yoHomographs().getOrDefault(lower, spelled);
            }
            if (!spelled.equals(lower)) words.set(i, sameLength(word, spelled));
        }
        return words;
    }

    private List<String> resolveHomographs(List<String> words, List<String> contextWords) throws OrtException {
        List<String> resolved = new ArrayList<>(words);
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            List<String> readings = dictionaries.homographs().get(SentenceWords.lowerCaseKeepingLength(word));
            if (readings == null) continue;
            String chosen = homographModel.choose(SentenceWords.homographContext(contextWords, i), readings);
            resolved.set(i, StressMarks.withCaseOf(word, chosen));
        }
        return resolved;
    }

    private List<String> stressTheRest(List<String> words) throws OrtException {
        List<String> stressed = new ArrayList<>(words.size());
        for (String word : words) stressed.add(word.indexOf('+') >= 0 ? word : stress(word));
        return stressed;
    }

    private String stress(String word) throws OrtException {
        String lower = SentenceWords.lowerCaseKeepingLength(word);
        String known = dictionaries.accents().get(lower);
        if (known != null && known.indexOf('+') >= 0) return StressMarks.withCaseOf(word, known);
        if (hasPunctuation(lower) || StressMarks.countVowels(lower) < 2) return word;
        return accentModel.putAccent(word);
    }

    /**
     * {@code spelled} in {@code word}'s capitals.
     * <p>
     * WHY: a ё spelling is the same word with one letter changed. One that is longer or shorter is a dictionary
     * entry that is not that, so the word stays as written rather than turning into another one.
     */
    private static String sameLength(String word, String spelled) {
        return spelled.length() == word.length() ? StressMarks.withCaseOf(word, spelled) : word;
    }

    private static boolean hasPunctuation(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (ASCII_PUNCTUATION.indexOf(word.charAt(i)) >= 0) return true;
        }
        return false;
    }

    @Override
    public void close() throws OrtException {
        accentModel.close();
        homographModel.close();
        yoModel.close();
    }
}
