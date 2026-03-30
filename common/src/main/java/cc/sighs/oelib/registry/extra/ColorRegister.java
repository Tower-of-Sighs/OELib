package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.ColorBlockAction;
import cc.sighs.oelib.registry.action.ColorItemAction;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

public final class ColorRegister {
    private ColorRegister() {
    }

    @SafeVarargs
    public static void registerBlockTints(List<BlockTintSource> sources, Supplier<? extends Block>... blocks) {
        RegistrationDispatcher.perform(new ColorBlockAction(sources, blocks));
    }

    public static void registerItemTint(Identifier identifier, MapCodec<? extends ItemTintSource> codec) {
        RegistrationDispatcher.perform(new ColorItemAction(identifier, codec));
    }
}
