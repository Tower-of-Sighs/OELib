package com.mafuyu404.oelib.registry.action;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.Objects;

public record TradeVillagerAction(VillagerProfession profession,
                                  int level,
                                  VillagerTrades.ItemListing[] trades) implements RegistrationAction {
    public TradeVillagerAction {
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(trades, "trades");
        if (level < 1) throw new IllegalArgumentException("Villager trade level must be >= 1");
    }
}