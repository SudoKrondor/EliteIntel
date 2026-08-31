package elite.intel.ui.event;

/**
 * Published on the UI bus when the rows of the playlist change - tracks added or removed, or a batch of
 * them just gained the tags the table shows.
 * <p>
 * Sent once per batch rather than once per track: a library of ten thousand files would otherwise put ten
 * thousand repaints through the event thread to fill in a column.
 */
public class JukeboxPlaylistChangedEvent {
}
