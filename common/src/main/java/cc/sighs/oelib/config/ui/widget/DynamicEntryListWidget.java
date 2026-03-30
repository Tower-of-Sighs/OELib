package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class DynamicEntryListWidget extends DynamicElementListWidget {
    public static final Identifier VERTICAL_HEADER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    private final Scroller scroller = new Scroller();

    public DynamicEntryListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom);
    }

    public void scrollTo(int pixels, boolean animated) {
        scroller.scrollTo(pixels, animated);
    }

    public void offset(double pixels) {
        scroller.offset(pixels);
    }

    public int getScrollOffset() {
        return scroller.currentInt();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int totalHeight = 0;
        for (AbstractConfigEntry<?> entry : children) totalHeight += entry.getItemHeight();
        int maxScroll = Math.max(0, totalHeight - (bottom - top));
        scroller.setMax(maxScroll);
        scroller.update();
        int y = top - scroller.currentInt();
        for (int i = 0; i < children.size(); i++) {
            AbstractConfigEntry<?> entry = children.get(i);
            int h = entry.getItemHeight();
            boolean hovered = mouseY >= y && mouseY < y + h && mouseX >= left && mouseX < left + width
                    && mouseY >= top && mouseY < bottom;
            entry.render(graphics, i, y, left + 12, getItemWidth(), h, mouseX, mouseY, hovered, delta);
            y += h;
        }
    }

    private static final class Scroller {
        private static final double SMOOTH_SPEED = 18.0;
        private static final double SNAP_EPSILON = 0.35;

        private double value;
        private double target;
        private int max;
        private long lastUpdateNanos = System.nanoTime();

        void setMax(int max) {
            this.max = Math.max(0, max);
            clampAll();
        }

        void scrollTo(double t, boolean animated) {
            this.target = t;
            if (!animated) {
                this.value = t;
            }
            clampAll();
        }

        void offset(double delta) {
            this.target = clamp(this.target + delta);
        }

        void update() {
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

        private double clamp(double v) {
            if (v < 0) return 0;
            if (v > max) return max;
            return v;
        }

        private void clampAll() {
            target = clamp(target);
            value = clamp(value);
        }
    }
}
