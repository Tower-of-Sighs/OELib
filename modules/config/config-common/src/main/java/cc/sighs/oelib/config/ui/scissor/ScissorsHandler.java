package cc.sighs.oelib.config.ui.scissor;

import java.util.List;

/**
 * Manages a stack of scissor regions for OpenGL clipping in the UI.
 *
 * <p>Adapted from Cloth Config's scissor system.
 */
public interface ScissorsHandler {
    /** The global scissor handler instance. */
    ScissorsHandler INSTANCE = new ScissorsHandlerImpl();

    /** Clears all active scissor regions. */
    void clearScissors();

    /**
     * Returns an unmodifiable view of the current scissor area stack.
     *
     * @return the scissor areas
     */
    List<Rectangle> getScissorsAreas();

    /**
     * Pushes a scissor region onto the stack and applies the intersection
     * of all active regions.
     *
     * @param rectangle the region to add
     */
    void scissor(Rectangle rectangle);

    /** Removes the most recently pushed scissor region. */
    void removeLastScissor();

    /** Applies the intersection of all active scissor regions to OpenGL. */
    void applyScissors();
}
