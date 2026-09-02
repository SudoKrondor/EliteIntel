package elite.intel.jukebox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which files the jukebox will play, and how each one is opened.
 * <p>
 * <b>Why both questions live in one class.</b> They are the same question asked at two moments. The
 * folder scanner and the playlist importer ask it when a library is added; the player asks it when a
 * track's turn comes, hours later. If the two ever disagreed - a scanner that accepts an extension the
 * player cannot open - the commander would watch tracks be imported and then silently marked missing on
 * playback, which looks like a corrupt library rather than an unsupported format. Keeping the extension
 * set and the opener in one map makes that disagreement impossible to write: adding a format is one
 * entry, and it is answered consistently everywhere by construction.
 */
final class AudioSources {

    private static final Map<String, JukeboxPlayer.SourceFactory> BY_EXTENSION = new LinkedHashMap<>();

    static {
        BY_EXTENSION.put(".mp3", Mp3AudioSource::open);
        BY_EXTENSION.put(".flac", FlacAudioSource::open);
        BY_EXTENSION.put(".m4a", AacAudioSource::open);
        // The same AAC-in-MP4 file under the extension an audiobook is published with. Audiobooks are
        // why the scanner sorts by path and why a resume position is stored at all, so the format they
        // actually arrive in belongs here.
        BY_EXTENSION.put(".m4b", AacAudioSource::open);
        BY_EXTENSION.put(".ogg", VorbisAudioSource::open);
        // The extension Xiph actually specifies for Ogg audio. Rarer than .ogg in the wild, but a
        // library tagged by a strict tool will use it, and it is the same file either way.
        BY_EXTENSION.put(".oga", VorbisAudioSource::open);
    }

    private AudioSources() {
    }

    /**
     * The extensions a library scan collects, lower case and dot-prefixed.
     */
    static Set<String> extensions() {
        return BY_EXTENSION.keySet();
    }

    /**
     * Whether this file is one the jukebox can play, judged by extension alone.
     * <p>
     * By extension rather than by opening it: a library scan crosses thousands of files and a music
     * folder holds artwork, playlists and sleeve notes among them. Opening each one to find out would
     * turn adding a library into a long wait to reach the same answer.
     */
    static boolean isPlayable(Path file) {
        Path name = file == null ? null : file.getFileName();
        if (name == null) return false;
        String lower = name.toString().toLowerCase(Locale.ROOT);
        for (String extension : BY_EXTENSION.keySet()) {
            if (lower.endsWith(extension)) return true;
        }
        return false;
    }

    /**
     * Opens {@code file} positioned at {@code startMs}, matching {@link JukeboxPlayer.SourceFactory}.
     *
     * @throws IOException when the format is not one we play, or the file will not decode
     */
    static AudioSource open(Path file, long startMs) throws IOException {
        Path name = file == null ? null : file.getFileName();
        if (name != null) {
            String lower = name.toString().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, JukeboxPlayer.SourceFactory> candidate : BY_EXTENSION.entrySet()) {
                if (lower.endsWith(candidate.getKey())) {
                    return candidate.getValue().open(file, startMs);
                }
            }
        }
        throw new IOException("Not a format the jukebox plays: " + file);
    }
}
