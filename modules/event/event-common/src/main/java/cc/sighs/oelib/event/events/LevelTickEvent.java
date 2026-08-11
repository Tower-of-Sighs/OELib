package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.Event;
import net.minecraft.world.level.Level;

import java.util.function.BooleanSupplier;

public abstract class LevelTickEvent implements Event {
    private final BooleanSupplier hasTime;
    private final Level level;

    protected LevelTickEvent(BooleanSupplier hasTime, Level level) {
        this.hasTime = hasTime;
        this.level = level;
    }

    public boolean hasTime() {
        return this.hasTime.getAsBoolean();
    }

    public Level getLevel() {
        return level;
    }

    public static class Pre extends LevelTickEvent {
        public Pre(BooleanSupplier haveTime, Level level) {
            super(haveTime, level);
        }
    }

    public static class Post extends LevelTickEvent {
        public Post(BooleanSupplier haveTime, Level level) {
            super(haveTime, level);
        }
    }
}
