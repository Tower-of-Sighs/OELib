package com.sighs.oelib.example;

import com.sighs.oelib.OELib;
import com.sighs.oelib.registry.DeferredRegister;
import com.sighs.oelib.registry.RegisterSupplier;
import com.sighs.oelib.registry.extra.CreativeTabRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class ExampleCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OELib.MODID);

    public static final RegisterSupplier<CreativeModeTab> GENERAL = TABS.register("general", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.oelib.general"))
                    .icon(() -> new ItemStack(ExampleRegistry.EXAMPLE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ExampleRegistry.EXAMPLE_BLOCK_ITEM.get());
                    })
                    .build()
    );

    public static void init() {
        TABS.register();
    }

    public static void registerCreativeTabEntries() {
        CreativeTabRegister.appendStack(
                GENERAL.key(),
                () -> {

                    var mc = Minecraft.getInstance();

                    var registryAccess = mc.level.registryAccess();
                    var enchantReg = registryAccess.registryOrThrow(Registries.ENCHANTMENT);

                    var sharpness = enchantReg
                            .getHolder(Enchantments.SHARPNESS)
                            .orElse(null);

                    var sword = new ItemStack(ExampleRegistry.EXAMPLE_ITEM.get());
                    sword.enchant(sharpness, 5);

                    return sword;
                }
        );
    }

    public static void modifyCreativeTab() {
        CreativeTabRegister.modify(
                CreativeModeTabs.COMBAT, ((flags, output, canUseGameMasterBlocks) -> {
                    var item = new ItemStack(ExampleRegistry.EXAMPLE_ITEM.get());
                    var sword = new ItemStack(Items.NETHERITE_SWORD);
                    output.acceptAfter(sword, item);
                })
        );
    }
}
