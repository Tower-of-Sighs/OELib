package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.SpawnPlacementAction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Supplier;

public final class SpawnPlacementsRegister {
    private SpawnPlacementsRegister() {
    }

    public static <T extends Mob> void register(Supplier<? extends EntityType<T>> type,
                                                SpawnPlacementType placement,
                                                Heightmap.Types heightmap,
                                                SpawnPlacements.SpawnPredicate<T> predicate) {
        RegistrationDispatcher.perform(new SpawnPlacementAction<>(type, placement, heightmap, predicate));
    }
}