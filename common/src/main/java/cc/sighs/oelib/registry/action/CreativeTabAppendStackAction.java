package cc.sighs.oelib.registry.action;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

public record CreativeTabAppendStackAction(ResourceKey<CreativeModeTab> tab,
                                           Supplier<ItemStack> item) implements RegistrationAction {
    public CreativeTabAppendStackAction {
        Objects.requireNonNull(tab, "tab");
        Objects.requireNonNull(item, "item");
    }
}