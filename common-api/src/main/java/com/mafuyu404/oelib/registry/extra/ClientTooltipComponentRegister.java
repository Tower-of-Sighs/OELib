package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.ClientTooltipComponentAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class ClientTooltipComponentRegister {
    private ClientTooltipComponentRegister() {
    }

    public static <T extends TooltipComponent> void register(Class<T> clazz, Function<? super T, ? extends ClientTooltipComponent> factory) {
        RegistrationDispatcher.perform(new ClientTooltipComponentAction<>(clazz, factory));
    }
}
