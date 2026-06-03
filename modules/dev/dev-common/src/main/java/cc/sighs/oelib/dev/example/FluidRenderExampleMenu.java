package cc.sighs.oelib.dev.example;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FluidRenderExampleMenu extends AbstractContainerMenu {
    public final ContainerLevelAccess access;

    public FluidRenderExampleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FluidRenderExampleMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ExampleMenus.FLUID_RENDER_EXAMPLE.get(), containerId);
        this.access = access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof ExampleBlock, true);
    }

    public static class Provider implements MenuProvider {
        private final BlockPos pos;

        public Provider(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Fluid Render Example");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new FluidRenderExampleMenu(containerId, playerInventory, ContainerLevelAccess.create(player.level(), pos));
        }
    }
}
