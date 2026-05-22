package cc.sighs.oelib.registry.spi;

import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface IMenuRegistry {
    <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory);
    void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider);
}
