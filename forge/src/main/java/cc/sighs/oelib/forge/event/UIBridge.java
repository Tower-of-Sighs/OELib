package cc.sighs.oelib.forge.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.events.InputEvent;
import cc.sighs.oelib.event.events.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

public final class UIBridge {
    private UIBridge() {}

    public static void initClient() {
        // Input: Mouse scrolling (outside screens)
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseScroll);
        // Global mouse button (outside screens)
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onGlobalMouseButtonPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onGlobalMouseButtonPost);
        // Global key input (outside screens)
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onGlobalKey);

        // Screen init
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenInitPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenInitPost);

        // Screen render
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenRenderPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenRenderPost);

        // Background rendered
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onBackgroundRendered);

        // Mouse button pressed/released on Screen
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMousePressedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMousePressedPost);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseReleasedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseReleasedPost);
        // Mouse dragged on Screen
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseDraggedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseDraggedPost);
        // Mouse scrolled on Screen
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseScrolledPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onMouseScrolledPost);

        // Keyboard on Screen
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onKeyPressedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onKeyPressedPost);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onKeyReleasedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onKeyReleasedPost);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onCharTypedPre);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onCharTypedPost);

        // Screen opening/closing
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenOpening);
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onScreenClosing);
        // Inventory mob effects render
        MinecraftForge.EVENT_BUS.addListener(UIBridge::onRenderInventoryMobEffects);
    }

    private static void onMouseScroll(net.minecraftforge.client.event.InputEvent.MouseScrollingEvent e) {
        InputEvent.MouseScrollingEvent ev = new InputEvent.MouseScrollingEvent(
                e.getScrollDelta(),
                e.isLeftDown(), e.isMiddleDown(), e.isRightDown(),
                e.getMouseX(), e.getMouseY()
        );
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onGlobalMouseButtonPre(net.minecraftforge.client.event.InputEvent.MouseButton.Pre e) {
        InputEvent.MouseButton.Pre ev = new InputEvent.MouseButton.Pre(e.getButton(), e.getAction(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onGlobalMouseButtonPost(net.minecraftforge.client.event.InputEvent.MouseButton.Post e) {
        InputEvent.MouseButton.Post ev = new InputEvent.MouseButton.Post(e.getButton(), e.getAction(), e.getModifiers());
        EventBus.post(ev);
    }

    private static void onGlobalKey(net.minecraftforge.client.event.InputEvent.Key e) {
        InputEvent.Key ev = new InputEvent.Key(e.getKey(), e.getScanCode(), e.getAction(), e.getModifiers());
        EventBus.post(ev);
    }

    private static void onScreenInitPre(net.minecraftforge.client.event.ScreenEvent.Init.Pre e) {
        ScreenEvent.Init.Pre ev = new ScreenEvent.Init.Pre(e.getScreen(), e.getListenersList(), e::addListener, e::removeListener);
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onScreenInitPost(net.minecraftforge.client.event.ScreenEvent.Init.Post e) {
        ScreenEvent.Init.Post ev = new ScreenEvent.Init.Post(e.getScreen(), e.getListenersList(), e::addListener, e::removeListener);
        EventBus.post(ev);
    }

    private static void onScreenRenderPre(net.minecraftforge.client.event.ScreenEvent.Render.Pre e) {
        ScreenEvent.Render.Pre ev = new ScreenEvent.Render.Pre(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onScreenRenderPost(net.minecraftforge.client.event.ScreenEvent.Render.Post e) {
        ScreenEvent.Render.Post ev = new ScreenEvent.Render.Post(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick());
        EventBus.post(ev);
    }

    private static void onBackgroundRendered(net.minecraftforge.client.event.ScreenEvent.BackgroundRendered e) {
        ScreenEvent.BackgroundRendered ev = new ScreenEvent.BackgroundRendered(e.getScreen(), e.getGuiGraphics());
        EventBus.post(ev);
    }

    private static void onMousePressedPre(net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre e) {
        ScreenEvent.MouseButtonPressed.Pre ev = new ScreenEvent.MouseButtonPressed.Pre(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onMousePressedPost(net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Post e) {
        ScreenEvent.MouseButtonPressed.Post ev = new ScreenEvent.MouseButtonPressed.Post(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton(), e.wasHandled());
        EventBus.post(ev);
        switch (ev.getResult()) {
            case ALLOW -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            case DENY -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            default -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.DEFAULT);
        }
    }

    private static void onMouseReleasedPre(net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Pre e) {
        ScreenEvent.MouseButtonReleased.Pre ev = new ScreenEvent.MouseButtonReleased.Pre(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onMouseReleasedPost(net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Post e) {
        ScreenEvent.MouseButtonReleased.Post ev = new ScreenEvent.MouseButtonReleased.Post(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getButton(), e.wasHandled());
        EventBus.post(ev);
        switch (ev.getResult()) {
            case ALLOW -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            case DENY -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            default -> e.setResult(net.minecraftforge.eventbus.api.Event.Result.DEFAULT);
        }
    }

    private static void onMouseDraggedPre(net.minecraftforge.client.event.ScreenEvent.MouseDragged.Pre e) {
        ScreenEvent.MouseDragged.Pre ev = new ScreenEvent.MouseDragged.Pre(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getMouseButton(), e.getDragX(), e.getDragY());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onMouseDraggedPost(net.minecraftforge.client.event.ScreenEvent.MouseDragged.Post e) {
        ScreenEvent.MouseDragged.Post ev = new ScreenEvent.MouseDragged.Post(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getMouseButton(), e.getDragX(), e.getDragY());
        EventBus.post(ev);
    }

    private static void onMouseScrolledPre(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre e) {
        ScreenEvent.MouseScrolled.Pre ev = new ScreenEvent.MouseScrolled.Pre(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getScrollDelta());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onMouseScrolledPost(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Post e) {
        ScreenEvent.MouseScrolled.Post ev = new ScreenEvent.MouseScrolled.Post(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getScrollDelta());
        EventBus.post(ev);
    }

    private static void onKeyPressedPre(net.minecraftforge.client.event.ScreenEvent.KeyPressed.Pre e) {
        ScreenEvent.KeyPressed.Pre ev = new ScreenEvent.KeyPressed.Pre(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onKeyPressedPost(net.minecraftforge.client.event.ScreenEvent.KeyPressed.Post e) {
        ScreenEvent.KeyPressed.Post ev = new ScreenEvent.KeyPressed.Post(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onKeyReleasedPre(net.minecraftforge.client.event.ScreenEvent.KeyReleased.Pre e) {
        ScreenEvent.KeyReleased.Pre ev = new ScreenEvent.KeyReleased.Pre(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onKeyReleasedPost(net.minecraftforge.client.event.ScreenEvent.KeyReleased.Post e) {
        ScreenEvent.KeyReleased.Post ev = new ScreenEvent.KeyReleased.Post(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onCharTypedPre(net.minecraftforge.client.event.ScreenEvent.CharacterTyped.Pre e) {
        ScreenEvent.CharacterTyped.Pre ev = new ScreenEvent.CharacterTyped.Pre(e.getScreen(), e.getCodePoint(), e.getModifiers());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
    }

    private static void onCharTypedPost(net.minecraftforge.client.event.ScreenEvent.CharacterTyped.Post e) {
        ScreenEvent.CharacterTyped.Post ev = new ScreenEvent.CharacterTyped.Post(e.getScreen(), e.getCodePoint(), e.getModifiers());
        EventBus.post(ev);
    }

    private static void onScreenOpening(net.minecraftforge.client.event.ScreenEvent.Opening e) {
        ScreenEvent.Opening ev = new ScreenEvent.Opening(e.getCurrentScreen(), e.getNewScreen());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        }
        @Nullable var newScreen = ev.getNewScreen();
        if (newScreen != e.getNewScreen()) {
            e.setNewScreen(newScreen);
        }
    }

    private static void onScreenClosing(net.minecraftforge.client.event.ScreenEvent.Closing e) {
        ScreenEvent.Closing ev = new ScreenEvent.Closing(e.getScreen());
        EventBus.post(ev);
    }

    private static void onRenderInventoryMobEffects(net.minecraftforge.client.event.ScreenEvent.RenderInventoryMobEffects e) {
        ScreenEvent.RenderInventoryMobEffects ev = new ScreenEvent.RenderInventoryMobEffects(e.getScreen(), e.getAvailableSpace(), e.isCompact(), e.getHorizontalOffset());
        EventBus.post(ev);
        if (ev.isCanceled()) {
            e.setCanceled(true);
        } else {
            e.setCompact(ev.isCompact());
            e.setHorizontalOffset(ev.getHorizontalOffset());
        }
    }
}
