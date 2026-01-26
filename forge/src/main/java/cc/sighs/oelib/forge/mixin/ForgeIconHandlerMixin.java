package cc.sighs.oelib.forge.mixin;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.icon.DynamicIconRegistry;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.forgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(value = ModListScreen.class)
public abstract class ForgeIconHandlerMixin {

    @Redirect(
            method = "updateCache",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/forgespi/language/IModInfo;getLogoFile()Ljava/util/Optional;"
            )
    )
    private Optional<String> oelib$redirectLogo(IModInfo info) {
        String modId = info.getModId();

        Optional<String> dynamic = DynamicIconRegistry.getSelectedForgePath(modId);
        if (dynamic.isPresent()) {
            OELib.LOGGER.info("[OELib: DynamicIcon] Set icon for {}: {}", modId, dynamic.get());
            return dynamic;
        }

        return info.getLogoFile();
    }
}

