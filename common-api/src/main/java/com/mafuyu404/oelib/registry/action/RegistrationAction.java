package com.mafuyu404.oelib.registry.action;

public sealed interface RegistrationAction
        permits RegistryBatchAction, KeyMappingAction, ClientTooltipComponentAction,
        ColorItemAction, ColorBlockAction,
        RenderTypeBlocksAction, RenderTypeFluidsAction,
        EntityRendererAction, EntityModelLayerAction,
        ParticleProviderAction, FuelAction, EntityAttributeAction,
        SpawnPlacementAction, TradeVillagerAction,
        TradeWandererAction {
}