package cc.sighs.oelib.registry.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;

public interface ExtendedMenuProvider extends MenuProvider {
    void saveExtraData(FriendlyByteBuf buf);
}
