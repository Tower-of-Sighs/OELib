package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.TradeVillagerAction;
import com.mafuyu404.oelib.registry.action.TradeWandererAction;
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