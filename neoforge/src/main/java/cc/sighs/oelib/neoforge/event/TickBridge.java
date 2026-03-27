package cc.sighs.oelib.neoforge.event;

import cc.sighs.oelib.event.EventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TickBridge {
    private TickBridge() {}

    public static void initServer() {
        NeoForge.EVENT_BUS.addListener(TickBridge::onServerTickPre);
        NeoForge.EVENT_BUS.addListener(TickBridge::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(TickBridge::onLevelTickPre);
        NeoForge.EVENT_BUS.addListener(TickBridge::onLevelTickPost);
    }

    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(TickBridge::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(TickBridge::onClientTickPost);
    }

    private static void onServerTickPre(ServerTickEvent.Pre e) {
        EventBus.post(new cc.sighs.oelib.event.events.ServerTickEvent.Pre(e::hasTime, e.getServer()));
    }

    private static void onServerTickPost(ServerTickEvent.Post e) {
        EventBus.post(new cc.sighs.oelib.event.events.ServerTickEvent.Post(e::hasTime, e.getServer()));
    }

    private static void onLevelTickPre(LevelTickEvent.Pre e) {
        EventBus.post(new cc.sighs.oelib.event.events.LevelTickEvent.Pre(e::hasTime, e.getLevel()));
    }

    private static void onLevelTickPost(LevelTickEvent.Post e) {
        EventBus.post(new cc.sighs.oelib.event.events.LevelTickEvent.Post(e::hasTime, e.getLevel()));
    }

    private static void onClientTickPre(ClientTickEvent.Pre e) {
        EventBus.post(new cc.sighs.oelib.event.events.ClientTickEvent.Pre());
    }

    private static void onClientTickPost(ClientTickEvent.Post e) {
        EventBus.post(new cc.sighs.oelib.event.events.ClientTickEvent.Post());
    }
}

