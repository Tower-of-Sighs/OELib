package cc.sighs.oelib.neoforge.example;

import cc.sighs.oelib.example.FluidRenderExampleMenu;
import cc.sighs.oelib.renderer.FluidRenderers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class FluidRenderExampleScreen extends AbstractContainerScreen<FluidRenderExampleMenu> {

    public FluidRenderExampleScreen(FluidRenderExampleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = 140;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0x66000000);

        long capacity = FluidType.BUCKET_VOLUME;
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        FluidStack lava = new FluidStack(Fluids.LAVA, 1000);

        int fluidWidth = 16;
        int fluidHeight = 64;
        int gap = 20;
        int totalWidth = fluidWidth * 2 + gap;
        int startX = left + (imageWidth - totalWidth) / 2;
        int startY = top + (imageHeight - fluidHeight) / 2;

        FluidRenderers.render(graphics, water, water.getAmount(), capacity,
                startX, startY, fluidWidth, fluidHeight);

        FluidRenderers.render(graphics, lava, lava.getAmount(), capacity,
                startX + fluidWidth + gap, startY, fluidWidth, fluidHeight);
    }
}