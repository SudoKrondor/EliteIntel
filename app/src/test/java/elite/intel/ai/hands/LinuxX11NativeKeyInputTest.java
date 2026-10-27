package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinuxX11NativeKeyInputTest {

    /**
     * A key left to Robot goes out over AWT's own X connection, and KWin asks permission once per connection:
     * every plain key the app can press must be sent over the native one.
     */
    @Test
    void everyPlainKeyIsSentOverTheNativeConnection() throws IllegalAccessException {
        List<String> unmapped = new ArrayList<>();
        for (Field field : KeyProcessor.class.getFields()) {
            if (!field.getName().startsWith("KEY_") || field.getType() != int.class
                    || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            int keyCode = field.getInt(null);
            if (keyCode < KeyProcessor.NATIVE_BASE && !LinuxX11NativeKeyInput.mapsVirtualKey(keyCode)) {
                unmapped.add(field.getName());
            }
        }
        assertEquals(List.of(), unmapped);
    }
}
