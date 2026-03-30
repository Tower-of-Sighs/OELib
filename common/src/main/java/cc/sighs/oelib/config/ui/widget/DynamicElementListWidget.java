package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class DynamicElementListWidget {

    protected final Minecraft minecraft;
    protected final List<AbstractConfigEntry<?>> children = new ArrayList<>();
    public int left;
    public int width;
    public int top;
    public int bottom;

    public DynamicElementListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        this.minecraft = minecraft;
        this.left = 0;
        this.width = width;
        this.top = top;
        this.bottom = bottom;
    }

    public void setLeftPos(int left) {
        this.left = left;
    }

    public int getItemWidth() {
        return width - 24;
    }

    public List<AbstractConfigEntry<?>> children() {
        return children;
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int y = top;
        for (int i = 0; i < children.size(); i++) {
            AbstractConfigEntry<?> entry = children.get(i);
            int h = entry.getItemHeight();
            boolean hovered = mouseY >= y && mouseY < y + h && mouseX >= left && mouseX < left + width
                    && mouseY >= top && mouseY < bottom;
            entry.render(graphics, i, y, left + 12, getItemWidth(), h, mouseX, mouseY, hovered, delta);
            y += h;
        }
    }
}
