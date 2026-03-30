package cc.sighs.oelib.registry.action;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public record ColorBlockAction(List<BlockTintSource> sources,
                               Supplier<? extends Block>[] blocks) implements RegistrationAction {
    public ColorBlockAction {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(blocks, "blocks");
    }
}