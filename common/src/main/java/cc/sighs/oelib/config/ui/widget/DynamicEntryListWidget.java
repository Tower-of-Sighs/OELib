package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * A smooth-scrolling list widget for the auto-generated config screen.
 *
 * <p>Extends {@link DynamicElementListWidget} with a frame-rate-independent
 * scrolling implementation that uses exponential smoothing for a natural
 * feel. The scroll bounds are recalculated on every frame based on the
 * total height of all child entries.
 */
public class DynamicEntryListWidget extends DynamicElementListWidget {
    /** The texture identifier for the vertical header separator. */
    public static final Identifier VERTICAL_HEADER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    private final Scroller scroller = new Scroller();

    /**
     * Constructs a scrolling list widget.
     *
     * @param minecraft the Minecraft instance
     * @param width     the total width
     * @param height    the total height
     * @param top       the top y coordinate of the visible area
     * @param bottom    the bottom y coordinate of the visible area
     */
    public DynamicEntryListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom);
    }

    /**
     * Scrolls to the given pixel offset.
     *
     * @param pixels   the target scroll offset
     * @param animated {@code true} for smooth scrolling, {@code false} for instant
     */
    public void scrollTo(int pixels, boolean animated) {
        scroller.scrollTo(pixels, animated);
    }

    /**
     * Offsets the scroll position by the given amount.
     *
     * @param pixels the offset, positive to scroll down
     */
    public void offset(double pixels) {
        scroller.offset(pixels);
    }

    /**
     * Returns the current scroll offset in pixels.
     *
     * @return the scroll offset
     */
    public int getScrollOffset() {
        return scroller.currentInt();
    }

    /**
     * Returns the target scroll offset (may differ during smooth scrolling).
     *
     * @return the target scroll offset
     */
    public int getScrollTargetOffset() {
        return scroller.targetInt();
    }

    /**
     * Recalculates the scroll bounds based on the total height of all
     * child entries.
     */
    public void refreshScrollBounds() {
        int totalHeight = 0;
        for (AbstractConfigEntry<?> entry : children) {
            totalHeight += entry.getItemHeight();
        }
        int maxScroll = Math.max(0, totalHeight - (bottom - top));
        scroller.setMax(maxScroll);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        refreshScrollBounds();
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

        int targetInt() {
            return (int) Math.round(target);
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
