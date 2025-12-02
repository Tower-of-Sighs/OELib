package com.mafuyu404.oelib.api.registry;

import net.minecraft.world.flag.FeatureFlagSet;

public interface CreativeTabModifyCallback {
    void accept(FeatureFlagSet flags, CreativeTabOutput output, boolean canUseGameMasterBlocks);
}