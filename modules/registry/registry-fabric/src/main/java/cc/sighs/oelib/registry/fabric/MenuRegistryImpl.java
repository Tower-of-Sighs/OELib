package cc.sighs.oelib.registry.fabric;

import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import cc.sighs.oelib.registry.spi.IMenuRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class MenuRegistryImpl implements IMenuRegistry {

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory) {
        return new ExtendedScreenHandlerType<>(factory::create);
    }

    @Override
    public void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                provider.saveExtraData(buf);
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return provider.createMenu(i, inventory, player);
            }
        });
    }
}
