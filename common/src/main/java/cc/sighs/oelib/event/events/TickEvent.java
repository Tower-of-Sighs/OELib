package cc.sighs.oelib.event.events;

import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.LogicalSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.BooleanSupplier;

public class TickEvent implements Event {
    public final Type type;
    public final LogicalSide side;
    public final Phase phase;
    public TickEvent(Type type, LogicalSide side, Phase phase) {
        this.type = type;
        this.side = side;
        this.phase = phase;
    }
    public enum Type {
        LEVEL, PLAYER, CLIENT, SERVER, RENDER
    }

    public enum Phase {
        START, END
    }

    public static class ServerTickEvent extends TickEvent {
        private final BooleanSupplier haveTime;
        private final MinecraftServer server;

        public ServerTickEvent(Phase phase, BooleanSupplier haveTime, MinecraftServer server) {
            super(Type.SERVER, LogicalSide.SERVER, phase);
            this.haveTime = haveTime;
            this.server = server;
        }

        public boolean haveTime() {
            return this.haveTime.getAsBoolean();
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(Type.CLIENT, LogicalSide.CLIENT, phase);
        }
    }

    public static class LevelTickEvent extends TickEvent {
        public final Level level;
        private final BooleanSupplier haveTime;

        public LevelTickEvent(LogicalSide side, Phase phase, Level level, BooleanSupplier haveTime) {
            super(Type.LEVEL, side, phase);
            this.level = level;
            this.haveTime = haveTime;
        }

        public boolean haveTime() {
            return this.haveTime.getAsBoolean();
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final Player player;

        public PlayerTickEvent(Phase phase, Player player) {
            super(Type.PLAYER, player instanceof ServerPlayer ? LogicalSide.SERVER : LogicalSide.CLIENT, phase);
            this.player = player;
        }
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;
        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(Type.RENDER, LogicalSide.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }
}
