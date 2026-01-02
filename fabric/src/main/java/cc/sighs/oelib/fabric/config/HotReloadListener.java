package cc.sighs.oelib.fabric.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HotReloadListener implements SimpleResourceReloadListener<Void> {

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(OELib.MODID, "config_hot_reload");
    }

    @Override
    public CompletableFuture<Void> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> apply(Void data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(ConfigManager::reloadAll, executor);
    }
}
