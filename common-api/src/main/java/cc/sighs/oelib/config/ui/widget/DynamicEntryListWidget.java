package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class DynamicEntryListWidget extends DynamicElementListWidget {
    public static final ResourceLocation VERTICAL_HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private final Scroller scroller = new Scroller();

    public DynamicEntryListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom);
    }

    public void scrollTo(int pixels, boolean animated) {
        scroller.setTarget(pixels, animated ? 10 : 0);
    }

    public void offset(double pixels) {
        scroller.setTarget(scroller.currentInt() + (int) pixels, 10);
    }

    public int getScrollOffset() {
        return scroller.currentInt();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        scroller.update(delta);
        int y = top - scroller.currentInt();
        int totalHeight = 0;
        for (AbstractConfigEntry<?> entry : children) totalHeight += entry.getItemHeight();
        int maxScroll = Math.max(0, totalHeight - (bottom - top));
        scroller.setMax(maxScroll);
        for (int i = 0; i < children.size(); i++) {
            AbstractConfigEntry<?> entry = children.get(i);
            int h = entry.getItemHeight();
            boolean hovered = mouseY >= y && mouseY < y + h && mouseX >= left && mouseX < left + width;
            entry.render(graphics, i, y, left + 12, getItemWidth(), h, mouseX, mouseY, hovered, delta);
            y += h;
        }
    }

    private static final class Scroller {
        private double value;
        private double target;
        private int max;
        private long duration;
        private long start;

        void setMax(int max) {
            this.max = Math.max(0, max);
            clampAll();
        }

        void setTarget(int t, long duration) {
            this.target = t;
            this.duration = duration;
            this.start = System.currentTimeMillis();
            clampAll();
        }

        void update(float delta) {
            double diff = target - value;
            if (duration <= 0 || Math.abs(diff) < 0.5) {
                value = target;
                return;
            }
            double factor = Math.min(1.0, (System.currentTimeMillis() - start) / (double) duration);
            value = value + diff * factor;
        }

        int currentInt() {
            return (int) Math.round(value);
        }

        private double clamp(double v) {
            if (v < 0) return 0;
            if (max > 0 && v > max) return max;
            return v;
        }

        private void clampAll() {
            target = clamp(target);
            value = clamp(value);
        }
    }
}
