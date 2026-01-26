package cc.sighs.oelib.renderer.spi;

import net.minecraft.client.gui.GuiGraphics;

public interface IFluidRenderers {
    void render(GuiGraphics graphics, Object fluidRef, long amount, long capacity,
                int x, int y, int width, int height);
}