package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.CancellableEvent;
import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.Result;
import cc.sighs.oelib.event.ResultEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class ScreenEvent implements Event {
    private final Screen screen;

    protected ScreenEvent(Screen screen) {
        this.screen = Objects.requireNonNull(screen);
    }

    public Screen getScreen() {
        return screen;
    }

    private static abstract class CancellableScreenEvent extends ScreenEvent implements CancellableEvent {
        private boolean canceled;

        protected CancellableScreenEvent(Screen screen) {
            super(screen);
        }

        @Override
        public final boolean isCanceled() {
            return canceled;
        }

        @Override
        public final void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static abstract class Init extends ScreenEvent {
        private final Consumer<GuiEventListener> add;
        private final Consumer<GuiEventListener> remove;
        private final List<GuiEventListener> listenerList;

        protected Init(Screen screen, List<GuiEventListener> listenerList, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
            super(screen);
            this.listenerList = Collections.unmodifiableList(listenerList);
            this.add = add;
            this.remove = remove;
        }

        public List<GuiEventListener> getListenersList() {
            return listenerList;
        }

        public void addListener(GuiEventListener listener) {
            add.accept(listener);
        }

        public void removeListener(GuiEventListener listener) {
            remove.accept(listener);
        }

        private static abstract class CancellableInit extends Init implements CancellableEvent {
            private boolean canceled;

            protected CancellableInit(Screen screen, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
                super(screen, list, add, remove);
            }

            @Override
            public boolean isCanceled() {
                return canceled;
            }

            @Override
            public void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableInit {
            public Pre(Screen screen, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
                super(screen, list, add, remove);
            }
        }

        public static class Post extends Init {
            public Post(Screen screen, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
                super(screen, list, add, remove);
            }
        }
    }

    public static abstract class Render extends ScreenEvent {
        private final GuiGraphics guiGraphics;
        private final int mouseX;
        private final int mouseY;
        private final float partialTick;

        protected Render(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super(screen);
            this.guiGraphics = guiGraphics;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.partialTick = partialTick;
        }

        public GuiGraphics getGuiGraphics() {
            return guiGraphics;
        }

        public int getMouseX() {
            return mouseX;
        }

        public int getMouseY() {
            return mouseY;
        }

        public float getPartialTick() {
            return partialTick;
        }

        private static abstract class CancellableRender extends Render implements CancellableEvent {
            private boolean canceled;

            protected CancellableRender(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super(screen, guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableRender {
            public Pre(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super(screen, guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        public static class Post extends Render {
            public Post(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super(screen, guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public static class BackgroundRendered extends ScreenEvent {
        private final GuiGraphics guiGraphics;

        public BackgroundRendered(Screen screen, GuiGraphics guiGraphics) {
            super(screen);
            this.guiGraphics = guiGraphics;
        }

        public GuiGraphics getGuiGraphics() {
            return guiGraphics;
        }
    }

    public static class RenderInventoryMobEffects extends CancellableScreenEvent {
        private final int availableSpace;
        private boolean compact;
        private int horizontalOffset;

        public RenderInventoryMobEffects(Screen screen, int availableSpace, boolean compact, int horizontalOffset) {
            super(screen);
            this.availableSpace = availableSpace;
            this.compact = compact;
            this.horizontalOffset = horizontalOffset;
        }

        public int getAvailableSpace() {
            return availableSpace;
        }

        public boolean isCompact() {
            return compact;
        }

        public void setCompact(boolean compact) {
            this.compact = compact;
        }

        public int getHorizontalOffset() {
            return horizontalOffset;
        }

        public void setHorizontalOffset(int offset) {
            horizontalOffset = offset;
        }

        public void addHorizontalOffset(int offset) {
            horizontalOffset += offset;
        }

    }

    private static abstract class MouseInput extends ScreenEvent {
        private final double mouseX;
        private final double mouseY;

        protected MouseInput(Screen screen, double mouseX, double mouseY) {
            super(screen);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getMouseX() {
            return mouseX;
        }

        public double getMouseY() {
            return mouseY;
        }
    }

    public static abstract class MouseButtonPressed extends MouseInput {
        private final int button;

        protected MouseButtonPressed(Screen screen, double mouseX, double mouseY, int button) {
            super(screen, mouseX, mouseY);
            this.button = button;
        }

        public int getButton() {
            return button;
        }

        private static abstract class CancellablePressed extends MouseButtonPressed implements CancellableEvent {
            private boolean canceled;

            protected CancellablePressed(Screen screen, double mouseX, double mouseY, int button) {
                super(screen, mouseX, mouseY, button);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellablePressed {
            public Pre(Screen screen, double mouseX, double mouseY, int button) {
                super(screen, mouseX, mouseY, button);
            }
        }

        public static class Post extends MouseButtonPressed implements ResultEvent {
            private final boolean handled;
            private Result result = Result.DEFAULT;

            public Post(Screen screen, double mouseX, double mouseY, int button, boolean handled) {
                super(screen, mouseX, mouseY, button);
                this.handled = handled;
            }

            public boolean wasHandled() {
                return handled;
            }

            @Override
            public Result getResult() {
                return result;
            }

            @Override
            public void setResult(Result result) {
                this.result = result == null ? Result.DEFAULT : result;
            }
        }
    }

    public static abstract class MouseButtonReleased extends MouseInput {
        private final int button;

        protected MouseButtonReleased(Screen screen, double mouseX, double mouseY, int button) {
            super(screen, mouseX, mouseY);
            this.button = button;
        }

        public int getButton() {
            return button;
        }

        private static abstract class CancellableReleased extends MouseButtonReleased implements CancellableEvent {
            private boolean canceled;

            protected CancellableReleased(Screen screen, double mouseX, double mouseY, int button) {
                super(screen, mouseX, mouseY, button);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableReleased {
            public Pre(Screen screen, double mouseX, double mouseY, int button) {
                super(screen, mouseX, mouseY, button);
            }
        }

        public static class Post extends MouseButtonReleased implements ResultEvent {
            private final boolean handled;
            private Result result = Result.DEFAULT;

            public Post(Screen screen, double mouseX, double mouseY, int button, boolean handled) {
                super(screen, mouseX, mouseY, button);
                this.handled = handled;
            }

            public boolean wasHandled() {
                return handled;
            }

            @Override
            public Result getResult() {
                return result;
            }

            @Override
            public void setResult(Result result) {
                this.result = result == null ? Result.DEFAULT : result;
            }
        }
    }

    public static abstract class MouseDragged extends MouseInput {
        private final int mouseButton;
        private final double dragX;
        private final double dragY;

        protected MouseDragged(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
            super(screen, mouseX, mouseY);
            this.mouseButton = mouseButton;
            this.dragX = dragX;
            this.dragY = dragY;
        }

        public int getMouseButton() {
            return mouseButton;
        }

        public double getDragX() {
            return dragX;
        }

        public double getDragY() {
            return dragY;
        }

        private static abstract class CancellableDragged extends MouseDragged implements CancellableEvent {
            private boolean canceled;

            protected CancellableDragged(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
                super(screen, mouseX, mouseY, mouseButton, dragX, dragY);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableDragged {
            public Pre(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
                super(screen, mouseX, mouseY, mouseButton, dragX, dragY);
            }
        }

        public static class Post extends MouseDragged {
            public Post(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
                super(screen, mouseX, mouseY, mouseButton, dragX, dragY);
            }
        }
    }

    public static abstract class MouseScrolled extends MouseInput {
        private final double scrollDelta;

        protected MouseScrolled(Screen screen, double mouseX, double mouseY, double scrollDelta) {
            super(screen, mouseX, mouseY);
            this.scrollDelta = scrollDelta;
        }

        public double getScrollDelta() {
            return scrollDelta;
        }

        private static abstract class CancellableScrolled extends MouseScrolled implements CancellableEvent {
            private boolean canceled;

            protected CancellableScrolled(Screen screen, double mouseX, double mouseY, double scrollDelta) {
                super(screen, mouseX, mouseY, scrollDelta);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableScrolled {
            public Pre(Screen screen, double mouseX, double mouseY, double scrollDelta) {
                super(screen, mouseX, mouseY, scrollDelta);
            }
        }

        public static class Post extends MouseScrolled {
            public Post(Screen screen, double mouseX, double mouseY, double scrollDelta) {
                super(screen, mouseX, mouseY, scrollDelta);
            }
        }
    }

    private static abstract class KeyInput extends ScreenEvent {
        private final int keyCode;
        private final int scanCode;
        private final int modifiers;

        protected KeyInput(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen);
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getModifiers() {
            return modifiers;
        }
    }

    public static abstract class KeyPressed extends KeyInput {
        protected KeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
        }

        private static abstract class CancellableKeyPressed extends KeyPressed implements CancellableEvent {
            private boolean canceled;

            protected CancellableKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableKeyPressed {
            public Pre(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }
        }

        public static final class Post extends CancellableKeyPressed {
            public Post(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }
        }
    }

    public static abstract class KeyReleased extends KeyInput {
        protected KeyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
        }

        private static abstract class CancellableKeyReleased extends KeyReleased implements CancellableEvent {
            private boolean canceled;

            protected CancellableKeyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableKeyReleased {
            public Pre(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }
        }

        public static final class Post extends CancellableKeyReleased {
            public Post(Screen screen, int keyCode, int scanCode, int modifiers) {
                super(screen, keyCode, scanCode, modifiers);
            }
        }
    }

    public static class CharacterTyped extends ScreenEvent {
        private final char codePoint;
        private final int modifiers;

        public CharacterTyped(Screen screen, char codePoint, int modifiers) {
            super(screen);
            this.codePoint = codePoint;
            this.modifiers = modifiers;
        }

        public char getCodePoint() {
            return codePoint;
        }

        public int getModifiers() {
            return modifiers;
        }

        private static abstract class CancellableCharacterTyped extends CharacterTyped implements CancellableEvent {
            private boolean canceled;

            protected CancellableCharacterTyped(Screen screen, char codePoint, int modifiers) {
                super(screen, codePoint, modifiers);
            }

            @Override
            public final boolean isCanceled() {
                return canceled;
            }

            @Override
            public final void setCanceled(boolean canceled) {
                this.canceled = canceled;
            }
        }

        public static final class Pre extends CancellableCharacterTyped {
            public Pre(Screen screen, char codePoint, int modifiers) {
                super(screen, codePoint, modifiers);
            }
        }

        public static class Post extends CharacterTyped {
            public Post(Screen screen, char codePoint, int modifiers) {
                super(screen, codePoint, modifiers);
            }
        }
    }

    @ApiStatus.Internal
    public static class Opening extends CancellableScreenEvent {
        @Nullable
        private final Screen currentScreen;
        private Screen newScreen;

        public Opening(@Nullable Screen currentScreen, Screen screen) {
            super(screen);
            this.currentScreen = currentScreen;
            this.newScreen = screen;
        }

        @Nullable
        public Screen getCurrentScreen() {
            return currentScreen;
        }

        public Screen getNewScreen() {
            return newScreen;
        }

        public void setNewScreen(Screen newScreen) {
            this.newScreen = newScreen;
        }

    }

    @ApiStatus.Internal
    public static class Closing extends ScreenEvent {
        public Closing(Screen screen) {
            super(screen);
        }
    }
}
