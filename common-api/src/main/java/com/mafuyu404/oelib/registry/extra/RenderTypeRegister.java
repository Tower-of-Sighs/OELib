package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.RenderTypeBlocksAction;
import com.mafuyu404.oelib.registry.action.RenderTypeFluidsAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class RenderTypeRegister {
    private RenderTypeRegister() {
    }

    @SafeVarargs
    public static void registerBlocks(RenderType type, Supplier<? extends Block>... blocks) {
        RegistrationDispatcher.perform(new RenderTypeBlocksAction(type, blocks));
    }

    @SafeVarargs
    public static void registerFluids(RenderType type, Supplier<? extends Fluid>... fluids) {
        RegistrationDispatcher.perform(new RenderTypeFluidsAction(type, fluids));
    }
}