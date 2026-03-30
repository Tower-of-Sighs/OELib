package cc.sighs.oelib.registry.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record ColorItemAction(Identifier identifier, MapCodec<? extends ItemTintSource> source) implements RegistrationAction {
    public ColorItemAction {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(source, "source");
    }
}