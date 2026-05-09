package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A collapsible entry for editing a JSON array field.
 *
 * <p>When collapsed, only the field label and an expand arrow are visible.
 * When expanded, each element is rendered as an editable text box (or
 * checkbox for booleans), with up/down/delete buttons for reordering and
 * removal, and an add button to append new elements.
 */
public class ListEntry extends AbstractConfigEntry<JsonArray> {
    private final String keyPath;
    private final JsonObject working;
    private final int controlWidth;
    private final int rowHeight;
    private final Component label;
    private final Component tooltip;
    private final JsonArray array;
    private final List<EditBox> valueBoxes = new ArrayList<>();
    private final List<Checkbox> toggleBoxes = new ArrayList<>();
    private final List<Button> delButtons = new ArrayList<>();
    private final List<Button> upButtons = new ArrayList<>();
    private final List<Button> downButtons = new ArrayList<>();
    private boolean created = false;
    private Screen screen;
    private Button addBtn;
    private Button resetButton;
    private JsonArray defaultArray;
    private boolean expanded = false;
    private int expX, expY, expW, expH;

    /**
     * Constructs a list entry.
     *
     * @param keyPath      the dotted path to the array field in the working JSON
     * @param label        the display label
     * @param working      the working JSON object
     * @param controlWidth the width of the edit controls
     * @param rowHeight    the row height
     * @param tooltip      the tooltip component, or {@code null}
     */
    public ListEntry(String keyPath, Component label, JsonObject working, int controlWidth, int rowHeight, Component tooltip) {
        this.keyPath = keyPath;
        this.label = label;
        this.tooltip = tooltip;
        this.working = working;
        this.controlWidth = controlWidth;
        this.rowHeight = rowHeight;
        var value = ConfigGuiUtil.getPath(working, keyPath);
        this.array = value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    @Override
    public int getItemHeight() {
        return expanded ? rowHeight * Math.max(1, array.size() + 1) : rowHeight;
    }

    /**
     * Attaches this entry to a screen, creating widgets.
     *
     * @param screen   the parent screen
     * @param x        the x position
     * @param y        the y position
     * @param defaults the default values JSON object
     */
    public void attach(Screen screen, int x, int y, JsonObject defaults) {
        if (created) return;
        this.screen = screen;
        int resetX = screen.width - 80;

        var def = ConfigGuiUtil.getPath(defaults, keyPath);
        defaultArray = def != null && def.isJsonArray() ? def.getAsJsonArray() : new JsonArray();

        addBtn = Button.builder(Component.literal("+"), b -> {
            array.add(new JsonPrimitive(""));
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(x, y, resetX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(resetX - 24, y, 22, 20).build();
        screen.addRenderableWidget(addBtn);

        resetButton = Button.builder(Component.translatable("config.oelib.reset"), b -> {
            while (!array.isEmpty()) {
                array.remove(array.size() - 1);
            }
            for (JsonElement el : defaultArray) array.add(el);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(x, y, resetX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(resetX, y, 72, 20).build();
        screen.addRenderableWidget(resetButton);

        rebuildRows(x, y, resetX);
        updateResetButtonState();
        created = true;
    }

    private void rebuildRows(int x, int y, int resetX) {
        if (screen == null) return;

        clearWidgets();
        valueBoxes.clear();
        toggleBoxes.clear();
        delButtons.clear();
        upButtons.clear();
        downButtons.clear();

        int rowY = y + rowHeight;
        int vx = x + 40;
        int vw = Math.max(60, resetX - vx - 86);

        for (int i = 0; i < array.size(); i++) {
            var el = array.get(i);
            var vb = ConfigGuiUtil.createEditBox(ConfigGuiUtil.jsonToString(el), vx, rowY, vw);
            screen.addRenderableWidget(vb);

            Checkbox cb = null;
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                cb = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                        .selected(el.getAsBoolean())
                        .onValueChange((box, selected) -> {
                            int idx = valueBoxes.indexOf(vb);
                            if (idx >= 0) {
                                array.set(idx, new JsonPrimitive(selected));
                                ConfigGuiUtil.setPath(working, keyPath, array);
                                updateResetButtonState();
                                if (screen instanceof ConfigScreen cs) cs.markDirty();
                            }
                        }).build();
                screen.addRenderableWidget(cb);
            }

            int deleteX = resetX - 21;
            int downX = deleteX - 22;
            int upX = downX - 22;

            var up = Button.builder(Component.literal("▲"), b -> moveUp(vb)).bounds(upX, rowY, 20, 20).build();
            var down = Button.builder(Component.literal("▼"), b -> moveDown(vb)).bounds(downX, rowY, 20, 20).build();
            var del = Button.builder(Component.literal("x"), b -> removeItem(vb)).bounds(deleteX, rowY, 20, 20).build();

            screen.addRenderableWidget(up);
            screen.addRenderableWidget(down);
            screen.addRenderableWidget(del);

            vb.setResponder(str -> {
                int idx = valueBoxes.indexOf(vb);
                if (idx >= 0) {
                    var prev = ConfigGuiUtil.jsonToString(array.get(idx));
                    if (!prev.equals(str)) {
                        array.set(idx, ConfigGuiUtil.parsePrimitive(str));
                        ConfigGuiUtil.setPath(working, keyPath, array);
                        updateResetButtonState();
                        if (screen instanceof ConfigScreen cs) cs.markDirty();
                    }
                }
            });

            valueBoxes.add(vb);
            toggleBoxes.add(cb);
            delButtons.add(del);
            upButtons.add(up);
            downButtons.add(down);
            rowY += rowHeight;
        }
    }

    private void moveUp(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx > 0 && idx < array.size()) {
            var cur = array.get(idx);
            var prev = array.get(idx - 1);
            array.set(idx - 1, cur);
            array.set(idx, prev);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void moveDown(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx >= 0 && idx < array.size() - 1) {
            var cur = array.get(idx);
            var next = array.get(idx + 1);
            array.set(idx + 1, cur);
            array.set(idx, next);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void removeItem(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx >= 0 && idx < array.size()) {
            array.remove(idx);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void clearWidgets() {
        for (var list : List.of(valueBoxes, delButtons, upButtons, downButtons)) {
            for (var w : list) {
                if (w instanceof EditBox eb) {
                    eb.setVisible(false);
                    screen.children().remove(eb);
                } else {
                    w.visible = false;
                    screen.children().remove(w);
                }
            }
        }
    }

    private void updateResetButtonState() {
        if (resetButton == null) return;
        resetButton.active = defaultArray != null && !array.equals(defaultArray);
    }

    /**
     * Toggles the expanded state if the given coordinates hit the arrow icon.
     *
     * @param mx the mouse x
     * @param my the mouse y
     * @return {@code true} if toggled
     */
    public boolean toggleIfHit(double mx, double my) {
        if (mx >= expX && mx <= expX + expW && my >= expY && my <= expY + expH) {
            expanded = !expanded;
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        graphics.text(Minecraft.getInstance().font, label, x + 4, y + 6, 0xFFFFFFFF);
        if (isHovered && tooltip != null && screen instanceof ConfigScreen cs) {
            if (mouseY >= y && mouseY <= y + rowHeight) {
                cs.setHoverTooltip(tooltip, mouseX, mouseY);
            }
        }
        int resetX = Minecraft.getInstance().screen.width - 80;
        int addX = resetX - 24;
        expX = addX - 12;
        expY = y + 2;
        expW = 12;
        expH = 16;
        String arrow = expanded ? "▾" : "▸";
        graphics.text(Minecraft.getInstance().font, arrow, expX + 2, expY + 4, 0xFFFFFFFF);

        if (addBtn != null) {
            addBtn.setX(addX);
            addBtn.setY(y);
        }
        if (resetButton != null) {
            resetButton.setX(resetX);
            resetButton.setY(y);
        }

        int rowY = y + rowHeight;
        int vx = x + 40;
        int topBound = 0;
        int bottomBound = Minecraft.getInstance().screen.height - 32;
        if (screen instanceof ConfigScreen cs) {
            topBound = cs.getContentTop();
            bottomBound = cs.getContentBottom();
        }
        for (int i = 0; i < valueBoxes.size(); i++) {
            var vb = valueBoxes.get(i);
            var cb = toggleBoxes.get(i);
            var del = delButtons.get(i);
            var up = upButtons.get(i);
            var down = downButtons.get(i);
            boolean visible = expanded && rowY + rowHeight > topBound && rowY <= bottomBound - 20;
            vb.visible = visible && cb == null;
            if (cb != null) cb.visible = visible;
            del.visible = visible;
            up.visible = visible;
            down.visible = visible;
            if (visible) {
                int vw = Math.max(60, resetX - vx - 86);
                if (cb == null) {
                    vb.setX(vx);
                    vb.setY(rowY);
                    vb.setWidth(vw);
                } else {
                    cb.setX(vx);
                    cb.setY(rowY);
                    cb.setWidth(20);
                }
                int deleteX = resetX - 21;
                int downX = deleteX - 22;
                int upX = downX - 22;
                del.setX(deleteX);
                del.setY(rowY);
                down.setX(downX);
                down.setY(rowY);
                up.setX(upX);
                up.setY(rowY);
                rowY += rowHeight;
            }
        }
    }

    @Override
    public Component getFieldName() {
        return label;
    }

    @Override
    public JsonArray getValue() {
        return array;
    }

    @Override
    public Optional<JsonArray> getDefaultValue() {
        return Optional.of(array);
    }
}
