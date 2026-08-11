package cc.sighs.oelib.misc.neoforge.mixin;

import cc.sighs.oelib.misc.OELibMisc;
import cc.sighs.oelib.misc.icon.DynamicIconRegistry;
import com.flechazo.hkt.business.util.OptionalOps;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(value = ModListScreen.class)
public abstract class NeoForgeIconHandlerMixin {

    @Redirect(
            method = "updateCache",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforgespi/language/IModInfo;getLogoFile()Ljava/util/Optional;"
            )
    )
    private Optional<String> oelib$redirectLogo(IModInfo info) {
        String modId = info.getModId();

        return OptionalOps.fromMaybe(DynamicIconRegistry.findSelectedNeoForgePath(modId)
                .peek(path -> OELibMisc.LOGGER.info("[OELib: DynamicIcon] Set icon for {}: {}", modId, path))
                .or(() -> OptionalOps.toMaybe(info.getLogoFile())));
    }
}

