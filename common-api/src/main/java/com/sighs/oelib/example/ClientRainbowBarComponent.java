package com.sighs.oelib.example;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class ClientRainbowBarComponent implements ClientTooltipComponent {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 2;

    public ClientRainbowBarComponent(RainbowBarComponent ignored) {
    }

    @Override
    public int getHeight() {
        return BAR_HEIGHT + 2;
    }

    @Override
    public int getWidth(Font font) {
        return BAR_WIDTH + 4;
    }

    @Override
    public void renderImage(Font font, int tooltipX, int tooltipY, GuiGraphics guiGraphics) {
        int x0 = tooltipX + 2;
        int y0 = tooltipY + 1;

        float time = (System.currentTimeMillis() % 10_000L) / 10_000.0f;
        float speed = 0.5f;

        for (int x = 0; x < BAR_WIDTH; x++) {
            float hue = (x / (float) BAR_WIDTH + time * speed) % 1.0f;

            int rgb = Mth.hsvToRgb(
                    hue,
                    1.0f,
                    1.0f
            );

            guiGraphics.fill(
                    x0 + x,
                    y0,
                    x0 + x + 1,
                    y0 + BAR_HEIGHT,
                    0xFF000000 | rgb
            );
        }
    }
}