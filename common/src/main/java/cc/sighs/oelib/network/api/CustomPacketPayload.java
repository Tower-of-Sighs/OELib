package cc.sighs.oelib.network.api;

import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    default ResourceLocation id() {
        return type().id();
    }

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
