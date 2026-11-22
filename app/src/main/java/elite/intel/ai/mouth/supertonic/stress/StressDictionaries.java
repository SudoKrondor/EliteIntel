package elite.intel.ai.mouth.supertonic.stress;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * RUAccent's word lists, all keyed by the lower-case word.
 *
 * @param accents      the stressed form of a word with one possible stress ({@code аба} -> {@code аб+а})
 * @param homographs   every stressed form of a word whose stress depends on the sentence
 * @param yoWords      the ё spelling of a word that always has one ({@code еще} -> {@code ещё})
 * @param yoHomographs the ё spelling of a word that has one only in some sentences ({@code все} -> {@code всё})
 */
record StressDictionaries(Map<String, String> accents,
                          Map<String, List<String>> homographs,
                          Map<String, String> yoWords,
                          Map<String, String> yoHomographs) {

    private static final Type WORD_TO_WORD = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type WORD_TO_WORDS = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    static StressDictionaries load(Path dictionaryDir) throws IOException {
        Map<String, String> accents = new HashMap<>(read(dictionaryDir.resolve("accents_nn.json.gz"), WORD_TO_WORD));
        // WHY: RUAccent adds these by hand on load - the preposition "о" is stressed, and "коса" (braid / scythe)
        // is missing from its homograph list.
        accents.put("о", "+о");
        Map<String, List<String>> homographs = new HashMap<>(read(dictionaryDir.resolve("omographs.json.gz"), WORD_TO_WORDS));
        homographs.put("коса", List.of("к+оса", "кос+а"));
        return new StressDictionaries(
                Map.copyOf(accents),
                Map.copyOf(homographs),
                Map.copyOf(read(dictionaryDir.resolve("yo_words.json.gz"), WORD_TO_WORD)),
                Map.copyOf(read(dictionaryDir.resolve("yo_homographs.json.gz"), WORD_TO_WORD)));
    }

    private static <T> T read(Path file, Type type) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            T value = new Gson().fromJson(reader, type);
            if (value == null) throw new IOException("Empty stress dictionary: " + file);
            return value;
        }
    }
}
