package cc.sighs.oelib.config.ui.scissor;

import java.util.List;

public interface ScissorsHandler {
    ScissorsHandler INSTANCE = new ScissorsHandlerImpl();

    void clearScissors();

    List<Rectangle> getScissorsAreas();

    void scissor(Rectangle rectangle);

    void removeLastScissor();

    void applyScissors();
}

