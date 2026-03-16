package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.fabric.util.mixin.hook.OELHookClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"lambda$keyPress$3", "method_1454"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z")
    )
    private static boolean wrapKeyPressed(Screen instance, int keyCode, int scanCode, int modifiers, Operation<Boolean> original) {
        boolean handled = OELHookClient.onScreenKeyPressedPre(instance, keyCode, scanCode, modifiers);

        if (!handled) {
            handled = original.call(instance, keyCode, scanCode, modifiers);
        }

        if (!handled) {
            handled = OELHookClient.onScreenKeyPressedPost(instance, keyCode, scanCode, modifiers);
        }

        return handled;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"lambda$keyPress$3", "method_1454"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(III)Z")
    )
    private static boolean wrapKeyReleased(Screen instance, int keyCode, int scanCode, int modifiers, Operation<Boolean> original) {
        boolean handled = OELHookClient.onScreenKeyReleasedPre(instance, keyCode, scanCode, modifiers);

        if (!handled) {
            handled = original.call(instance, keyCode, scanCode, modifiers);
        }

        if (!handled) {
            handled = OELHookClient.onScreenKeyReleasedPost(instance, keyCode, scanCode, modifiers);
        }

        return handled;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"method_1458", "lambda$charTyped$5"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/GuiEventListener;charTyped(CI)Z")
    )
    private static boolean wrapCharTypedSingle(GuiEventListener listener, char codePoint, int modifiers, Operation<Boolean> original) {
        return wrapCharTyped(listener, codePoint, modifiers, original);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = {"method_1473", "lambda$charTyped$6"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/GuiEventListener;charTyped(CI)Z")
    )
    private static boolean wrapCharTypedLoop(GuiEventListener listener, char codePoint, int modifiers, Operation<Boolean> original) {
        return wrapCharTyped(listener, codePoint, modifiers, original);
    }

    private static boolean wrapCharTyped(GuiEventListener listener, char codePoint, int modifiers, Operation<Boolean> original) {
        if (listener instanceof Screen screen) {
            if (OELHookClient.onScreenCharTypedPre(screen, codePoint, modifiers)) {
                return true;
            }
            if (original.call(listener, codePoint, modifiers)) {
                return true;
            }
            OELHookClient.onScreenCharTypedPost(screen, codePoint, modifiers);
            return false;
        }

        return original.call(listener, codePoint, modifiers);
    }

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void injectKeyInput(long l, int i, int j, int k, int m, CallbackInfo ci) {
        if (l == Minecraft.getInstance().getWindow().getWindow()) {
            OELHookClient.onKeyInput(i, j, k, m);
        }
    }
}
