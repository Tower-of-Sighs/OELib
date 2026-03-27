package cc.sighs.oelib.fabric.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.events.ClientTickEvent;
import cc.sighs.oelib.event.events.LevelTickEvent;
import cc.sighs.oelib.event.events.ServerTickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
        ClientTickEvents.START_CLIENT_TICK.register(client -> EventBus.post(new ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EventBus.post(new ClientTickEvent.Post());
        });
    }

    private static void onServerStart(MinecraftServer server) {
        EventBus.post(new ServerTickEvent.Pre(() -> true, server));

    }

    private static void onServerEnd(MinecraftServer server) {
        EventBus.post(new ServerTickEvent.Post(() -> true, server));
    }

    private static void onWorldStart(Level level) {
        EventBus.post(new LevelTickEvent.Pre(() -> true, level));
    }

    private static void onWorldEnd(Level level) {
        EventBus.post(new LevelTickEvent.Post(() -> true, level));
    }
}

