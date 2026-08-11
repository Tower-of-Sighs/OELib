package cc.sighs.oelib.dev.example.net;

import cc.sighs.oelib.dev.OELibDev;
import cc.sighs.oelib.dev.example.ExampleBlock;
import cc.sighs.oelib.dev.example.FluidRenderExampleMenu;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

@NetworkPacket(
        modId = OELibDev.MOD_ID,
        id = "open_gui",
        side = Side.SERVER
)
public record OpenGuiPacket(
        BlockPos pos,
        Component title,
        ResourceLocation enchantmentId
) implements INetworkPacket<OpenGuiPacket> {

    @Override
    public void handle(INetworkContext context) {
        context.enqueueWork(() -> {
                    ServerPlayer player = context.sender();
                    if (player != null) {
                        double distanceSq = player.distanceToSqr(
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5
                        );

                        if (distanceSq <= 4.0 &&
                                player.level().getBlockState(pos).getBlock() instanceof ExampleBlock) {
                            player.openMenu(new FluidRenderExampleMenu.Provider(pos));
                            String enchantmentName = "unknown";
                            if (enchantmentId != null) {
                                enchantmentName = enchantmentId.toString();

                            } else {
                                OELibDev.LOGGER.warn("Received OpenGuiPacket with null enchantment ID!");
                            }

                            OELibDev.LOGGER.info("Open fluid example: {} with enchantment {}",
                                    title.getString(), enchantmentName);
                        }
                    }
                }
        );
    }
}