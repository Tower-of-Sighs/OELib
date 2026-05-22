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

/**
 * Common base for entries in the auto-generated configuration screen.
 *
 * <p>Each entry represents one logical row: a field, a category header,
 * a divider, or a group toggle. Subclasses implement
 * {@link #render(GuiGraphics, int, int, int, int, int, int, int, boolean, float)}
 * to draw themselves and {@link #getItemHeight()} to declare their height.
 *
 * @param <T> the value type associated with this entry
 */
public abstract class AbstractConfigEntry<T> implements GuiEventListener, NarratableEntry {
    protected final Minecraft mc = Minecraft.getInstance();
    @Nullable
    protected Consumer<T> saveCallback;

    /**
     * Returns the height of this entry in pixels.
     *
     * @return the item height
     */
    public abstract int getItemHeight();

    /**
     * Renders this entry at the given position.
     *
     * @param graphics    the graphics context
     * @param index       the index of this entry in the list
     * @param y           the y coordinate of the top of this entry
     * @param x           the x coordinate of the left of this entry
     * @param entryWidth  the available width for this entry
     * @param entryHeight the height of this entry
     * @param mouseX      the current mouse x
     * @param mouseY      the current mouse y
     * @param isHovered   {@code true} if the mouse is over this entry
     * @param delta       the partial tick delta
     */
    public abstract void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta);

    /**
     * Renders overlay elements for this entry, called after all entries
     * have been rendered.
     *
     * @param graphics the graphics context
     * @param mouseX   the current mouse x
     * @param mouseY   the current mouse y
     * @param delta    the partial tick delta
     */
    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    /**
     * Returns the display name for this entry.
     *
     * @return the field name component
     */
    public abstract Component getFieldName();

    /**
     * Returns the current value of this entry.
     *
     * @return the value
     */
    public abstract T getValue();

    /**
     * Returns the default value of this entry.
     *
     * @return the default value, or {@link Optional#empty()}
     */
    public abstract Optional<T> getDefaultValue();

    /**
     * Returns a configuration error message, if any.
     *
     * @return the error message, or {@link Optional#empty()}
     */
    public Optional<Component> getConfigError() {
        return Optional.empty();
    }

    /**
     * Returns {@code true} if this entry has been edited from its original
     * state or has a configuration error.
     *
     * @return {@code true} if edited
     */
    public boolean isEdited() {
        return getConfigError().isPresent();
    }

    /**
     * Returns child GUI event listeners for this entry.
     *
     * @return the children, or an empty list
     */
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    /**
     * Returns child narratable entries.
     *
     * @return the narratables, or an empty list
     */
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    /**
     * Persists the current value via the registered save callback.
     */
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
