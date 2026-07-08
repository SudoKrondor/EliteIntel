package elite.intel.diagnostics;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.AppPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Gate for the file-driven diagnostics harness. Diagnostics mode is ON when the input file
 * ({@code diagnostics/input.txt}) exists at startup — the tester creates it before launch and the app only
 * ever <b>reads</b> it. The app must NEVER create or recreate {@code input.txt}: if it did, the gate would
 * outlive the run and silently re-enable diagnostics on every later launch (the earlier bug). So the file's
 * presence reflects only the tester's intent, and the tester owns its lifecycle (create before launch, delete
 * after the run). Evaluated once at class load, so the whole run has a single, stable answer.
 * <p>
 * {@code language.txt} is a separate, optional data file (not the gate): it carries the command language to
 * apply at startup via {@link #applyBootLanguage()}.
 */
public final class DiagnosticsMode {

    /** UTF-8 BOM PowerShell's {@code Set-Content -Encoding utf8} prepends; stripped before parsing text files. */
    private static final String BOM = Character.toString(0xFEFF);

    private static final boolean ENABLED = detect();

    private DiagnosticsMode() {
    }

    private static boolean detect() {
        try {
            return Files.exists(AppPaths.getDiagnosticsInputFile());
        } catch (IOException e) {
            return false;
        }
    }

    /** Whether the file-driven diagnostics harness is active for this run. */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Applies the command language from {@code language.txt} at startup, before the companion (and its
     * {@code SemanticActionReducer}) are built. The reducer freezes the language at construction, so this MUST
     * run before services start — a later {@code @lang} switch would set the session language but never reach
     * the already-built reducer, leaving it matching the boot language's aliases. No-op if the file is absent
     * or invalid.
     */
    public static void applyBootLanguage() {
        try {
            Path file = AppPaths.getDiagnosticsLanguageFile();
            if (!Files.exists(file)) {
                return;
            }
            // Strip a leading UTF-8 BOM (PowerShell writes one) before parsing the enum.
            String code = Files.readString(file, StandardCharsets.UTF_8)
                    .replace(BOM, "").trim().toUpperCase(Locale.ROOT);
            if (code.isEmpty()) {
                return;
            }
            Language language = Language.valueOf(code);
            SystemSession.getInstance().setLanguage(language);
            DiagnosticsLog.write("DIAG boot-language=" + language);
        } catch (Exception e) {
            DiagnosticsLog.write("DIAG boot-language error: " + e.getMessage());
        }
    }
}
