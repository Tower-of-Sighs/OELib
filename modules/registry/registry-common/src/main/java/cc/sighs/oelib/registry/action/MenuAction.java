package cc.sighs.oelib.registry.action;

import cc.sighs.oelib.registry.api.ScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

public record MenuAction<H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>>(
        Supplier<? extends MenuType<H>> type,
        ScreenFactory<H, S> factory
) implements RegistrationAction {
    public MenuAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
    }
}