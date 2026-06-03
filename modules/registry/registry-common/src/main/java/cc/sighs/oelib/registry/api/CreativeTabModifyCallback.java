package cc.sighs.oelib.registry.api;

import net.minecraft.world.flag.FeatureFlagSet;

public interface CreativeTabModifyCallback {
    void accept(FeatureFlagSet flags, CreativeTabOutput output, boolean canUseGameMasterBlocks);
}