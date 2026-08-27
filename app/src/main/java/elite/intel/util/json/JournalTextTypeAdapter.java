package elite.intel.util.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Decodes the one HTML entity Frontier leaks into journal text.
 * <p>
 * Localised names occasionally arrive with a literal {@code &NBSP;} in them, for example
 * {@code "NearestDestination_Localised":"Crash Site [Threat&NBSP;1]"}. Left alone it survives all the
 * way to the commander: the prompt builder correctly escapes the ampersand on its way into the XML
 * payload, the model copies the string it was given back into its answer, and the companion says
 * "Crash Site, Threat ampersand N B S P one".
 * <p>
 * WHY this sits on the shared Gson rather than at a call site: every journal event and auxiliary file
 * DTO binds its {@code _Localised} fields through this instance, so one adapter covers all 43 of them
 * and anything added later. Reads only - what we write out is our own text, never Frontier's.
 */
public final class JournalTextTypeAdapter extends TypeAdapter<String> {

    private static final Pattern NON_BREAKING_SPACE = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE);

    @Override
    public void write(JsonWriter out, String value) throws IOException {
        out.value(value);
    }

    @Override
    public String read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return decode(in.nextString());
    }

    /**
     * The text as a human should read it. Visible for tests, and cheap on the journal's hot path:
     * a string with no ampersand in it is handed straight back.
     */
    public static String decode(String text) {
        if (text == null || text.indexOf('&') < 0) return text;
        return NON_BREAKING_SPACE.matcher(text).replaceAll(" ");
    }
}
