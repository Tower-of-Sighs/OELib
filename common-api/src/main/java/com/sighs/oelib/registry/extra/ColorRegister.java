package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.ColorBlockAction;
import com.sighs.oelib.registry.action.ColorItemAction;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public final class ColorRegister {
    private ColorRegister() {
    }

    public static void registerColorBlocks(BlockColor color, Supplier<? extends Block>[] blocks) {
        RegistrationDispatcher.perform(new ColorBlockAction(color, blocks));
    }

    public static void registerColorItems(ItemColor color, Supplier<? extends ItemLike>[] items) {
        RegistrationDispatcher.perform(new ColorItemAction(color, items));
    }
}
