package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.CreativeTabAppendStackAction;
import cc.sighs.oelib.registry.action.CreativeTabModifyAction;
import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public final class CreativeTabRegister {
    private CreativeTabRegister() {
    }

    public static void modify(ResourceKey<CreativeModeTab> tab, CreativeTabModifyCallback callback) {
        RegistrationDispatcher.perform(new CreativeTabModifyAction(tab, callback));
    }

    public static void modifyBuiltin(CreativeModeTab tab, CreativeTabModifyCallback callback) {
        var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) throw new IllegalArgumentException("Builtin tab not registered: " + tab);
        modify(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id), callback);
    }

    public static void append(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> item) {
        RegistrationDispatcher.perform(new CreativeTabAppendStackAction(tab, item));
    }

    public static void append(ResourceKey<CreativeModeTab> tab, ItemLike... items) {
        for (ItemLike item : items) append(tab, () -> new ItemStack(item));
    }

    public static void appendStack(ResourceKey<CreativeModeTab> tab, ItemStack... items) {
        for (ItemStack stack : items) append(tab, () -> stack);
    }

    @SafeVarargs
    public static void appendStack(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack>... items) {
        for (Supplier<ItemStack> s : items) append(tab, s);
    }

    public static void appendBuiltin(CreativeModeTab tab, ItemLike... items) {
        var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) throw new IllegalArgumentException("Builtin tab not registered: " + tab);
        append(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id), items);
    }

    public static void appendBuiltinStack(CreativeModeTab tab, ItemStack... items) {
        var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) throw new IllegalArgumentException("Builtin tab not registered: " + tab);
        appendStack(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id), items);
    }

    @SafeVarargs
    public static void appendBuiltinStack(CreativeModeTab tab, Supplier<ItemStack>... items) {
        var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) throw new IllegalArgumentException("Builtin tab not registered: " + tab);
        appendStack(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id), items);
    }
}