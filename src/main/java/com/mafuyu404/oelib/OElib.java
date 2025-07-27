package com.mafuyu404.oelib;

import com.mojang.logging.LogUtils;
import com.mafuyu404.oelib.core.DataRegistry;
import com.mafuyu404.oelib.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * OELib - 通用数据驱动框架。
 * <p>
 * 提供数据加载、验证、缓存、网络同步、表达式引擎等功能，
 * 让模组开发者可以轻松实现数据驱动的功能。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
 */
@Mod(OElib.MODID)
public class OElib {
    
    public static final String MODID = "oelib";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public OElib() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }
    public void commonSetup(FMLCommonSetupEvent event) {
        DataRegistry.initialize();
        NetworkHandler.register();
    }
}
