package elite.intel.gameapi.journal.events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the hand-written copy constructor. Gson has already produced a fully populated instance by the
 * time the constructor runs; the field-by-field copy that follows exists only so {@code super(...)} can
 * run first. A field added to the class but not to that list is not a compile error and not a warning:
 * it silently reads as null/0 at runtime. That is how StarType went missing, which made every star look
 * like an unclassified body and pushed classification onto a surface-temperature heuristic.
 */
class ScanEventTest {

    /**
     * Every {@code @SerializedName} field on ScanEvent, with a distinct non-default value so a dropped
     * copy shows up as a mismatch rather than coinciding with the field's zero value.
     */
    private static final String FULLY_POPULATED_SCAN = """
            {"timestamp":"2026-07-20T01:46:27Z","event":"Scan","ScanType":"Detailed",
             "StarPos":[12.5,-34.25,56.75],"BodyName":"Stuelou AF-P d6-996 A 2","BodyID":4,
             "Parents":[{"Star":1},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","StarType":"A",
             "SystemAddress":34231400926907,"DistanceFromArrivalLS":419.356817,"TidalLock":true,
             "TerraformState":"Terraformable","PlanetClass":"High metal content body",
             "Atmosphere":"thin sulphur dioxide atmosphere","AtmosphereType":"SulphurDioxide",
             "Volcanism":"major silicate vapour geysers volcanism","MassEM":1.25,"Radius":6183530.5,
             "SurfaceGravity":9.81,"SurfaceTemperature":1518.012329,"SurfacePressure":101325.0,
             "Landable":true,"Materials":[{"Name":"iron","Percent":22.5}],
             "Composition":{"Ice":0.1,"Rock":0.6,"Metal":0.3},"SemiMajorAxis":1.2345e11,
             "Eccentricity":0.0071,"OrbitalInclination":3.5,"Periapsis":128.9,"OrbitalPeriod":9.87e6,
             "AscendingNode":-45.6,"MeanAnomaly":77.7,"RotationPeriod":83000.5,"AxialTilt":0.42,
             "WasDiscovered":true,"WasMapped":true}
            """;

    @Test
    void theCopyConstructorCarriesEveryDeclaredField() {
        JsonObject json = JsonParser.parseString(FULLY_POPULATED_SCAN).getAsJsonObject();
        Gson gson = GsonFactory.getGson();

        ScanEvent copied = new ScanEvent(json);
        ScanEvent parsedDirectly = gson.fromJson(json, ScanEvent.class);

        for (Field field : copyableFields()) {
            field.setAccessible(true);
            // Serialize rather than compare directly: the nested Parent/Material/Composition types
            // define no equals(), so distinct instances holding identical data would never be equal.
            String expected = gson.toJson(valueOf(field, parsedDirectly));
            String actual = gson.toJson(valueOf(field, copied));
            assertEquals(expected, actual, "copy constructor dropped field: " + field.getName());
        }
    }

    /**
     * Without this, adding a field to ScanEvent and forgetting it in both the fixture and the copy
     * constructor would leave the parity test comparing null to null and passing.
     */
    @Test
    void theFixtureExercisesEveryDeclaredField() {
        JsonObject json = JsonParser.parseString(FULLY_POPULATED_SCAN).getAsJsonObject();

        for (Field field : copyableFields()) {
            SerializedName name = field.getAnnotation(SerializedName.class);
            assertTrue(name != null, "field is not journal-mapped, so parity cannot be checked: " + field.getName());
            assertTrue(json.has(name.value()),
                    "fixture is missing a value for " + field.getName() + " (\"" + name.value() + "\")");
        }
    }

    private static List<Field> copyableFields() {
        List<Field> fields = new ArrayList<>();
        // Declared fields only: BaseEvent's are set by super(...), not by the copy block under test.
        for (Field field : ScanEvent.class.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) continue;
            fields.add(field);
        }
        return fields;
    }

    private static Object valueOf(Field field, ScanEvent event) {
        try {
            return field.get(event);
        } catch (IllegalAccessException e) {
            throw new AssertionError("could not read field " + field.getName(), e);
        }
    }
}
