package cc.sighs.oelib.config.ui.scissor;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScissorsHandlerImpl implements ScissorsHandler {
    private final List<Rectangle> scissorsAreas = new ArrayList<>();

    @Override
    public void clearScissors() {
        scissorsAreas.clear();
        applyScissors();
    }

    @Override
    public List<Rectangle> getScissorsAreas() {
        return Collections.unmodifiableList(scissorsAreas);
    }

    @Override
    public void scissor(Rectangle rectangle) {
        scissorsAreas.add(new Rectangle(rectangle));
        applyScissors();
    }

    @Override
    public void removeLastScissor() {
        if (!scissorsAreas.isEmpty()) {
            scissorsAreas.remove(scissorsAreas.size() - 1);
        }
        applyScissors();
    }

    @Override
    public void applyScissors() {
        if (!scissorsAreas.isEmpty()) {
            var r = scissorsAreas.get(0).clone();
            for (int i = 1; i < scissorsAreas.size(); i++) {
                var other = scissorsAreas.get(i);
                if (r.intersects(other)) {
                    r.setBounds(r.intersection(other));
                } else {
                    _applyScissor(null);
                    return;
                }
            }
            r.setBounds(
                    Math.min(r.x, r.x + r.width),
                    Math.min(r.y, r.y + r.height),
                    Math.abs(r.width),
                    Math.abs(r.height)
            );
            _applyScissor(r);
        } else {
            _applyScissor(null);
        }
    }

    private void _applyScissor(Rectangle r) {
        if (r != null) {
            GlStateManager._enableScissorTest();
            if (r.isEmpty()) {
                GlStateManager._scissorBox(0, 0, 0, 0);
            } else {
                var window = Minecraft.getInstance().getWindow();
                double scaleFactor = window.getGuiScale();
                int x = (int) (r.x * scaleFactor);
                int y = (int) ((window.getGuiScaledHeight() - r.height - r.y) * scaleFactor);
                int w = (int) (r.width * scaleFactor);
                int h = (int) (r.height * scaleFactor);
                GlStateManager._scissorBox(x, y, w, h);
            }
        } else {
            GlStateManager._disableScissorTest();
        }
    }
}
