package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.events.ScreenEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin{
    @Shadow
    @Nullable
    public Screen screen;

    @WrapOperation(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V")
    )
    private void cancelDefaultRemoved(Screen instance, Operation<Void> original) {
    }

    @Inject(
            method = "setScreen",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0, opcode = Opcodes.GETFIELD),
            cancellable = true
    )
    private void forgeLikeScreenEvent(Screen screen, CallbackInfo ci) {
        Screen old = this.screen;

        if (screen != null) {
            var event = new ScreenEvent.Opening(old, screen);
            EventBus.post(event);
            if (event.isCanceled()) {
                ci.cancel();
                return;
            }
        }

        if (old != null && screen != old) {
            EventBus.post(new ScreenEvent.Closing(old));
            old.removed();
        }
    }

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen handleScreenReplacement(Screen newScreen) {
        return newScreen;
    }
}
