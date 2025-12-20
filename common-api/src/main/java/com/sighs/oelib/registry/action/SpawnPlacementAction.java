package com.sighs.oelib.registry.action;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;
import java.util.function.Supplier;

public record SpawnPlacementAction<T extends Mob>(Supplier<? extends EntityType<T>> type,
                                                  SpawnPlacements.Type placement,
                                                  Heightmap.Types heightmap,
                                                  SpawnPlacements.SpawnPredicate<T> predicate) implements RegistrationAction {
    public SpawnPlacementAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(heightmap, "heightmap");
        Objects.requireNonNull(predicate, "predicate");
    }
}