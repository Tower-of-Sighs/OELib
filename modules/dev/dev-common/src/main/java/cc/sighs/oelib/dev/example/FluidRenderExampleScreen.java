package cc.sighs.oelib.dev.example;

import cc.sighs.oelib.renderer.FluidRenderers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0x66000000);

        long capacity = 1000;
        long amount = 1000;

        int fluidWidth = 16;
        int fluidHeight = 64;
        int gap = 20;
        int totalWidth = fluidWidth * 2 + gap;
        int startX = left + (imageWidth - totalWidth) / 2;
        int startY = top + (imageHeight - fluidHeight) / 2;

        FluidRenderers.render(guiGraphics, FluidRenderers.of(Fluids.WATER), amount, capacity,
                startX, startY, fluidWidth, fluidHeight);

        FluidRenderers.render(guiGraphics, FluidRenderers.of(Fluids.LAVA), amount, capacity,
                startX + fluidWidth + gap, startY, fluidWidth, fluidHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
