package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public class ConfigHotReloadListeners {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor)
                -> CompletableFuture.runAsync(() -> {
                }, backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenRunAsync(() -> {
                    OELib.LOGGER.info("Hot-reloading configurations via Minecraft reload...");
                    ConfigManager.reloadAll();
                }, gameExecutor));
    }
}