package cc.sighs.oelib.registry.action;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

public record RenderTypeBlocksAction(RenderType type,
                                     Supplier<? extends Block>[] blocks) implements RegistrationAction {
    public RenderTypeBlocksAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(blocks, "blocks");
    }
}