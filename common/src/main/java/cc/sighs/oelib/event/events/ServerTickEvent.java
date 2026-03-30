package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.Event;
import net.minecraft.server.MinecraftServer;

import java.util.function.BooleanSupplier;

public abstract class ServerTickEvent implements Event {
    private final BooleanSupplier hasTime;
    private final MinecraftServer server;

    protected ServerTickEvent(BooleanSupplier hasTime, MinecraftServer server) {
        this.hasTime = hasTime;
        this.server = server;
    }

    public boolean hasTime() {
        return this.hasTime.getAsBoolean();
    }

    public MinecraftServer getServer() {
        return server;
    }

    public static class Pre extends ServerTickEvent {
        public Pre(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server);
        }
    }

    public static class Post extends ServerTickEvent {
        public Post(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server);
        }
    }
}
