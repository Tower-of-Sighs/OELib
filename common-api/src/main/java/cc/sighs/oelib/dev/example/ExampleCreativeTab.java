package cc.sighs.oelib.dev.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.DeferredRegister;
import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.extra.CreativeTabRegister;
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
                    var sharpness = Enchantments.SHARPNESS;
                    ItemStack sword = new ItemStack(ExampleRegistry.EXAMPLE_ITEM.get());
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
