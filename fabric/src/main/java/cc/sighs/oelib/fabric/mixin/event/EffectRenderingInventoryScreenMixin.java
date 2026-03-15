package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.fabric.util.mixin.hook.OELHookClient;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {
    @Inject(
            method = "renderEffects",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;size()I", ordinal = 0),
            cancellable = true
    )
    private void hookPotionSize(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci,
                                @Local(ordinal = 0) LocalIntRef iRef,
                                @Local(ordinal = 1) int j,
                                @Local(ordinal = 0) LocalBooleanRef flagRef
    ) {
        var event = OELHookClient.onScreenPotionSize(
                (EffectRenderingInventoryScreen<?>) (Object) this,
                j,
                !flagRef.get(),
                iRef.get()
        );

        if (event.isCanceled()) {
            ci.cancel();
            return;
        }

        flagRef.set(!event.isCompact());
        iRef.set(event.getHorizontalOffset());
    }
}