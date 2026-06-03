package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A non-scrolling list widget that renders a flat sequence of
 * {@link AbstractConfigEntry} instances.
 *
 * <p>This is the base class for {@link DynamicEntryListWidget}, which
 * adds smooth scrolling on top.
 */
public class DynamicElementListWidget {

    protected final Minecraft minecraft;
    protected final List<AbstractConfigEntry<?>> children = new ArrayList<>();
    public int left;
    public int width;
    public int top;
    public int bottom;

    /**
     * Constructs a list widget.
     *
     * @param minecraft the Minecraft instance
     * @param width     the total width
     * @param height    the total height
     * @param top       the top y coordinate of the visible area
     * @param bottom    the bottom y coordinate of the visible area
     */
    public DynamicElementListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        this.minecraft = minecraft;
        this.left = 0;
        this.width = width;
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * Sets the left position of this widget.
     *
     * @param left the left x coordinate
     */
    public void setLeftPos(int left) {
        this.left = left;
    }

    /**
     * Returns the usable width for items.
     *
     * @return the item width
     */
    public int getItemWidth() {
        return width - 24;
    }

    /**
     * Returns the list of child entries.
     *
     * @return the children
     */
    public List<AbstractConfigEntry<?>> children() {
        return children;
    }

    /**
     * Renders all visible children.
     *
     * @param graphics the graphics context
     * @param mouseX   the current mouse x
     * @param mouseY   the current mouse y
     * @param delta    the partial tick delta
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
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
