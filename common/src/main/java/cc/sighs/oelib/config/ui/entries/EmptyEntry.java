package cc.sighs.oelib.config.ui.entries;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class EmptyEntry extends AbstractConfigEntry<Object> {
    private final int height;

    public EmptyEntry(int height) {
        this.height = height;
    }

    @Override
    public int getItemHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
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
