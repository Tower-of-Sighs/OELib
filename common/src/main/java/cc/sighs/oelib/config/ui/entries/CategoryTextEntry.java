package cc.sighs.oelib.config.ui.entries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * A non-interactive category header rendered as centered text.
 */
public class CategoryTextEntry extends AbstractConfigEntry<Object> {
    private final Component category;

    /**
     * Constructs a category text entry.
     *
     * @param category the category label
     * @param text     (unused) reserved for future use
     */
    public CategoryTextEntry(Component category, Component text) {
        this.category = category;
    }

    @Override
    public int getItemHeight() {
        return 24;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        int centerX = x + entryWidth / 2;
        graphics.centeredText(Minecraft.getInstance().font, category, centerX, y + 6, 0xFFFFFFFF);
    }

    @Override
    public Component getFieldName() {
        return category;
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
