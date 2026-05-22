package cc.sighs.oelib.config.ui.entries;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * A thin horizontal line used as a visual separator between logical
 * blocks of configuration fields.
 */
public class DividerEntry extends AbstractConfigEntry<Object> {
    private final int height;
    private final int color;
    private final int inset;

    /**
     * Constructs a divider with default dimensions and color.
     */
    public DividerEntry() {
        this(8, 0x50FFFFFF, 10);
    }

    /**
     * Constructs a divider with custom dimensions.
     *
     * @param height the total height of the divider row
     * @param color  the ARGB color of the line
     * @param inset  the horizontal inset from each side
     */
    public DividerEntry(int height, int color, int inset) {
        this.height = height;
        this.color = color;
        this.inset = inset;
    }

    @Override
    public int getItemHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        int lineY = y + Math.max(1, entryHeight / 2);
        graphics.fill(x + inset, lineY, x + entryWidth - inset, lineY + 1, color);
    }

    @Override
    public Component getFieldName() {
        return Component.empty();
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }
}
