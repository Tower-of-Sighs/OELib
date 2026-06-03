package cc.sighs.oelib.misc.fabric.mixin;

import cc.sighs.oelib.OELibMisc;
import cc.sighs.oelib.misc.icon.DynamicIconRegistry;
import com.mojang.blaze3d.platform.NativeImage;
import com.terraformersmc.modmenu.util.mod.fabric.FabricIconHandler;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Mixin(value = FabricIconHandler.class, remap = false)
public abstract class FabricIconHandlerMixin {

    @Shadow
    abstract DynamicTexture getCachedModIcon(Path path);

    @Shadow
    abstract void cacheModIcon(Path path, DynamicTexture tex);

    @Inject(method = "createIcon", at = @At("HEAD"), cancellable = true)
    private void oelib$createIcon(ModContainer iconSource, String iconPath, CallbackInfoReturnable<DynamicTexture> cir) {
        String modId = iconSource.getMetadata().getId();
        Optional<String> selected = DynamicIconRegistry.getSelectedFabricPath(modId);
        if (selected.isEmpty()) return;

        Optional<Path> pathOpt = iconSource.findPath(selected.get());
        if (pathOpt.isEmpty()) return;

        Path path = pathOpt.get();
        DynamicTexture cachedIcon = getCachedModIcon(path);
        if (cachedIcon != null) {
            cir.setReturnValue(cachedIcon);
            return;
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));
            if (image.getHeight() != image.getWidth()) return;
            DynamicTexture tex = new DynamicTexture(image);
            cacheModIcon(path, tex);
            cir.setReturnValue(tex);
            OELibMisc.LOGGER.info("[OELib: DynamicIcon] Set icon for {}: {}", modId, selected.get());
        } catch (Throwable ignored) {
        }
    }
}