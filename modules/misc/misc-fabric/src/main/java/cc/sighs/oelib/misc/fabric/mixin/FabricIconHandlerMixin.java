package cc.sighs.oelib.misc.fabric.mixin;

import cc.sighs.oelib.misc.OELibMisc;
import cc.sighs.oelib.misc.icon.DynamicIconRegistry;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.util.OptionalOps;
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
import java.util.concurrent.Callable;

@Mixin(value = FabricIconHandler.class, remap = false)
public abstract class FabricIconHandlerMixin {

    @Shadow
    abstract DynamicTexture getCachedModIcon(Path path);

    @Shadow
    abstract void cacheModIcon(Path path, DynamicTexture tex);

    @Inject(method = "createIcon", at = @At("HEAD"), cancellable = true)
    private void oelib$createIcon(ModContainer iconSource, String iconPath, CallbackInfoReturnable<DynamicTexture> cir) {
        String modId = iconSource.getMetadata().getId();
        Pathway.maybe(DynamicIconRegistry.findSelectedFabricPath(modId))
                .via(selected -> Pathway.maybe(OptionalOps.toMaybe(iconSource.findPath(selected)))
                        .map(path -> new ResolvedIcon(selected, path)))
                .via(this::resolveTexture)
                .peek(icon -> {
                    cir.setReturnValue(icon.texture());
                    OELibMisc.LOGGER.info("[OELib: DynamicIcon] Set icon for {}: {}", modId, icon.selectedPath());
                });
    }

    private MaybePath<LoadedIcon> resolveTexture(ResolvedIcon icon) {
        return Pathway.nullable(getCachedModIcon(icon.path()))
                .orElse(() -> loadTexture(icon.path()).peek(texture -> cacheModIcon(icon.path(), texture)))
                .map(texture -> new LoadedIcon(icon.selectedPath(), texture));
    }

    private MaybePath<DynamicTexture> loadTexture(Path path) {
        return Pathway.ioAutoResource((Callable<InputStream>) () -> Files.newInputStream(path))
                .use(inputStream -> Pathway.ioAutoResource((Callable<NativeImageLease>) () ->
                                new NativeImageLease(NativeImage.read(inputStream)))
                        .useSync(NativeImageLease::transferToTexture))
                .toTryPath()
                .peekFailure(error -> OELibMisc.LOGGER.debug("Failed to load dynamic icon {}", path, error))
                .toMaybePath()
                .via(Pathway::maybe);
    }

    private record ResolvedIcon(String selectedPath, Path path) {
    }

    private record LoadedIcon(String selectedPath, DynamicTexture texture) {
    }

    private static final class NativeImageLease implements AutoCloseable {
        private final NativeImage image;
        private boolean transferred;

        private NativeImageLease(NativeImage image) {
            this.image = image;
        }

        private Maybe<DynamicTexture> transferToTexture() {
            if (image.getHeight() != image.getWidth()) {
                return Maybe.none();
            }
            DynamicTexture texture = new DynamicTexture(image);
            transferred = true;
            return Maybe.some(texture);
        }

        @Override
        public void close() {
            if (!transferred) {
                image.close();
            }
        }
    }
}
