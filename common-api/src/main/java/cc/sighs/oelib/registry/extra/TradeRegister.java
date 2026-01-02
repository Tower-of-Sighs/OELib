package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.TradeVillagerAction;
import cc.sighs.oelib.registry.action.TradeWandererAction;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

public final class TradeRegister {
    private TradeRegister() {
    }

    public static void registerVillagerTrade(VillagerProfession profession, int level, VillagerTrades.ItemListing... trades) {
        RegistrationDispatcher.perform(new TradeVillagerAction(profession, level, trades));
    }

    public static void registerTradeForWanderingTrader(boolean rare, VillagerTrades.ItemListing... trades) {
        RegistrationDispatcher.perform(new TradeWandererAction(rare, trades));
    }
}