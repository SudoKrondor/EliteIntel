package elite.intel.ui.event;

/**
 * UI-layer signal that a bookmark was saved or deleted by voice, so the Bookmarks page on the Commander tab
 * re-reads the list. Carries nothing: the page reads the bookmarks back from the manager, off the EDT.
 */
public class LocationBookmarksChangedEvent {
}
