package elite.intel.ui.support;

/**
 * A bindings-grid cell that carries its own hover text beside what it shows.
 */
public interface TooltipCell {

    /**
     * The hover text, or {@code null} for none.
     */
    String tooltip();
}
