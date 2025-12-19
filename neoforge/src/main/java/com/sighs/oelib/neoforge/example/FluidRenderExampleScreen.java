package com.sighs.oelib.neoforge.example;

import com.sighs.oelib.renderer.FluidRenderers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

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

        graphics.fill(left, top, left + w, top + h, 0x66000000); // 半透明背景

        long capacity = FluidType.BUCKET_VOLUME;
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        FluidStack lava = new FluidStack(Fluids.LAVA, 1000);

        int fluidWidth = 16;
        int fluidHeight = 64;
        int gap = 20;
        int totalWidth = fluidWidth * 2 + gap;
        int startX = left + (w - totalWidth) / 2;
        int startY = top + (h - fluidHeight) / 2;

        FluidRenderers.render(graphics, water, water.getAmount(), capacity, startX, startY, fluidWidth, fluidHeight);
        FluidRenderers.render(graphics, lava, lava.getAmount(), capacity, startX + fluidWidth + gap, startY, fluidWidth, fluidHeight);
    }
}