package cc.sighs.oelib.fabric.util.mixin.hook;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.Result;
import cc.sighs.oelib.event.events.InputEvent;
import cc.sighs.oelib.event.events.ScreenEvent;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;


public class OELHookClient {

    public static ScreenEvent.RenderInventoryMobEffects onScreenPotionSize(Screen screen, int availableSpace, boolean compact, int horizontalOffset) {
        final var event = new ScreenEvent.RenderInventoryMobEffects(screen, availableSpace, compact, horizontalOffset);
        EventBus.post(event);
        return event;
    }

    public static boolean onScreenMouseClickedPre(Screen guiScreen, double mouseX, double mouseY, int button) {
        return EventBus.post(new ScreenEvent.MouseButtonPressed.Pre(guiScreen, mouseX, mouseY, button));
    }

    public static boolean onScreenMouseClickedPost(Screen guiScreen, double mouseX, double mouseY, int button, boolean handled) {
        final var event = new ScreenEvent.MouseButtonPressed.Post(guiScreen, mouseX, mouseY, button, handled);
        EventBus.post(event);
        Result result = event.getResult();
        return result == Result.DEFAULT ? handled : result == Result.ALLOW;
    }

    public static boolean onScreenMouseReleasedPre(Screen guiScreen, double mouseX, double mouseY, int button) {
        return EventBus.post(new ScreenEvent.MouseButtonReleased.Pre(guiScreen, mouseX, mouseY, button));
    }

    public static boolean onScreenMouseReleasedPost(Screen guiScreen, double mouseX, double mouseY, int button, boolean handled) {
        final var event = new ScreenEvent.MouseButtonReleased.Post(guiScreen, mouseX, mouseY, button, handled);
        EventBus.post(event);
        Result result = event.getResult();
        return result == Result.DEFAULT ? handled : result == Result.ALLOW;
    }

    public static boolean onScreenMouseDragPre(Screen guiScreen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return EventBus.post(new ScreenEvent.MouseDragged.Pre(guiScreen, mouseX, mouseY, button, dragX, dragY));
    }

    public static void onScreenMouseDragPost(Screen guiScreen, double mouseX, double mouseY, int button, double dragX, double dragY) {
        EventBus.post(new ScreenEvent.MouseDragged.Post(guiScreen, mouseX, mouseY, button, dragX, dragY));
    }

    public static boolean onScreenMouseScrollPre(MouseHandler mouseHandler, Screen guiScreen, double scrollDelta) {
        Window window = Minecraft.getInstance().getWindow();
        double mouseX = mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double mouseY = mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
        return EventBus.post(new ScreenEvent.MouseScrolled.Pre(guiScreen, mouseX, mouseY, scrollDelta));
    }

    public static void onScreenMouseScrollPost(MouseHandler mouseHandler, Screen guiScreen, double scrollDelta) {
        Window window = Minecraft.getInstance().getWindow();
        double mouseX = mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double mouseY = mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
        EventBus.post(new ScreenEvent.MouseScrolled.Post(guiScreen, mouseX, mouseY, scrollDelta));
    }

    public static boolean onScreenKeyPressedPre(Screen guiScreen, int keyCode, int scanCode, int modifiers) {
        return EventBus.post(new ScreenEvent.KeyPressed.Pre(guiScreen, keyCode, scanCode, modifiers));
    }

    public static boolean onScreenKeyPressedPost(Screen guiScreen, int keyCode, int scanCode, int modifiers) {
        return EventBus.post(new ScreenEvent.KeyPressed.Post(guiScreen, keyCode, scanCode, modifiers));
    }

    public static boolean onScreenKeyReleasedPre(Screen guiScreen, int keyCode, int scanCode, int modifiers) {
        return EventBus.post(new ScreenEvent.KeyReleased.Pre(guiScreen, keyCode, scanCode, modifiers));
    }

    public static boolean onScreenKeyReleasedPost(Screen guiScreen, int keyCode, int scanCode, int modifiers) {
        return EventBus.post(new ScreenEvent.KeyReleased.Post(guiScreen, keyCode, scanCode, modifiers));
    }

    public static boolean onScreenCharTypedPre(Screen guiScreen, char codePoint, int modifiers) {
        return EventBus.post(new ScreenEvent.CharacterTyped.Pre(guiScreen, codePoint, modifiers));
    }

    public static void onScreenCharTypedPost(Screen guiScreen, char codePoint, int modifiers) {
        EventBus.post(new ScreenEvent.CharacterTyped.Post(guiScreen, codePoint, modifiers));
    }

    public static boolean wrapCharTypedWithEvents(GuiEventListener listener, char codePoint, int modifiers) {
        if (listener instanceof Screen screen) {
            if (onScreenCharTypedPre(screen, codePoint, modifiers)) {
                return true;
            }

            if (screen.charTyped(codePoint, modifiers)) {
                return true;
            }

            onScreenCharTypedPost(screen, codePoint, modifiers);

            return false;
        }

        return listener.charTyped(codePoint, modifiers);
    }

    public static boolean onMouseButtonPre(int button, int action, int mods) {
        return EventBus.post(new InputEvent.MouseButton.Pre(button, action, mods));
    }

    public static void onMouseButtonPost(int button, int action, int mods) {
        EventBus.post(new InputEvent.MouseButton.Post(button, action, mods));
    }

    public static boolean onMouseScroll(MouseHandler mouseHelper, double scrollDelta) {
        var event = new InputEvent.MouseScrollingEvent(scrollDelta, mouseHelper.isLeftPressed(), mouseHelper.isMiddlePressed(), mouseHelper.isRightPressed(), mouseHelper.xpos(), mouseHelper.ypos());
        return EventBus.post(event);
    }

    public static void onKeyInput(int key, int scanCode, int action, int modifiers) {
        EventBus.post(new InputEvent.Key(key, scanCode, action, modifiers));
    }
}
