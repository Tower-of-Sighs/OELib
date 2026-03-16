package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.fabric.util.mixin.hook.OELHookClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"lambda$onPress$0", "method_1611"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"),
            remap = false
    )
    private static boolean wrapMouseClicked(Screen screen, double d0, double d1, int i, Operation<Boolean> original) {
        if (OELHookClient.onScreenMouseClickedPre(screen, d0, d1, i)) {
            return true;
        }
        boolean handled = original.call(screen, d0, d1, i);
        return OELHookClient.onScreenMouseClickedPost(screen, d0, d1, i, handled);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"lambda$onPress$1", "method_1605"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"),
            remap = false
    )
    private static boolean wrapMouseReleased(Screen screen, double d0, double d1, int i, Operation<Boolean> original) {
        if (OELHookClient.onScreenMouseReleasedPre(screen, d0, d1, i)) {
            return true;
        }
        boolean handled = original.call(screen, d0, d1, i);
        return OELHookClient.onScreenMouseReleasedPost(screen, d0, d1, i, handled);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"method_1602", "lambda$onMove$11"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"),
            remap = false
    )
    private boolean wrapMouseDragged(Screen screen, double mouseX, double mouseY, int button, double dragX, double dragY, Operation<Boolean> original) {
        if (OELHookClient.onScreenMouseDragPre(screen, mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        boolean handled = original.call(screen, mouseX, mouseY, button, dragX, dragY);
        if (handled) {
            return true;
        }
        OELHookClient.onScreenMouseDragPost(screen, mouseX, mouseY, button, dragX, dragY);
        return false;
    }

    // FORGE: Allows for Horizontal Scroll to be recognized as Vertical Scroll - Fixes MC-121772
    @ModifyVariable(method = "onScroll", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double handleOSXHorizontalScroll(double verticalDelta, long window, double horizontalDelta) {
        if (Minecraft.ON_OSX && verticalDelta == 0) {
            return horizontalDelta;
        }
        return verticalDelta;
    }

    @WrapOperation(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z")
    )
    private boolean wrapScreenScroll(Screen screen, double mouseX, double mouseY, double delta, Operation<Boolean> original, @Local(ordinal = 0, argsOnly = true) double f) {
        if (OELHookClient.onScreenMouseScrollPre((MouseHandler) (Object) this, this.minecraft.screen, f)) {
            return true;
        }

        boolean handled = original.call(screen, mouseX, mouseY, delta);
        if (handled) {
            return true;
        }

        OELHookClient.onScreenMouseScrollPost((MouseHandler) (Object) this, this.minecraft.screen, f);

        return false;
    }

    @Inject(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            cancellable = true
    )
    private void onInGameMouseScroll(long window, double x, double y, CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) double f) {
        if (OELHookClient.onMouseScroll((MouseHandler) (Object) this, f)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onPress(JIII)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0, shift = At.Shift.BEFORE),
            cancellable = true
    )
    private void injectMouseButtonPre(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (OELHookClient.onMouseButtonPre(button, action, modifiers)) ci.cancel();
    }

    @Inject(method = "onPress", at = @At("TAIL"))
    private void injectMouseButtonPost(long window, int button, int action, int modifiers, CallbackInfo ci) {

        if (window == this.minecraft.getWindow().getWindow()) {
            OELHookClient.onMouseButtonPost(button, action, modifiers);
        }
    }
}
