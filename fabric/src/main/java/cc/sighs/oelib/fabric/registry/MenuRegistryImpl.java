package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import cc.sighs.oelib.registry.spi.IMenuRegistry;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class MenuRegistryImpl implements IMenuRegistry {
    @Override
    public <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory) {
        return new ExtendedScreenHandlerType<>((syncId, inventory, data) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            T menu = factory.create(syncId, inventory, buf);
            buf.release();
            return menu;
        }, ByteBufCodecs.BYTE_ARRAY.mapStream(Function.identity()));
    }

    @Override
    public void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider) {
        player.openMenu(new ExtendedScreenHandlerFactory<byte[]>() {
            @Override
            public byte[] getScreenOpeningData(ServerPlayer player) {
                var buf = PacketByteBufs.create();
                provider.saveExtraData(buf);
                byte[] bytes = ByteBufUtil.getBytes(buf);
                buf.release();
                return bytes;
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
