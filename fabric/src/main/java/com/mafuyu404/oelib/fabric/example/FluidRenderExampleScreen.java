package com.mafuyu404.oelib.fabric.example;

import com.mafuyu404.oelib.client.renderer.FluidRenderers;
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
        FluidVariant water = FluidVariant.of(Fluids.WATER);
        FluidRenderers.render(graphics, water, amount, capacity, left + 50, top + 50, 16, 16);

        FluidVariant lava = FluidVariant.of(Fluids.LAVA);
        FluidRenderers.render(graphics, lava, amount, capacity, left + 1, top + 1, 16, 16);
    }
}