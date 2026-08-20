package elite.intel.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.util.SpokenNumbers;

/**
 * Reads the search radius out of an LLM parameter block.
 * <p>
 * Every "find X within Y light years" command reads {@code max_distance} through here, so a radius the
 * commander stated is either honoured by all of them or silently dropped by all of them. It used to require
 * bare digits, which meant a model echoing the commander's own words - "two hundred", as speech-to-text
 * wrote them - fell through to the default: the commander asked for 200 ly, searched 40, and was told the
 * thing he wanted does not exist. {@link SpokenNumbers} now reads the words as well as the digits.
 * <p>
 * A radius that cannot be read at all is still reported as the caller's default rather than as an error:
 * searching a sensible distance is a better answer than refusing the search.
 */
public class GetNumberFromParam {

    public static Number extractRangeParameter(JsonObject params, int defaultValue) {
        JsonElement element = params.get("max_distance");
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        Integer stated = SpokenNumbers.parse(element.getAsString());
        // A zero or negative radius searches nothing; the default is what the caller means by "unstated".
        return stated == null || stated < 1 ? defaultValue : stated;
    }
}
