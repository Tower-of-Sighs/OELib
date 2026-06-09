package cc.sighs.oelib.misc.fabric;

import cc.sighs.oelib.misc.OELibMisc;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class OELibMiscFabric implements ModInitializer {

    private static MinecraftServer currentServer = null;

    @Override
    public void onInitialize() {
        OELibMisc.init();
        registerEvents();
    }

    private static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            currentServer = null;
        });
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }
}
