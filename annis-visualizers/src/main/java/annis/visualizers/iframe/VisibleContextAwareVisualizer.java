package annis.visualizers.iframe;

/**
 * Represents a visualizer that is able to show connections to/from outside the
 * currently visible context and hence needs to know the extend of the visible
 * range.
 *
 * @author Lari Lampen (lari.lampen@gmail.com)
 */
public interface VisibleContextAwareVisualizer {
    /**
     * Specify the extent of the visible context.
     *
     * @param minVisible token number of the earliest visible token
     * @param maxVisible token number of the latest visible token
     */
    public void setVisibleRange(int minVisible, int maxVisible);
}
