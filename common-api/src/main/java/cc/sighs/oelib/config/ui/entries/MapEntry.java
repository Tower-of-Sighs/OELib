package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import cc.sighs.oelib.mixin.access.ScreenInvoker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MapEntry extends AbstractConfigEntry<JsonObject> {
    private final String keyPath;
    private final JsonObject working;
    private final int controlWidth;
    private final int rowHeight;
    private final Component label;
    private final Component tooltip;
    private final JsonObject mapObj;
    private final List<EditBox> keyBoxes = new ArrayList<>();
    private final List<EditBox> valBoxes = new ArrayList<>();
    private final List<Checkbox> valToggles = new ArrayList<>();
    private final List<Button> delButtons = new ArrayList<>();
    private boolean created = false;
    private Screen screen;
    private Button addBtn;
    private boolean expanded = false;
    private int expX, expY, expW, expH;
    private Button resetButton;
    private JsonObject defaultMap;
    private int headerY;

    public MapEntry(String keyPath, Component label, JsonObject working, int controlWidth, int rowHeight, Component tooltip) {
        this.keyPath = keyPath;
        this.label = label;
        this.tooltip = tooltip;
        this.working = working;
        this.controlWidth = controlWidth;
        this.rowHeight = rowHeight;
        var value = ConfigGuiUtil.getPath(working, keyPath);
        this.mapObj = value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    @Override
    public int getItemHeight() {
        return expanded ? rowHeight * Math.max(1, mapObj.size() + 1) : rowHeight;
    }

    public void attach(Screen screen, int x, int y, JsonObject defaults) {
        if (created) return;
        this.screen = screen;
        this.headerY = y;
        int resetX = screen.width - 80;
        int deleteX = resetX - 24;
        int addX = resetX - 24;

        var def = ConfigGuiUtil.getPath(defaults, keyPath);
        defaultMap = def != null && def.isJsonObject() ? def.getAsJsonObject() : new JsonObject();

        addBtn = Button.builder(Component.literal("+"), b -> {
            mapObj.add("", new JsonPrimitive(""));
            ConfigGuiUtil.setPath(working, keyPath, mapObj);
            rebuildRows(x, headerY, resetX, deleteX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(addX, y, 22, 20).build();
        ((ScreenInvoker) screen).invokeAddRenderableWidget(addBtn);

        resetButton = Button.builder(Component.translatable("config.oelib.reset"), b -> {
            mapObj.entrySet().clear();
            for (Map.Entry<String, JsonElement> e : defaultMap.entrySet()) {
                mapObj.add(e.getKey(), e.getValue());
            }
            ConfigGuiUtil.setPath(working, keyPath, mapObj);
            rebuildRows(x, headerY, resetX, deleteX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(resetX, y, 72, 20).build();
        ((ScreenInvoker) screen).invokeAddRenderableWidget(resetButton);

        rebuildRows(x, y, resetX, deleteX);
        updateResetButtonState();
        created = true;
    }

    private void rebuildRows(int x, int y, int resetX, int deleteX) {
        if (screen == null) return;

        clearWidgets();
        keyBoxes.clear();
        valBoxes.clear();
        valToggles.clear();
        delButtons.clear();

        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(mapObj.entrySet());
        int rowY = y + rowHeight;
        int margin = 40;
        int contentLeft = x + margin;
        int contentRight = resetX - margin;

        for (Map.Entry<String, JsonElement> e : entries) {
            var v = e.getValue();
            int width = Math.max(60, contentRight - contentLeft);
            int half = width / 2 - 6;

            var keyBox = ConfigGuiUtil.createEditBox(e.getKey(), contentLeft, rowY, half);
            ((ScreenInvoker) screen).invokeAddRenderableWidget(keyBox);

            EditBox valBox = ConfigGuiUtil.createEditBox(ConfigGuiUtil.jsonToString(v), contentLeft + half + 12, rowY, half);
            ((ScreenInvoker) screen).invokeAddRenderableWidget(valBox);

            Checkbox valToggle = null;
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
                boolean initial = v.getAsBoolean();
                valToggle = new Checkbox(contentLeft + half + 12, rowY, 20, 20, Component.empty(), initial) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        mapObj.add(keyBox.getValue(), new JsonPrimitive(this.selected()));
                        ConfigGuiUtil.setPath(working, keyPath, mapObj);
                        updateResetButtonState();
                        if (screen instanceof ConfigScreen cs) cs.markDirty();
                    }
                };
                ((ScreenInvoker) screen).invokeAddRenderableWidget(valToggle);
            }

            var del = Button.builder(Component.literal("x"), b -> {
                mapObj.remove(keyBox.getValue());
                ConfigGuiUtil.setPath(working, keyPath, mapObj);
                rebuildRows(x, headerY, resetX, deleteX);
                updateResetButtonState();
                if (screen instanceof ConfigScreen cs) cs.markDirty();
            }).bounds(deleteX, rowY, 20, 20).build();
            ((ScreenInvoker) screen).invokeAddRenderableWidget(del);

            keyBox.setResponder(newKey -> {
                if (newKey.equals(e.getKey())) return;
                var currentValue = mapObj.remove(e.getKey());
                if (currentValue == null) currentValue = v;
                mapObj.add(newKey, currentValue);
                ConfigGuiUtil.setPath(working, keyPath, mapObj);
                updateResetButtonState();
                if (screen instanceof ConfigScreen cs) cs.markDirty();
            });

            valBox.setResponder(newVal -> {
                var prev = mapObj.get(keyBox.getValue());
                String prevStr = ConfigGuiUtil.jsonToString(prev);
                if (!prevStr.equals(newVal)) {
                    mapObj.add(keyBox.getValue(), ConfigGuiUtil.parsePrimitive(newVal));
                    ConfigGuiUtil.setPath(working, keyPath, mapObj);
                    updateResetButtonState();
                    if (screen instanceof ConfigScreen cs) cs.markDirty();
                }
            });

            keyBoxes.add(keyBox);
            valBoxes.add(valBox);
            valToggles.add(valToggle);
            delButtons.add(del);
            rowY += rowHeight;
        }
    }

    private void clearWidgets() {
        for (EditBox kb : keyBoxes) {
            kb.setVisible(false);
            screen.children().remove(kb);
        }
        for (EditBox vb : valBoxes) {
            if (vb != null) {
                vb.setVisible(false);
                screen.children().remove(vb);
            }
        }
        for (Checkbox cb : valToggles) {
            if (cb != null) {
                cb.visible = false;
                screen.children().remove(cb);
            }
        }
        for (Button db : delButtons) {
            db.visible = false;
            screen.children().remove(db);
        }
    }

    private void updateResetButtonState() {
        if (resetButton == null) return;
        resetButton.active = defaultMap != null && !mapObj.equals(defaultMap);
    }

    public boolean toggleIfHit(double mx, double my) {
        if (mx >= expX && mx <= expX + expW && my >= expY && my <= expY + expH) {
            expanded = !expanded;
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        headerY = y;
        graphics.drawString(Minecraft.getInstance().font, label, x + 4, y + 6, 0xFFFFFFFF);
        if (isHovered && tooltip != null && screen instanceof ConfigScreen cs) {
            if (mouseY >= y && mouseY <= y + rowHeight) {
                cs.setHoverTooltip(tooltip, mouseX, mouseY);
            }
        }
        int resetX = Minecraft.getInstance().screen.width - 80;
        int deleteX = resetX - 24;
        int addX = resetX - 24;
        expX = addX - 12;
        expY = y + 2;
        expW = 12;
        expH = 16;
        String arrow = expanded ? "▾" : "▸";
        graphics.drawString(Minecraft.getInstance().font, arrow, expX + 2, expY + 4, 0xFFFFFFFF);

        if (addBtn != null) {
            addBtn.visible = expanded;
            addBtn.setX(addX);
            addBtn.setY(y);
        }
        if (resetButton != null) {
            resetButton.visible = expanded;
            resetButton.setX(resetX);
            resetButton.setY(y);
        }

        int rowY = y + rowHeight;
        int margin = 40;
        int contentLeft = x + margin;
        int contentRight = resetX - margin;
        for (int i = 0; i < keyBoxes.size(); i++) {
            EditBox kb = keyBoxes.get(i);
            EditBox vb = valBoxes.get(i);
            Checkbox cb = valToggles.get(i);
            Button del = delButtons.get(i);
            int bottomBarTop = Minecraft.getInstance().screen.height - 32;
            boolean visible = expanded && rowY <= bottomBarTop - 20;
            kb.visible = visible;
            if (vb != null) vb.visible = visible && cb == null;
            if (cb != null) cb.visible = visible;
            del.visible = visible;
            if (visible) {
                int width = Math.max(60, contentRight - contentLeft);
                int half = width / 2 - 6;
                kb.setX(contentLeft);
                kb.setY(rowY);
                kb.setWidth(half);
                if (vb != null && cb == null) {
                    vb.setX(contentLeft + half + 12);
                    vb.setY(rowY);
                    vb.setWidth(half);
                }
                if (cb != null) {
                    cb.setX(contentLeft + half + 12);
                    cb.setY(rowY);
                    cb.setWidth(20);
                }
                del.setX(deleteX);
                del.setY(rowY);
                rowY += rowHeight;
            }
        }
    }

    @Override
    public Component getFieldName() {
        return label;
    }

    @Override
    public JsonObject getValue() {
        return mapObj;
    }

    @Override
    public Optional<JsonObject> getDefaultValue() {
        return Optional.of(mapObj);
    }

    @Override
    public void dispose() {
        if (screen != null) {
            clearWidgets();
            if (addBtn != null) {
                addBtn.visible = false;
                screen.children().remove(addBtn);
            }
            if (resetButton != null) {
                resetButton.visible = false;
                screen.children().remove(resetButton);
            }
        }
        keyBoxes.clear();
        valBoxes.clear();
        valToggles.clear();
        delButtons.clear();
        addBtn = null;
        resetButton = null;
        created = false;
    }
}
