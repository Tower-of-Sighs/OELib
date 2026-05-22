package cc.sighs.oelib.registry.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface ScreenFactory<H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> {
    S create(H containerMenu, Inventory inventory, Component component);
}