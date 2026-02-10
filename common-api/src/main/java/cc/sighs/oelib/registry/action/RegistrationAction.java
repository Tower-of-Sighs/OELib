package cc.sighs.oelib.registry.action;

public sealed interface RegistrationAction
        permits RegistryBatchAction, KeyMappingAction, ClientTooltipComponentAction,
        ColorItemAction, ColorBlockAction,
        RenderTypeBlocksAction, RenderTypeFluidsAction,
        EntityRendererAction, EntityModelLayerAction,
        ParticleProviderAction, FuelAction, EntityAttributeAction,
        SpawnPlacementAction, TradeVillagerAction,
        TradeWandererAction, ListenAction, CreativeTabModifyAction, CreativeTabAppendStackAction,
        CommandRegisterAction, ShaderRegisterAction, MenuAction {
}