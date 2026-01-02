package cc.sighs.oelib.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import cc.sighs.oelib.network.serialization.NetFieldCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@NetworkPacket(
        modId = OELib.MODID,
        id = "open_gui",
        side = Side.SERVER
)
public record OpenGuiPacket(
        @NetFieldCodec(holder = BlockPos.class, field = "STREAM_CODEC")
        BlockPos pos
) implements INetworkPacket<OpenGuiPacket> {

    @Override
    public void handle(INetworkContext context) {
        if (!context.isServerSide()) {
            return;
        }
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
            }
        }
    }
}