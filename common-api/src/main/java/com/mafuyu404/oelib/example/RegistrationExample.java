package com.mafuyu404.oelib.example;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.registry.DeferredRegister;
import com.mafuyu404.oelib.registry.RegisterSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class RegistrationExample {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OELib.MODID);
    public static final RegisterSupplier<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties()));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, OELib.MODID);
    public static final RegisterSupplier<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of()));

    public static void init() {
        ITEMS.register();
        BLOCKS.register();
        OELib.LOGGER.info("Queued example registration: {}", EXAMPLE_ITEM.id() + " " + EXAMPLE_BLOCK.id());
    }
}