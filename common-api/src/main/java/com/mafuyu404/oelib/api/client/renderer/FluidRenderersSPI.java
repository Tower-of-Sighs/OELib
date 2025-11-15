package com.mafuyu404.oelib.api.client.renderer;

import net.minecraft.client.gui.GuiGraphics;

public interface FluidRenderersSPI {
    void render(GuiGraphics graphics, Object fluidRef, long amount, long capacity,
                int x, int y, int width, int height);
}