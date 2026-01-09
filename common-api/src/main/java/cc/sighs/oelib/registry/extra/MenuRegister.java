package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.MenuAction;
import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import cc.sighs.oelib.registry.api.ScreenFactory;
import cc.sighs.oelib.registry.spi.IMenuRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MenuRegister {

    private static final IMenuRegistry IMPL = ServiceLoader
            .load(IMenuRegistry.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IMenuRegistry impl found"));

    private MenuRegister() {
    }

    public static <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void registerScreenFactory(Supplier<? extends MenuType<H>> type, ScreenFactory<H, S> factory) {
        RegistrationDispatcher.perform(new MenuAction<>(type, factory));
    }

    public static void openExtendedMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> bufWriter) {
        openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                bufWriter.accept(buf);
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

    public static void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider) {
        IMPL.openExtendedMenu(player, provider);
    }

    public static void openMenu(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory) {
        return IMPL.ofExtended(factory);
    }
}
