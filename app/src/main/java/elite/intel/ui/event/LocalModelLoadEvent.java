package elite.intel.ui.event;

/**
 * How far the app's own LM Studio has got loading the model, for the progress bar in the app header.
 * {@link #finished()} is published once the load is over, whether it worked or not, so the bar never stays up.
 *
 * @param loading whether a load is running
 * @param percent how much of the model is loaded, 0 to 100; 0 when not loading
 */
public record LocalModelLoadEvent(boolean loading, int percent) {

    public static LocalModelLoadEvent progress(int percent) {
        return new LocalModelLoadEvent(true, Math.clamp(percent, 0, 100));
    }

    public static LocalModelLoadEvent finished() {
        return new LocalModelLoadEvent(false, 0);
    }
}
