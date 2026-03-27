package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.Event;

public abstract class ClientTickEvent implements Event {
    public static class Pre extends ClientTickEvent {
        public Pre() {}
    }

    public static class Post extends ClientTickEvent {
        public Post() {}
    }
}
