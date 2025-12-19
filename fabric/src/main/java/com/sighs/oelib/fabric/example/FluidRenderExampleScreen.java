package com.sighs.oelib.fabric.example;

import com.sighs.oelib.renderer.FluidRenderers;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;

public class FluidRenderExampleScreen extends Screen {
    public FluidRenderExampleScreen() {
        super(Component.literal("OELib Fluid Render Example"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int w = 240, h = 140;
        int left = (this.width - w) / 2;
        int top = (this.height - h) / 2;

        graphics.fill(left, top, left + w, top + h, 0x66000000);

        long capacity = 1000;
        long amount = 1000;

        int fluidWidth = 16;
        int fluidHeight = 64;
        int gap = 20;
        int totalWidth = fluidWidth * 2 + gap;
        int startX = left + (w - totalWidth) / 2;
        int startY = top + (h - fluidHeight) / 2;

        FluidVariant water = FluidVariant.of(Fluids.WATER);
        FluidRenderers.render(graphics, water, amount, capacity, startX, startY, fluidWidth, fluidHeight);

        FluidVariant lava = FluidVariant.of(Fluids.LAVA);
        FluidRenderers.render(graphics, lava, amount, capacity, startX + fluidWidth + gap, startY, fluidWidth, fluidHeight);
    }
}