package cc.sighs.oelib.neoforge.registry;

import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import cc.sighs.oelib.registry.spi.IMenuRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public class MenuRegistryImpl implements IMenuRegistry {
    @Override
    public <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider) {
        player.openMenu(provider, provider::saveExtraData);
    }
}
