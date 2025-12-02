package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.RenderTypeBlocksAction;
import com.mafuyu404.oelib.registry.action.RenderTypeFluidsAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

@Environment(EnvType.CLIENT)
public final class RenderTypeRegister {
    private RenderTypeRegister() {
    }

    public static void register(RenderType type, Block... blocks) {
        RegistrationDispatcher.perform(new RenderTypeBlocksAction(type, blocks));
    }

    public static void register(RenderType type, Fluid... fluids) {
        RegistrationDispatcher.perform(new RenderTypeFluidsAction(type, fluids));
    }
}