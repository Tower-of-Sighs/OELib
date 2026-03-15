package cc.sighs.oelib.fabric.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.LogicalSide;
import cc.sighs.oelib.event.events.TickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public final class TickBridge {
    private TickBridge() {}

    public static void registerServer() {
        ServerTickEvents.START_SERVER_TICK.register(TickBridge::onServerStart);
        ServerTickEvents.END_SERVER_TICK.register(TickBridge::onServerEnd);
        ServerTickEvents.START_WORLD_TICK.register(TickBridge::onWorldStart);
        ServerTickEvents.END_WORLD_TICK.register(TickBridge::onWorldEnd);
    }

    public static void registerClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> EventBus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EventBus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            float delta = Minecraft.getInstance().getDeltaFrameTime();
            EventBus.post(new TickEvent.RenderTickEvent(TickEvent.Phase.END, delta));
        });
    }

    private static void onServerStart(MinecraftServer server) {
        EventBus.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, () -> true, server));
        server.getPlayerList().getPlayers().forEach(p ->
                EventBus.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p)));
    }

    private static void onServerEnd(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(p ->
                EventBus.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p)));
        EventBus.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
    }

    private static void onWorldStart(Level level) {
        EventBus.post(new TickEvent.LevelTickEvent(LogicalSide.SERVER, TickEvent.Phase.START, level, () -> true));
    }

    private static void onWorldEnd(Level level) {
        EventBus.post(new TickEvent.LevelTickEvent(LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
    }
}

