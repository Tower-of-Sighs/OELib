package cc.sighs.oelib.registry.action;

import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Objects;

public record CreativeTabModifyAction(ResourceKey<CreativeModeTab> tab,
                                      CreativeTabModifyCallback callback) implements RegistrationAction {
    public CreativeTabModifyAction {
        Objects.requireNonNull(tab, "tab");
        Objects.requireNonNull(callback, "callback");
    }
}