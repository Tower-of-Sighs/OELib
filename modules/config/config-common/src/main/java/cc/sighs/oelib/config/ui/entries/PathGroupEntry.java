package cc.sighs.oelib.config.ui.entries;

import com.flechazo.hkt.Maybe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;


/**
 * A collapsible group header for nested configuration paths.
 *
 * <p>When a configuration contains dotted-path fields (for example
 * {@code "db.host"}, {@code "db.port"}), a {@code PathGroupEntry} is
 * inserted before the first field of each path segment. The user can
 * click the arrow to collapse or expand all fields under that group.
 */
public class PathGroupEntry extends AbstractConfigEntry<Object> {
    private final String stateKey;
    private final String groupPath;
    private final Component label;
    private final int level;
    private boolean expanded;
    private int arrowX;
    private int arrowY;
    private int arrowW;
    private int arrowH;

    /**
     * Constructs a group header.
     *
     * @param stateKey  a unique key for tracking expand/collapse state
     * @param groupPath the dotted path prefix this group represents
     * @param label     the display label
     * @param level     the nesting depth (0 = root)
     * @param expanded  the initial expanded state
     */
    public PathGroupEntry(String stateKey, String groupPath, Component label, int level, boolean expanded) {
        this.stateKey = stateKey;
        this.groupPath = groupPath;
        this.label = label;
        this.level = level;
        this.expanded = expanded;
    }

    /**
     * Returns the unique state key for this group.
     *
     * @return the state key
     */
    public String stateKey() {
        return stateKey;
    }

    /**
     * Returns the dotted path prefix this group represents.
     *
     * @return the group path
     */
    public String groupPath() {
        return groupPath;
    }

    /**
     * Returns {@code true} if this group is currently expanded.
     *
     * @return {@code true} if expanded
     */
    public boolean expanded() {
        return expanded;
    }

    /**
     * Sets the expanded state.
     *
     * @param expanded {@code true} to expand, {@code false} to collapse
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Returns the nesting level of this group.
     *
     * @return the level (0 = top-level)
     */
    public int level() {
        return level;
    }

    /**
     * Toggles the expanded state if the given coordinates hit the arrow icon.
     *
     * @param mouseX the mouse x
     * @param mouseY the mouse y
     * @return {@code true} if the toggle was triggered
     */
    public boolean toggleIfHit(double mouseX, double mouseY) {
        if (mouseX >= arrowX && mouseX <= arrowX + arrowW && mouseY >= arrowY && mouseY <= arrowY + arrowH) {
            expanded = !expanded;
            return true;
        }
        return false;
    }

    @Override
    public int getItemHeight() {
        return 22;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        var font = Minecraft.getInstance().font;
        int indent = level * 14;
        arrowX = x + 6 + indent;
        arrowY = y + 3;
        arrowW = 12;
        arrowH = 16;

        graphics.drawString(font, expanded ? "▾" : "▸", arrowX + 2, arrowY + 4, 0xFFD0D0D0);
        graphics.drawString(font, label, x + 22 + indent, y + 6, 0xFFD0D0D0);
    }

    @Override
    public Component getFieldName() {
        return label;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Maybe<Object> getDefaultValue() {
        return Maybe.none();
    }
}
