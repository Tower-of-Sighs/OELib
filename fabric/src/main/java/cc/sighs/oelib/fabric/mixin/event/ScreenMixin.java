package cc.sighs.oelib.fabric.mixin.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.events.ScreenEvent;
import cc.sighs.oelib.fabric.util.mixin.IScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin implements IScreen {
    @Shadow
    @Final
    public List<Renderable> renderables;
    @Shadow
    @Final
    private List<GuiEventListener> children;
    @Shadow
    @Final
    private List<NarratableEntry> narratables;

    @Shadow
    public abstract void removeWidget(GuiEventListener widget);

    @WrapOperation(
            method = {"init(Lnet/minecraft/client/Minecraft;II)V", "rebuildWidgets()V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V")
    )
    private void wrapInitCall(Screen instance, Operation<Void> original) {
        if (!EventBus.post(new ScreenEvent.Init.Pre(
                instance,
                this.children,
                this::oelib$addEventWidget,
                this::removeWidget
        ))) {
            original.call(instance);
        }
        EventBus.post(new ScreenEvent.Init.Post(
                instance,
                this.children,
                this::oelib$addEventWidget,
                this::removeWidget
        ));
    }

    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V", shift = At.Shift.AFTER))
    private void renderBgEvent(GuiGraphics guiGraphics, CallbackInfo ci) {
        var event = new ScreenEvent.BackgroundRendered((Screen) (Object) this, guiGraphics);
        EventBus.post(event);
    }

    @Inject(method = "renderDirtBackground", at = @At(value = "RETURN"))
    private void renderDBgEvent(GuiGraphics guiGraphics, CallbackInfo ci) {
        var event = new ScreenEvent.BackgroundRendered((Screen) (Object) this, guiGraphics);
        EventBus.post(event);
    }

    @Unique
    @Override
    public void oelib$addEventWidget(GuiEventListener b) {
        if (b instanceof Renderable r)
            this.renderables.add(r);
        if (b instanceof NarratableEntry ne)
            this.narratables.add(ne);
        children.add(b);
    }
}
