package cc.sighs.oelib.config.ui.entries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AbstractConfigEntry<T> implements GuiEventListener, NarratableEntry {
    protected final Minecraft mc = Minecraft.getInstance();
    @Nullable
    protected Consumer<T> saveCallback;

    public abstract int getItemHeight();

    public abstract void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta);

    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public abstract Component getFieldName();

    public abstract T getValue();

    public abstract Optional<T> getDefaultValue();

    public Optional<Component> getConfigError() {
        return Optional.empty();
    }

    public boolean isEdited() {
        return getConfigError().isPresent();
    }

    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    public void dispose() {
    }

    public void save() {
        if (saveCallback != null) {
            saveCallback.accept(getValue());
        }
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }
}
