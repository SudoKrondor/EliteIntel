package elite.intel.ai.brain.inference.lmstudio.install;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import elite.intel.util.OsDetector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The commander's own PATH on Windows, which LM Studio's first {@code lms daemon up} adds its {@code bin} folder to
 * even with {@code LMS_NO_MODIFY_PATH=1} (smoke test, round 3). Uninstalling takes that entry out again.
 * <p>
 * On Linux nothing is added - LM Studio leaves the shell profiles alone - so there is nothing to take out.
 */
interface UserPath {

    /**
     * Takes every entry naming {@code folder} out of the commander's PATH, leaving every other entry as it was.
     */
    void removeEntry(Path folder) throws InstallException;

    static UserPath forThisPc() {
        return OsDetector.getOs() == OsDetector.OS.WINDOWS ? new WindowsUserPath() : folder -> {
        };
    }

    /**
     * {@code pathValue} without the entries naming {@code folder}, compared as Windows does: ignoring case and a
     * trailing backslash. Everything else - order, empty entries, unexpanded {@code %VARIABLES%} - is kept exactly.
     */
    static String withoutEntry(String pathValue, Path folder) {
        String unwanted = normalised(folder.toString());
        List<String> kept = new ArrayList<>();
        for (String entry : pathValue.split(";", -1)) {
            if (!normalised(entry).equals(unwanted)) kept.add(entry);
        }
        return String.join(";", kept);
    }

    private static String normalised(String entry) {
        String trimmed = entry.strip();
        while (trimmed.endsWith("\\") || trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * The user's PATH as Windows keeps it: the {@code Path} value under {@code HKEY_CURRENT_USER\Environment}.
     * <p>
     * WHY written back with the type it had: a PATH stored as an expandable string holds entries like
     * {@code %USERPROFILE%\...}, and rewriting it as a plain string would leave those unexpanded and broken.
     */
    final class WindowsUserPath implements UserPath {

        private static final String KEY = "Environment";
        private static final String VALUE = "Path";

        @Override
        public void removeEntry(Path folder) throws InstallException {
            try {
                if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, KEY, VALUE)) return;
                boolean expandable = isExpandable();
                String current = expandable
                        ? Advapi32Util.registryGetExpandableStringValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE)
                        : Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE);
                String updated = withoutEntry(current, folder);
                if (updated.equals(current)) return;
                if (expandable) {
                    Advapi32Util.registrySetExpandableStringValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE, updated);
                } else {
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE, updated);
                }
            } catch (Win32Exception e) {
                throw new InstallException("Could not take " + folder + " out of your PATH: " + e.getMessage(), e);
            }
        }

        private static boolean isExpandable() {
            try {
                Advapi32Util.registryGetExpandableStringValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE);
                return true;
            } catch (RuntimeException plainString) {
                // WHY: JNA reads a value only as the type it is stored as, and says which it is not by refusing.
                return false;
            }
        }
    }
}
