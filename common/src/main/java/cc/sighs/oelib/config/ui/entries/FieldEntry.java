package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import cc.sighs.oelib.config.ui.ConfigWidgetRegistry;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An interactive entry for a single configuration field, rendering the
 * appropriate widget based on the field's {@link ConfigUiHint}.
 *
 * <p>Supported widgets include text boxes, toggles (checkboxes), sliders,
 * dropdowns, and custom widgets registered via {@link ConfigWidgetRegistry}.
 * A reset button next to each field reverts it to the default value.
 */
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
    private AbstractWidget customWidget;
    private ConfigWidgetRegistry.CustomWidgetHandle customHandle;
    private Button resetButton;
    private JsonElement defaultValueElement;
    private Screen screen;

    /**
     * Constructs a field entry.
     *
     * @param meta         the field metadata
     * @param working      the working JSON object being edited
     * @param labelWidth   the reserved width for the label
     * @param controlWidth the width of the control widget
     * @param rowHeight    the row height
     */
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

    /**
     * Attaches this entry to a screen, creating widgets at the given position.
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
        int controlX = resetX - 8 - controlWidth;

        var value = ConfigGuiUtil.getPath(working, meta.key());
        defaultValueElement = ConfigGuiUtil.getPath(defaults, meta.key());

        createUiControl(controlX, y, value);
        createResetButton(resetX, y);

        updateResetButtonState();
        created = true;
    }

    private void createUiControl(int controlX, int y, JsonElement currentValue) {
        switch (hint) {
            case ConfigUiHint.Toggle ignored -> createToggleControl(controlX, y, currentValue);
            case ConfigUiHint.Slider sliderHint -> createSliderControl(controlX, y, currentValue, sliderHint);
            case ConfigUiHint.Dropdown dropdownHint -> createDropdownControl(controlX, y, currentValue, dropdownHint);
            case ConfigUiHint.Custom customHint -> createCustomControl(controlX, y, currentValue, customHint);
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

    private void createSliderControl(int controlX, int y, JsonElement currentValue, ConfigUiHint.Slider sliderHint) {
        double min = sliderHint.min();
        double max = sliderHint.max();
        double step = sliderHint.step();
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

    private void createDropdownControl(int controlX, int y, JsonElement currentValue, ConfigUiHint.Dropdown dropdownHint) {
        List<String> options = dropdownHint.options();
        String cur = ConfigGuiUtil.jsonToString(currentValue);

        if (options.isEmpty()) {
            cur = "";
        } else if (!options.contains(cur)) {
            cur = options.getFirst();
        }

        dropdown = CycleButton.builder(this::optionComponent, cur)
                .withValues(options)
                .displayOnlyValue()
                .create(controlX, y, controlWidth, 20, Component.empty(),
                        (d, v) -> {
                            ConfigGuiUtil.setPath(working, meta.key(), new JsonPrimitive(v));
                            updateResetButtonState();
                            if (screen instanceof ConfigScreen cs) cs.markDirty();
                        });
        screen.addRenderableWidget(dropdown);
    }

    private void createCustomControl(int controlX, int y, JsonElement currentValue, ConfigUiHint.Custom customHint) {
        var factoryOpt = ConfigWidgetRegistry.custom(customHint.widgetId());
        if (factoryOpt.isEmpty()) {
            createTextBoxControl(controlX, y, currentValue);
            return;
        }
        var context = new ConfigWidgetRegistry.CustomWidgetContext(
                screen,
                meta,
                working,
                currentValue,
                controlX,
                y,
                controlWidth,
                20,
                () -> {
                    updateResetButtonState();
                    if (screen instanceof ConfigScreen cs) {
                        cs.markDirty();
                    }
                }
        );
        customHandle = factoryOpt.get().create(context);
        if (customHandle == null || customHandle.widget() == null) {
            createTextBoxControl(controlX, y, currentValue);
            return;
        }
        customWidget = customHandle.widget();
        screen.addRenderableWidget(customWidget);
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
        switch (hint) {
            case ConfigUiHint.Toggle ignored -> {
                if (toggle != null && defaultValueElement.isJsonPrimitive()
                        && defaultValueElement.getAsJsonPrimitive().isBoolean()) {
                    toggle.selected = defaultValueElement.getAsBoolean();
                }
            }
            case ConfigUiHint.Slider sliderHint -> {
                if (slider != null) {
                    recreateSliderWithDefaultValue(defaultValueElement, sliderHint);
                }
            }
            case ConfigUiHint.Dropdown ignored -> {
                if (dropdown != null) {
                    dropdown.setValue(defaultValueStr);
                }
            }
            case ConfigUiHint.Custom ignored -> {
                if (customHandle != null) {
                    customHandle.reset(defaultValueElement);
                }
            }
            default -> {
                if (textBox != null) {
                    textBox.setValue(defaultValueStr);
                }
            }
        }
    }

    private void recreateSliderWithDefaultValue(JsonElement defaultValueElement, ConfigUiHint.Slider sliderHint) {
        double min = sliderHint.min();
        double max = sliderHint.max();
        double step = sliderHint.step();
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
    public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, label, x + 4, y + 6, 0xFFFFFFFF);

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
        boolean rowVisible = true;
        if (screen instanceof ConfigScreen cs) {
            rowVisible = y + rowHeight > cs.getContentTop() && y < cs.getContentBottom() - 20;
        }

        if (toggle != null) {
            toggle.visible = rowVisible;
            toggle.setPosition(controlX - 8, y);
            toggle.setWidth(20);
        }
        if (textBox != null) {
            textBox.visible = rowVisible;
            textBox.setX(controlX);
            textBox.setY(y);
            textBox.setWidth(controlWidth);
        }
        if (slider != null) {
            slider.visible = rowVisible;
            slider.setX(controlX);
            slider.setY(y);
            slider.setWidth(controlWidth);
        }
        if (dropdown != null) {
            dropdown.visible = rowVisible;
            dropdown.setX(controlX);
            dropdown.setY(y);
            dropdown.setWidth(controlWidth);
        }
        if (customWidget != null) {
            customWidget.visible = rowVisible;
            customWidget.setX(controlX);
            customWidget.setY(y);
            customWidget.setWidth(controlWidth);
        }
        if (resetButton != null) {
            resetButton.visible = rowVisible;
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
