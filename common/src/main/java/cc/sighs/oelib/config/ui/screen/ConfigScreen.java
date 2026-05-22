package cc.sighs.oelib.config.ui.screen;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.*;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.net.ConfigUpdateRequestPacket;
import cc.sighs.oelib.config.ui.entries.*;
import cc.sighs.oelib.config.ui.scissor.ScissorsHandler;
import cc.sighs.oelib.config.ui.widget.DynamicEntryListWidget;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.config.util.GsonUtil;
import cc.sighs.oelib.network.api.NetworkManager;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * An auto-generated screen for viewing and editing configuration values
 * at runtime.
 *
 * <p>The screen renders a scrollable list of {@link AbstractConfigEntry}
 * instances, one per visible field, with a sidebar showing available
 * configurations for the current mod. Changes are validated on save and,
 * for server-side configs, sent to the server via
 * {@link ConfigUpdateRequestPacket}.
 *
 * <p>The screen is opened via the mod's configuration key binding or
 * through the Mod Menu integration.
 */
public class ConfigScreen extends Screen {
    private static final int DIVIDER_HEIGHT = 8;
    private static final int DIVIDER_COLOR = 0x50FFFFFF;
    private static final int DIVIDER_BASE_INSET = 10;
    private static final int DIVIDER_NESTED_STEP = 14;
    private static final int NESTED_INNER_GAP = 4;
    private static final int NESTED_OUTER_GAP = 4;

    /** The id of the configuration being edited. */
    public final ResourceLocation configId;
    private final ConfigUnit<Object> unit;
    /** The codec for this configuration. */
    public final ConfigCodec<Object> codec;
    private final List<ConfigValueMeta> fields;
    private final List<Runnable> applyActions = new ArrayList<>();
    private final Map<String, Component> errors = new HashMap<>();
    private final Map<String, Boolean> groupExpanded = new HashMap<>();
    private final int contentTop = 36;
    private final int rowHeight = 24;
    private final Scroller refScroller = new Scroller();
    private final Scroller sideSlider = new Scroller();
    private final List<ConfigCtx> contexts = new ArrayList<>();
    private final Map<AbstractConfigEntry<?>, JsonObject> entryDefaults = new HashMap<>();
    private final JsonObject working;
    private boolean dirty;
    private String searchText = "";
    private boolean sidebarExpanded = true;
    private List<FieldRef> references = List.of();
    private Button saveButton;
    private Button cancelButton;
    private Component lastTooltip;
    private int mouseXLast = -1;
    private int mouseYLast = -1;
    private DynamicEntryListWidget listWidget;
    private EditBox searchBox;
    private int sideExpandLimit = 120;
    private List<ResourceLocation> modConfigs = new ArrayList<>();

    /**
     * Constructs a config screen for the given mod.
     *
     * @param parent the parent screen
     * @param modid  the mod id whose configs to display
     */
    public ConfigScreen(Screen parent, String modid) {
        super(Component.translatable("config." + modid + ".title"));
        Set<ResourceLocation> ids = new HashSet<>();
        ids.addAll(ClientConfigManager.all().keySet());
        ids.addAll(ServerConfigManager.all().keySet());
        ResourceLocation chosen = null;
        for (ResourceLocation id : ids) {
            if (id.getNamespace().equals(modid)) {
                chosen = id;
                break;
            }
        }
        if (chosen == null && !ids.isEmpty()) {
            chosen = ids.iterator().next();
        }
        if (chosen == null) {
            this.configId = new ResourceLocation(modid, "none");
            this.unit = null;
            this.codec = null;
            this.fields = List.of();
            this.working = new JsonObject();
            return;
        }
        var opt = ConfigManager.get(chosen);
        var cast = opt.map(ConfigGuiUtil::castUnit).orElse(null);
        this.configId = chosen;
        this.unit = cast;
        this.codec = cast.codec();
        this.fields = codec.fields();
        this.unit.reload();
        this.working = ConfigGuiUtil.toJson(unit.get(), this);
    }

    private int getSideSliderPosition() {
        return 14 + sideSlider.currentInt();
    }

    private int sidebarWidth() {
        return getSideSliderPosition();
    }

    private int sidebarContentWidth() {
        return sidebarWidth() - 14;
    }

    private static String idToGroupStateKey(ResourceLocation configId, String groupPath) {
        return configId.toString() + "|" + groupPath;
    }

    private static String parentPathOf(String key) {
        int idx = key.lastIndexOf('.');
        if (idx <= 0) {
            return null;
        }
        return key.substring(0, idx);
    }

    private void applyEdits() {
        applyActions.forEach(Runnable::run);
        applyActions.clear();
    }

    private static int depthOfPath(String path) {
        if (path == null || path.isBlank()) {
            return 0;
        }
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '.') {
                depth++;
            }
        }
        return depth;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        applyActions.clear();
        errors.clear();
        lastTooltip = null;
        if (unit == null || codec == null) {
            return;
        }
        int initialScroll = 0;
        if (listWidget != null) {
            initialScroll = Math.max(listWidget.getScrollOffset(), listWidget.getScrollTargetOffset());
        }
        Map<ResourceLocation, JsonObject> preservedWorking = snapshotWorkingByConfig();
        contexts.clear();
        int sidebar = sidebarWidth();
        int labelWidth = 160;
        int left = sidebar + 16;
        int controlWidth = Math.min(220, this.width - left - 40 - labelWidth);
        int listControlWidth = controlWidth;
        int fieldControlWidth = Math.min(200, this.width - left - 60 - labelWidth);
        int y = contentTop;
        searchBox = new EditBox(Minecraft.getInstance().font, left, y, this.width - left - 20, 20, Component.empty());
        searchBox.setSuggestion(Component.translatable("config.oelib.search").getString());
        searchBox.setValue(searchText);
        searchBox.setResponder(s -> {
            searchText = s;
            searchBox.setSuggestion(s.isEmpty() ? Component.translatable("config.oelib.search").getString() : "");
            init();
        });
        searchBox.setFocused(true);
        this.setInitialFocus(searchBox);
        addRenderableWidget(searchBox);
        y += 24;
        listWidget = new DynamicEntryListWidget(Minecraft.getInstance(), this.width - left - 12, this.height, y, this.height - 32);
        listWidget.setLeftPos(left);
        Set<ResourceLocation> modsConfigsSet = new HashSet<>();
        modsConfigsSet.addAll(ClientConfigManager.all().keySet());
        modsConfigsSet.addAll(ServerConfigManager.all().keySet());
        this.modConfigs = modsConfigsSet.stream()
                .filter(id -> id.getNamespace().equals(this.configId.getNamespace()))
                .sorted(Comparator.comparing(ResourceLocation::getPath))
                .toList();
        List<FieldRef> refs = new ArrayList<>();
        for (ResourceLocation id : modConfigs) {
            refs.add(new FieldRef(null, Component.translatable("config." + id.getNamespace() + "." + id.getPath() + ".title"), 0));
        }
        entryDefaults.clear();
        var items = listWidget.children();
        items.clear();
        items.add(new EmptyEntry(5));
        var searchLower = searchText.toLowerCase(Locale.ROOT);
        for (ResourceLocation id : modConfigs) {
            var optUnit = ConfigManager.get(id);
            if (optUnit.isEmpty()) continue;
            var u = ConfigGuiUtil.castUnit(optUnit.get());
            u.reload();
            var c = u.codec();
            var preserved = preservedWorking.get(id);
            var workingJson = preserved != null
                    ? preserved.deepCopy()
                    : ConfigGuiUtil.encodeToJsonObject(c.codec(), u.get());
            var defaultObj = c.codec().parse(JsonOps.INSTANCE, new JsonObject()).result().orElse(null);
            var defaultsJson = defaultObj != null ? ConfigGuiUtil.encodeToJsonObject(c.codec(), defaultObj) : new JsonObject();
            var flds = c.fields();
            contexts.add(new ConfigCtx(id, u, c, flds, workingJson, defaultsJson));
            items.add(new CategoryTextEntry(Component.translatable("config." + id.getNamespace() + "." + id.getPath() + ".title"), Component.empty()));
            items.add(new DividerEntry());
            items.add(new EmptyEntry(5));
            appendNestedFieldEntries(id, items, entryDefaults, flds, workingJson, defaultsJson, searchLower, labelWidth, fieldControlWidth, listControlWidth);
        }
        references = refs;
        int longest = 0;
        for (FieldRef r : references) {
            int w = this.font.width(Component.literal("- ").append(r.label()).getVisualOrderText());
            if (w > longest) longest = w;
        }
        sideExpandLimit = Math.min(Math.max(80, longest + 16), this.width / 4);
        sideSlider.setMaxScroll(sideExpandLimit - 14);
        sideSlider.offset(sidebarExpanded ? sideExpandLimit - 14 : -sideExpandLimit);
        int attachY = y;
        for (AbstractConfigEntry<?> entry : items) {
            var def = entryDefaults.getOrDefault(entry, new JsonObject());
            if (entry instanceof FieldEntry fe) {
                fe.attach(this, left, attachY, def);
            } else if (entry instanceof MapEntry me) {
                me.attach(this, left, attachY, def);
            } else if (entry instanceof ListEntry le) {
                le.attach(this, left, attachY, def);
            }
            attachY += entry.getItemHeight();
        }
        if (initialScroll > 0 && listWidget != null) {
            listWidget.refreshScrollBounds();
            listWidget.scrollTo(initialScroll, false);
        }
        if (!references.isEmpty()) {
            int itemHeight = this.font.lineHeight + 3;
            int available = this.height - 32 - 8;
            int total = references.size() * itemHeight;
            refScroller.setMaxScroll(Math.max(0, total - available));
        } else {
            refScroller.setMaxScroll(0);
        }
        int btnY = this.height - 28;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("config.oelib.done"), b -> onSave()).bounds(this.width / 2 + 4, btnY, 120, 20).build());
        cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(this.width / 2 - 124, btnY, 120, 20).build());
    }

    private void onSave() {
        applyEdits();
        if (syncAndSaveConfigs()) {
            dirty = false;
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (needsRefreshFromUnits()) {
            init();
        }
        lastTooltip = null;
        mouseXLast = -1;
        mouseYLast = -1;
        refScroller.update(partialTick);
        sideSlider.update(partialTick);
        super.renderBackground(gui);
        ScissorsHandler.INSTANCE.clearScissors();
        int sidebar = sidebarWidth();
        int contentLeft = sidebar;
        int sliderX = sidebar - 14;
        gui.drawCenteredString(this.font, this.title, this.width / 2, 12, -1);
        int left = sidebar + 16;
        if (searchBox != null) {
            searchBox.setX(left);
            searchBox.setWidth(this.width - left - 20);
            if (listWidget != null) {
                int searchY = listWidget.top - listWidget.getScrollOffset() - 24;
                searchBox.setY(searchY);
            }
        }
        listWidget.setLeftPos(left);
        listWidget.width = this.width - left - 12;
        int arrowY = this.height / 2 - this.font.lineHeight / 2;
        var arrow = sidebarExpanded ? "<" : ">";
        gui.drawString(this.font, arrow, sliderX + 7 - this.font.width(arrow) / 2, arrowY, 0xFFFFFFFF);
        if (sidebarExpanded) {
            gui.enableScissor(0, 0, sidebarContentWidth(), this.height);
            int refX = 4;
            int refAreaTop = 8;
            int refAreaBottom = this.height - 32;
            int refWidth = sidebarContentWidth() - 8;
            int itemHeight = this.font.lineHeight + 6;
            int scroll = refScroller.currentInt();
            int refY = refAreaTop - scroll;
            for (FieldRef ref : references) {
                if (refY + itemHeight >= refAreaTop && refY <= refAreaBottom) {
                    int color = 0xFFFFFFFF;
                    if (mouseX >= refX && mouseX <= refX + refWidth && mouseY >= refY && mouseY <= refY + itemHeight) {
                        color = 0xFFFFD37F;
                    }
                    gui.drawString(this.font, ref.label().getVisualOrderText(), refX + 2, refY, color);
                }
                refY += itemHeight;
            }
            if (refScroller.hasScroll()) {
                int barAreaHeight = refAreaBottom - refAreaTop;
                if (barAreaHeight > 0) {
                    int total = references.size() * itemHeight;
                    int barHeight = Math.max(10, barAreaHeight * barAreaHeight / Math.max(barAreaHeight, total));
                    int maxScroll = Math.max(1, refScroller.max);
                    int barY = refAreaTop + (int) Math.round((double) scroll * (barAreaHeight - barHeight) / maxScroll);
                    int barX1 = sliderX - 3;
                    int barX2 = sliderX - 1;
                    gui.fill(barX1, refAreaTop, barX2, refAreaBottom, 0x66000000);
                    gui.fill(barX1, barY, barX2, barY + barHeight, 0xCCFFFFFF);
                }
            }
            gui.disableScissor();
        }
        int headerBottom = this.contentTop;
        int bottomBarTop = this.height - 32;

        boolean saveVisible = saveButton != null && saveButton.visible;
        boolean cancelVisible = cancelButton != null && cancelButton.visible;
        if (saveButton != null) {
            saveButton.visible = false;
        }
        if (cancelButton != null) {
            cancelButton.visible = false;
        }

        gui.enableScissor(0, headerBottom, this.width, bottomBarTop);
        if (listWidget != null) {
            listWidget.render(gui, mouseX, mouseY, partialTick);
        }
        super.render(gui, mouseX, mouseY, partialTick);
        gui.disableScissor();

        if (saveButton != null) {
            saveButton.visible = saveVisible;
        }
        if (cancelButton != null) {
            cancelButton.visible = cancelVisible;
        }

        gui.blit(DynamicEntryListWidget.VERTICAL_HEADER_SEPARATOR, sliderX - 1, 0, 0, 0, 1, this.height, 2, 32);
        gui.fill(sliderX, headerBottom - 1, this.width, headerBottom, 0x80FFFFFF);
        gui.fill(sliderX, bottomBarTop - 1, this.width, bottomBarTop, 0x80FFFFFF);
        if (saveButton != null) {
            saveButton.render(gui, mouseX, mouseY, partialTick);
        }
        if (cancelButton != null) {
            cancelButton.render(gui, mouseX, mouseY, partialTick);
        }

        if (!errors.isEmpty()) {
            for (Map.Entry<String, Component> entry : errors.entrySet()) {
                gui.drawString(this.font, entry.getValue(), contentLeft + 16, this.height - 40, 0xFFFF4040);
            }
        }
        if (lastTooltip != null && mouseXLast >= 0) {
            gui.renderTooltip(this.font, lastTooltip, mouseXLast, mouseYLast);
        }
    }

    /**
     * Marks this screen as having unsaved changes.
     */
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public void onClose() {
        if (!dirty) {
            super.onClose();
            return;
        }
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirm -> {
            if (confirm) {
                applyEdits();
                if (syncAndSaveConfigs()) {
                    dirty = false;
                    Minecraft.getInstance().setScreen(null);
                    return;
                }
                Minecraft.getInstance().setScreen(this);
                return;
            }
            Minecraft.getInstance().setScreen(this);
        }, Component.translatable("config.oelib.unsaved.title"), Component.translatable("config.oelib.unsaved.message")));
    }

    private boolean syncAndSaveConfigs() {
        errors.clear();
        boolean allOk = true;
        for (ConfigCtx ctx : contexts) {
            for (var rootEntry : ctx.working.entrySet()) {
                var v = rootEntry.getValue();
                if (v.isJsonObject()) {
                    GsonUtil.removeBlankStringValues(v.getAsJsonObject());
                } else if (v.isJsonArray()) {
                    GsonUtil.removeBlankStringElements(v.getAsJsonArray());
                }
            }

            var parseResult = ctx.codec.codec().parse(JsonOps.INSTANCE, ctx.working);
            if (parseResult.error().isPresent()) {
                String message = parseResult.error().get().message();
                OELib.LOGGER.error("Failed to parse edited config {}: {}", ctx.id, message);
                errors.put(ctx.id.toString(), Component.literal("[" + ctx.id + "] " + message));
                allOk = false;
                continue;
            }

            var valueOpt = parseResult.result();
            if (valueOpt.isEmpty()) {
                String message = "Parse returned empty result";
                OELib.LOGGER.error("Failed to parse edited config {}: {}", ctx.id, message);
                errors.put(ctx.id.toString(), Component.literal("[" + ctx.id + "] " + message));
                allOk = false;
                continue;
            }

            var value = valueOpt.get();
            try {
                var side = ctx.codec.meta().side();
                if (side == ConfigSide.CLIENT) {
                    ctx.unit.setValue(value);
                    ctx.unit.save();
                } else if (side == ConfigSide.SERVER) {
                    ctx.unit.setValue(value);
                    ctx.unit.save();
                    if (Minecraft.getInstance().getConnection() != null) {
                        var format = ctx.codec.meta().format();
                        var encoded = ConfigSerializationUtil.encodeToString(value, format, ctx.codec.codec(), ctx.codec.fields());
                        if (encoded.isEmpty()) {
                            String message = "Failed to encode payload for server update";
                            OELib.LOGGER.error("{} {}", message, ctx.id);
                            errors.put(ctx.id.toString(), Component.literal("[" + ctx.id + "] " + message));
                            allOk = false;
                            continue;
                        }
                        NetworkManager.sendToServer(new ConfigUpdateRequestPacket(ctx.id, encoded.get(), format, true));
                    }
                }
            } catch (Throwable t) {
                OELib.LOGGER.error("Exception while saving config {}: {}", ctx.id, t.getMessage(), t);
                errors.put(ctx.id.toString(), Component.literal("[" + ctx.id + "] " + t.getMessage()));
                allOk = false;
            }
        }
        return allOk;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseXLast = (int) mouseX;
        this.mouseYLast = (int) mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    /**
     * Returns the top y coordinate of the scrollable content area.
     *
     * @return the content top
     */
    public int getContentTop() {
        return contentTop;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int sidebar = sidebarWidth();
        int sliderX = sidebar - 14;
        if (mouseX >= 0 && mouseX <= sliderX && mouseY >= 0 && mouseY <= this.height && delta != 0.0) {
            int itemHeight = this.font.lineHeight + 3;
            refScroller.offset(-delta * itemHeight);
            return true;
        }
        if (listWidget != null && mouseX >= sliderX && mouseX <= this.width && mouseY >= contentTop && mouseY <= this.height - 32 && delta != 0.0) {
            listWidget.offset(-delta * 28.0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean needsRefreshFromUnits() {
        if (dirty) return false;
        try {
            for (ConfigCtx ctx : contexts) {
                var fresh = ConfigGuiUtil.encodeToJsonObject(ctx.codec.codec(), ctx.unit.get());
                if (!Objects.equals(fresh.toString(), ctx.working.toString())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private record FieldRef(ConfigValueMeta meta, Component label, int topY) {
    }

    private static final class Scroller {
        private static final double SMOOTH_SPEED = 16.0;
        private static final double SNAP_EPSILON = 0.35;

        private double value;
        private double target;
        private int max;
        private long lastUpdateNanos = System.nanoTime();

        void setMaxScroll(int max) {
            this.max = Math.max(0, max);
            if (this.target > this.max) {
                this.target = this.max;
            }
            if (this.value > this.max) {
                this.value = this.max;
            }
            if (this.target < 0) {
                this.target = 0;
            }
            if (this.value < 0) {
                this.value = 0;
            }
        }

        void offset(double delta) {
            target = clamp(target + delta);
        }

        void update(float delta) {
            long now = System.nanoTime();
            double dt = (now - this.lastUpdateNanos) / 1_000_000_000.0;
            this.lastUpdateNanos = now;
            if (dt <= 0.0) {
                return;
            }

            double diff = target - value;
            if (Math.abs(diff) < SNAP_EPSILON) {
                value = target;
                return;
            }

            double alpha = 1.0 - Math.exp(-SMOOTH_SPEED * Math.min(dt, 0.05));
            value += diff * alpha;
            value = clamp(value);
        }

        int currentInt() {
            return (int) Math.round(value);
        }

        boolean hasScroll() {
            return max > 0;
        }

        private double clamp(double v) {
            if (v < 0) {
                return 0;
            }
            if (v > max) {
                return max;
            }
            return v;
        }
    }

    private record ConfigCtx(ResourceLocation id, ConfigUnit<Object> unit, ConfigCodec<Object> codec,
                             List<ConfigValueMeta> fields, JsonObject working, JsonObject defaults) {
    }

    /**
     * Returns the bottom y coordinate of the scrollable content area.
     *
     * @return the content bottom
     */
    public int getContentBottom() {
        return this.height - 32;
    }

    /**
     * Sets the tooltip to be rendered at the given position in the next frame.
     *
     * @param tooltip the tooltip component
     * @param mouseX  the mouse x position
     * @param mouseY  the mouse y position
     */
    public void setHoverTooltip(Component tooltip, int mouseX, int mouseY) {
        this.lastTooltip = tooltip;
        this.mouseXLast = mouseX;
        this.mouseYLast = mouseY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bottomBarTop = this.height - 32;
        if (mouseY >= bottomBarTop) {
            boolean handled = false;
            if (saveButton != null) {
                handled = saveButton.mouseClicked(mouseX, mouseY, button);
            }
            if (!handled && cancelButton != null) {
                handled = cancelButton.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        int sidebar = sidebarWidth();
        int sliderX = sidebar - 14;
        if (button == 0) {
            if (mouseX >= sliderX && mouseX <= sidebar && mouseY >= 0 && mouseY <= this.height) {
                sidebarExpanded = !sidebarExpanded;
                sideSlider.offset(sidebarExpanded ? sideExpandLimit - 14 : -(sideExpandLimit - 14));
                return true;
            }
            if (sidebarExpanded && mouseX >= 0 && mouseX <= sliderX && mouseY >= 0 && mouseY <= this.height) {
                int refX = 4;
                int refY = 8 - refScroller.currentInt();
                int h = this.font.lineHeight + 6;
                for (ResourceLocation modConfig : modConfigs) {
                    if (refY + h > this.height - 32) break;
                    if (mouseX >= refX && mouseX <= sliderX && mouseY >= refY && mouseY <= refY + h) {
                        var id = modConfig;
                        int offset = 0;
                        for (AbstractConfigEntry<?> entry : listWidget.children()) {
                            if (entry instanceof CategoryTextEntry cte) {
                                if (cte.getFieldName().getString().equals(Component.translatable("config." + id.getNamespace() + "." + id.getPath() + ".title").getString())) {
                                    listWidget.scrollTo(offset, true);
                                    break;
                                }
                            }
                            offset += entry.getItemHeight();
                        }
                        return true;
                    }
                    refY += h;
                }
            }
            if (listWidget != null && mouseX >= sliderX && mouseX <= this.width && mouseY >= contentTop && mouseY <= this.height - 32) {
                int yIt = listWidget.top - listWidget.getScrollOffset();
                for (AbstractConfigEntry<?> entry : listWidget.children()) {
                    int headerH = 20;
                    if (entry instanceof MapEntry me) {
                        if (mouseY >= yIt && mouseY <= yIt + headerH) {
                            if (me.toggleIfHit(mouseX, mouseY)) {
                                return true;
                            }
                        }
                        yIt += me.getItemHeight();
                    } else if (entry instanceof ListEntry le) {
                        if (mouseY >= yIt && mouseY <= yIt + headerH) {
                            if (le.toggleIfHit(mouseX, mouseY)) {
                                return true;
                            }
                        }
                        yIt += le.getItemHeight();
                    } else if (entry instanceof PathGroupEntry ge) {
                        if (mouseY >= yIt && mouseY <= yIt + headerH) {
                            if (ge.toggleIfHit(mouseX, mouseY)) {
                                setGroupExpanded(ge.stateKey(), ge.expanded());
                                init();
                                return true;
                            }
                        }
                        yIt += ge.getItemHeight();
                    } else {
                        yIt += entry.getItemHeight();
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Map<ResourceLocation, JsonObject> snapshotWorkingByConfig() {
        Map<ResourceLocation, JsonObject> snapshot = new HashMap<>();
        for (ConfigCtx ctx : contexts) {
            snapshot.put(ctx.id(), ctx.working().deepCopy());
        }
        return snapshot;
    }

    private void appendNestedFieldEntries(
            ResourceLocation configId,
            List<AbstractConfigEntry<?>> items,
            Map<AbstractConfigEntry<?>, JsonObject> defaultsMap,
            List<ConfigValueMeta> fields,
            JsonObject workingJson,
            JsonObject defaultsJson,
            String searchLower,
            int labelWidth,
            int fieldControlWidth,
            int listControlWidth
    ) {
        List<ConfigValueMeta> visible = fields.stream()
                .filter(meta -> !meta.hidden())
                .filter(meta -> {
                    if (searchLower.isEmpty()) {
                        return true;
                    }
                    String display = meta.translationKey()
                            .map(k -> Component.translatable(k).getString())
                            .orElse(meta.key());
                    return display.toLowerCase(Locale.ROOT).contains(searchLower);
                })
                .sorted(Comparator.comparing(ConfigValueMeta::key))
                .toList();

        Set<String> emittedGroups = new HashSet<>();
        Set<String> startedGroups = new HashSet<>();
        for (int idx = 0; idx < visible.size(); idx++) {
            ConfigValueMeta meta = visible.get(idx);
            String[] parts = meta.key().split("\\.");
            boolean ancestorsExpanded = true;
            StringBuilder pathBuilder = new StringBuilder();
            for (int depth = 0; depth < parts.length - 1; depth++) {
                if (depth > 0) {
                    pathBuilder.append('.');
                }
                pathBuilder.append(parts[depth]);
                String groupPath = pathBuilder.toString();
                if (emittedGroups.add(groupPath)) {
                    String stateKey = idToGroupStateKey(configId, groupPath);
                    boolean expanded = isGroupExpanded(stateKey);
                    String translationKey = "config." + configId.getNamespace() + "." + configId.getPath() + "." + groupPath;
                    items.add(new PathGroupEntry(stateKey, groupPath, Component.translatable(translationKey), depth, expanded));
                }
                ancestorsExpanded = ancestorsExpanded && isGroupExpanded(idToGroupStateKey(configId, groupPath));
                if (!ancestorsExpanded) {
                    break;
                }
            }
            if (!ancestorsExpanded) {
                continue;
            }

            if (parts.length > 1) {
                String parentPath = parentPathOf(meta.key());
                int parentDepth = depthOfPath(parentPath);
                if (parentPath != null && startedGroups.add(parentPath)) {
                    addDivider(items, parentDepth);
                    items.add(new EmptyEntry(NESTED_INNER_GAP));
                }
            }

            addFieldEntry(meta, items, defaultsMap, workingJson, defaultsJson, labelWidth, fieldControlWidth, listControlWidth);

            if (parts.length > 1) {
                String parentPath = parentPathOf(meta.key());
                String nextParentPath = idx + 1 < visible.size() ? parentPathOf(visible.get(idx + 1).key()) : null;
                if (!Objects.equals(parentPath, nextParentPath)) {
                    int parentDepth = depthOfPath(parentPath);
                    items.add(new EmptyEntry(NESTED_INNER_GAP));
                    addDivider(items, parentDepth);
                    items.add(new EmptyEntry(NESTED_OUTER_GAP));
                }
            }
        }
    }

    private void addFieldEntry(
            ConfigValueMeta meta,
            List<AbstractConfigEntry<?>> items,
            Map<AbstractConfigEntry<?>, JsonObject> defaultsMap,
            JsonObject workingJson,
            JsonObject defaultsJson,
            int labelWidth,
            int fieldControlWidth,
            int listControlWidth
    ) {
        var label = meta.translationKey().map(Component::translatable).orElse(Component.literal(meta.key()));
        var value = ConfigGuiUtil.getPath(workingJson, meta.key());
        if (value != null && value.isJsonArray()) {
            ListEntry listEntry = new ListEntry(meta.key(), label, workingJson, listControlWidth, rowHeight,
                    meta.tooltip().map(Component::translatable).orElse(null));
            items.add(listEntry);
            defaultsMap.put(listEntry, defaultsJson);
            return;
        }
        if (value != null && value.isJsonObject()) {
            MapEntry mapEntry = new MapEntry(meta.key(), label, workingJson, listControlWidth, rowHeight,
                    meta.tooltip().map(Component::translatable).orElse(null));
            items.add(mapEntry);
            defaultsMap.put(mapEntry, defaultsJson);
            return;
        }
        FieldEntry fieldEntry = new FieldEntry(meta, workingJson, labelWidth, fieldControlWidth, rowHeight);
        items.add(fieldEntry);
        defaultsMap.put(fieldEntry, defaultsJson);
    }

    private boolean isGroupExpanded(String stateKey) {
        return groupExpanded.getOrDefault(stateKey, true);
    }

    private void setGroupExpanded(String key, boolean expanded) {
        groupExpanded.put(key, expanded);
    }

    private void addDivider(List<AbstractConfigEntry<?>> items, int depth) {
        int inset = DIVIDER_BASE_INSET + Math.max(0, depth) * DIVIDER_NESTED_STEP;
        items.add(new DividerEntry(DIVIDER_HEIGHT, DIVIDER_COLOR, inset));
    }
}
