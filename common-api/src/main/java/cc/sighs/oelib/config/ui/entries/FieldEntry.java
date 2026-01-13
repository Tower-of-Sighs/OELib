package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FieldEntry extends AbstractConfigEntry<Object> {
    private final ConfigValueMeta meta;
    private final JsonObject working;
    private final int labelWidth;
    private final int controlWidth;
    private final int rowHeight;
    private final Component label;
    private final Component tooltip;
    private final ConfigUiHint hint;
    private boolean created = false;
    private Checkbox toggle;
    private EditBox textBox;
    private AbstractSliderButton slider;
    private CycleButton<String> dropdown;
    private Button resetButton;
    private JsonElement defaultValueElement;
    private Screen screen;

    public FieldEntry(ConfigValueMeta meta, JsonObject working, int labelWidth, int controlWidth, int rowHeight) {
        this.meta = meta;
        this.working = working;
        this.labelWidth = labelWidth;
        this.controlWidth = controlWidth;
        this.rowHeight = rowHeight;
        this.label = meta.translationKey().map(Component::translatable).orElse(Component.literal(meta.key()));
        this.tooltip = meta.tooltip().map(Component::translatable).orElse(null);
        var value = ConfigGuiUtil.getPath(working, meta.key());
        this.hint = meta.uiHint().orElse(defaultHintFor(value));
    }

    @Override
    public int getItemHeight() {
        return rowHeight;
    }

    public void attach(Screen screen, int x, int y, JsonObject defaults) {
        if (created) return;
        this.screen = screen;
        int resetX = screen.width - 80;
        int controlX = resetX - 8 - controlWidth;

        var value = ConfigGuiUtil.getPath(working, meta.key());
        defaultValueElement = ConfigGuiUtil.getPath(defaults, meta.key());

        createUiControl(controlX, y, value);
        createResetButton(resetX, y);

        updateResetButtonState();
        created = true;
    }

    private void createUiControl(int controlX, int y, JsonElement currentValue) {
        switch (hint.type()) {
            case TOGGLE -> createToggleControl(controlX, y, currentValue);
            case SLIDER -> createSliderControl(controlX, y, currentValue);
            case DROPDOWN -> createDropdownControl(controlX, y, currentValue);
            default -> createTextBoxControl(controlX, y, currentValue);
        }
    }

    private void createToggleControl(int controlX, int y, JsonElement currentValue) {
        boolean val = currentValue != null && currentValue.isJsonPrimitive()
                && currentValue.getAsJsonPrimitive().isBoolean()
                && currentValue.getAsBoolean();

        toggle = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                .selected(val)
                .onValueChange((box, selected) -> {
                    ConfigGuiUtil.setPath(working, meta.key(), new JsonPrimitive(selected));
                    updateResetButtonState();
                    if (screen instanceof ConfigScreen cs) cs.markDirty();
                })
                .build();
        toggle.setPosition(controlX - 8, y);
        toggle.setWidth(20);
        screen.addRenderableWidget(toggle);
    }

    private void createSliderControl(int controlX, int y, JsonElement currentValue) {
        double min = hint.min() != null ? hint.min() : 0.0;
        double max = hint.max() != null ? hint.max() : 1.0;
        double step = hint.step() != null ? hint.step() : 0.01;
        double cur = getDoubleValue(currentValue, min);

        slider = createSlider(controlX, y, controlWidth, min, max, step, cur);
        screen.addRenderableWidget(slider);
    }

    private AbstractSliderButton createSlider(int x, int y, int width,
                                              double min, double max, double step, double currentValue) {
        return new AbstractSliderButton(x, y, width, 20,
                Component.literal(String.format(Locale.ROOT, "%.2f", currentValue)),
                (currentValue - min) / Math.max(0.0001, (max - min))) {
            @Override
            protected void updateMessage() {
                double v = min + this.value * (max - min);
                setMessage(Component.literal(String.format(Locale.ROOT, "%.2f", v)));
            }

            @Override
            protected void applyValue() {
                double v = min + this.value * (max - min);
                double snapped = Math.round(v / step) * step;
                ConfigGuiUtil.setPath(working, meta.key(), new JsonPrimitive(snapped));
                updateResetButtonState();
                if (screen instanceof ConfigScreen cs) cs.markDirty();
            }
        };
    }

    private void createDropdownControl(int controlX, int y, JsonElement currentValue) {
        List<String> options = hint.options() != null ? hint.options() : List.of();
        String cur = ConfigGuiUtil.jsonToString(currentValue);

        if (options.isEmpty()) {
            cur = "";
        } else if (!options.contains(cur)) {
            cur = options.getFirst();
        }

        dropdown = CycleButton.builder(this::optionComponent)
                .withValues(options)
                .displayOnlyValue()
                .withInitialValue(cur)
                .create(controlX, y, controlWidth, 20, Component.empty(),
                        (d, v) -> {
                            ConfigGuiUtil.setPath(working, meta.key(), new JsonPrimitive(v));
                            updateResetButtonState();
                            if (screen instanceof ConfigScreen cs) cs.markDirty();
                        });
        screen.addRenderableWidget(dropdown);
    }

    private void createTextBoxControl(int controlX, int y, JsonElement currentValue) {
        textBox = ConfigGuiUtil.createEditBox(ConfigGuiUtil.jsonToString(currentValue), controlX, y, controlWidth);
        screen.addRenderableWidget(textBox);

        textBox.setResponder(str -> {
            var currentEl = ConfigGuiUtil.getPath(working, meta.key());
            String prevStr = ConfigGuiUtil.jsonToString(currentEl);

            if (!str.equals(prevStr)) {
                JsonElement newEl = parseStringToJson(str);
                ConfigGuiUtil.setPath(working, meta.key(), newEl);
                updateResetButtonState();
                if (screen instanceof ConfigScreen cs) cs.markDirty();
            }
        });
    }

    private void createResetButton(int resetX, int y) {
        resetButton = Button.builder(Component.translatable("config.oelib.reset"),
                        b -> resetToDefault())
                .bounds(resetX, y, 72, 20).build();
        screen.addRenderableWidget(resetButton);
    }

    private void resetToDefault() {
        if (defaultValueElement == null) return;

        ConfigGuiUtil.setPath(working, meta.key(), defaultValueElement);
        String defaultValueStr = ConfigGuiUtil.jsonToString(defaultValueElement);

        updateControlValue(defaultValueStr, defaultValueElement);
        updateResetButtonState();
        if (screen instanceof ConfigScreen cs) cs.markDirty();
    }

    private void updateControlValue(String defaultValueStr, JsonElement defaultValueElement) {
        switch (hint.type()) {
            case TOGGLE -> {
                if (toggle != null && defaultValueElement.isJsonPrimitive()
                        && defaultValueElement.getAsJsonPrimitive().isBoolean()) {
                    toggle.selected = defaultValueElement.getAsBoolean();
                }
            }
            case SLIDER -> {
                if (slider != null) {
                    recreateSliderWithDefaultValue(defaultValueElement);
                }
            }
            case DROPDOWN -> {
                if (dropdown != null) {
                    dropdown.setValue(defaultValueStr);
                }
            }
            default -> {
                if (textBox != null) {
                    textBox.setValue(defaultValueStr);
                }
            }
        }
    }

    private void recreateSliderWithDefaultValue(JsonElement defaultValueElement) {
        double min = hint.min() != null ? hint.min() : 0.0;
        double max = hint.max() != null ? hint.max() : 1.0;
        double step = hint.step() != null ? hint.step() : 0.01;
        double defaultValue = getDoubleValue(defaultValueElement, min);

        int sx = slider.getX();
        int sy = slider.getY();
        int sw = slider.getWidth();

        screen.children().remove(slider);
        slider.visible = false;

        slider = createSlider(sx, sy, sw, min, max, step, defaultValue);
        screen.addRenderableWidget(slider);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + 4, y + 6, 0xFFFFFFFF);

        int resetX = Minecraft.getInstance().screen.width - 80;
        int labelTextWidth = font.width(label.getVisualOrderText());
        int reservedLabelWidth = Math.max(this.labelWidth, labelTextWidth + 12);
        int dynamicControlX = x + reservedLabelWidth;
        int availableWidth = Math.max(60, resetX - 8 - dynamicControlX);
        int dynamicControlWidth = Math.max(100, Math.min(this.controlWidth, availableWidth));

        updateControlPositions(dynamicControlX, y, dynamicControlWidth, resetX);
        if (isHovered && tooltip != null && screen instanceof ConfigScreen cs) {
            cs.setHoverTooltip(tooltip, mouseX, mouseY);
        }
    }

    private void updateControlPositions(int controlX, int y, int controlWidth, int resetX) {
        // 更新控件位置
        if (toggle != null) {
            toggle.setPosition(controlX - 8, y); // 保持与创建时相同的偏移
            toggle.setWidth(20);
        }
        if (textBox != null) {
            textBox.setX(controlX);
            textBox.setY(y);
            textBox.setWidth(controlWidth);
        }
        if (slider != null) {
            slider.setX(controlX);
            slider.setY(y);
            slider.setWidth(controlWidth);
        }
        if (dropdown != null) {
            dropdown.setX(controlX);
            dropdown.setY(y);
            dropdown.setWidth(controlWidth);
        }
        if (resetButton != null) {
            resetButton.setX(resetX);
            resetButton.setY(y);
        }
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
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    private void updateResetButtonState() {
        if (resetButton == null) return;

        if (defaultValueElement == null) {
            resetButton.active = false;
            return;
        }

        var current = ConfigGuiUtil.getPath(working, meta.key());
        boolean same = (current == null && defaultValueElement.isJsonNull()) ||
                (current != null && current.equals(defaultValueElement));
        resetButton.active = !same;
    }

    private Component optionComponent(String option) {
        var base = meta.translationKey().orElse(null);
        return base != null ? Component.translatable(base + "." + option) : Component.literal(option);
    }

    private ConfigUiHint defaultHintFor(JsonElement value) {
        if (value != null && value.isJsonPrimitive()) {
            return value.getAsJsonPrimitive().isBoolean()
                    ? ConfigUiHint.toggle()
                    : ConfigUiHint.text();
        }
        return ConfigUiHint.text();
    }

    private double getDoubleValue(JsonElement element, double defaultValue) {
        return element != null && element.isJsonPrimitive()
                ? element.getAsJsonPrimitive().getAsDouble()
                : defaultValue;
    }

    private JsonElement parseStringToJson(String str) {
        String s = str.trim();

        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(s));
        }

        if (s.matches("^-?\\d+$")) {
            try {
                return new JsonPrimitive(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }

        if (s.matches("^-?\\d+(?:\\.\\d+)?$")) {
            try {
                return new JsonPrimitive(Double.parseDouble(s));
            } catch (NumberFormatException ignored) {
            }
        }

        return new JsonPrimitive(s);
    }
}
