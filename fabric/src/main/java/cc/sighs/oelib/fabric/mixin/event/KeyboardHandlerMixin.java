package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.fabric.util.mixin.hook.OELHookClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Redirect(
            method = "method_1454",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z")
    )
    private static boolean redirectKeyPressed(Screen instance, int keyCode, int scanCode, int modifiers) {
        boolean handled = OELHookClient.onScreenKeyPressedPre(instance, keyCode, scanCode, modifiers);

        if (!handled) {
            handled = instance.keyPressed(keyCode, scanCode, modifiers);
        }

        if (!handled) {
            handled = OELHookClient.onScreenKeyPressedPost(instance, keyCode, scanCode, modifiers);
        }

        return handled;
    }

    @Redirect(
            method = "method_1454",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(III)Z")
    )
    private static boolean redirectKeyReleased(Screen instance, int keyCode, int scanCode, int modifiers) {
        boolean handled = OELHookClient.onScreenKeyReleasedPre(instance, keyCode, scanCode, modifiers);

        if (!handled) {
            handled = instance.keyReleased(keyCode, scanCode, modifiers);
        }

        if (!handled) {
            handled = OELHookClient.onScreenKeyReleasedPost(instance, keyCode, scanCode, modifiers);
        }

        return handled;
    }

    @Redirect(
            method = "method_1458",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/GuiEventListener;charTyped(CI)Z")
    )
    private static boolean redirectCharTypedSingle(GuiEventListener listener, char codePoint, int modifiers) {
        return OELHookClient.wrapCharTypedWithEvents(listener, codePoint, modifiers);
    }

    @Redirect(
            method = "method_1473",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/GuiEventListener;charTyped(CI)Z")
    )
    private static boolean redirectCharTypedLoop(GuiEventListener listener, char codePoint, int modifiers) {
        return OELHookClient.wrapCharTypedWithEvents(listener, codePoint, modifiers);
    }

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void injectKeyInput(long l, int i, int j, int k, int m, CallbackInfo ci) {
        OELHookClient.onKeyInput(i, j, k, m);
    }
}