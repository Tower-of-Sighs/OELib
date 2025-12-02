package com.mafuyu404.oelib.registry.action;

import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.Objects;

public record TradeWandererAction(boolean rare, VillagerTrades.ItemListing[] trades) implements RegistrationAction {
    public TradeWandererAction {
        Objects.requireNonNull(trades, "trades");
    }
}