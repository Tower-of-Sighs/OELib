package cc.sighs.oelib.config.ui.entries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class CategoryTextEntry extends AbstractConfigEntry<Object> {
    private final Component category;

    public CategoryTextEntry(Component category, Component text) {
        this.category = category;
    }

    @Override
    public int getItemHeight() {
        return 24;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        int centerX = x + entryWidth / 2;
        Component title = category;
        graphics.fill(x, y + entryHeight - 1, x + entryWidth, y + entryHeight, 0x80FFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, title, centerX, y + 6, 0xFFFFFFFF);
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
