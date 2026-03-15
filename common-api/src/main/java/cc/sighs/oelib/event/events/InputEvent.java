package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.CancellableEvent;
import cc.sighs.oelib.event.Event;

public abstract class InputEvent implements Event {
    protected InputEvent() {
    }

    private static abstract class CancellableInputEvent extends InputEvent implements CancellableEvent {
        private boolean canceled;

        @Override
        public final boolean isCanceled() {
            return canceled;
        }

        @Override
        public final void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static abstract class MouseButton extends InputEvent {
        private final int button;
        private final int action;
        private final int modifiers;
        protected MouseButton(int button, int action, int modifiers) {
            this.button = button;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getButton() {
            return button;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }

        private static abstract class CancellableMouseButton extends MouseButton implements CancellableEvent {
            private boolean canceled;

            protected CancellableMouseButton(int button, int action, int modifiers) {
                super(button, action, modifiers);
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

        public static final class Pre extends CancellableMouseButton {
            public Pre(int button, int action, int modifiers) {
                super(button, action, modifiers);
            }
        }

        public static class Post extends MouseButton {
            public Post(int button, int action, int modifiers) {
                super(button, action, modifiers);
            }
        }
    }

    public static final class MouseScrollingEvent extends CancellableInputEvent {
        private final double scrollDelta;
        private final double mouseX;
        private final double mouseY;
        private final boolean leftDown;
        private final boolean middleDown;
        private final boolean rightDown;

        public MouseScrollingEvent(double scrollDelta, boolean leftDown, boolean middleDown, boolean rightDown, double mouseX, double mouseY) {
            this.scrollDelta = scrollDelta;
            this.leftDown = leftDown;
            this.middleDown = middleDown;
            this.rightDown = rightDown;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getScrollDelta() {
            return scrollDelta;
        }

        public boolean isLeftDown() {
            return leftDown;
        }

        public boolean isMiddleDown() {
            return middleDown;
        }

        public boolean isRightDown() {
            return rightDown;
        }

        public double getMouseX() {
            return mouseX;
        }

        public double getMouseY() {
            return mouseY;
        }

    }

    public static class Key extends InputEvent {
        private final int key;
        private final int scanCode;
        private final int action;
        private final int modifiers;

        public Key(int key, int scanCode, int action, int modifiers) {
            this.key = key;
            this.scanCode = scanCode;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getKey() {
            return key;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }
    }
}
