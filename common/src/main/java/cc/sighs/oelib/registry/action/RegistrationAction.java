package cc.sighs.oelib.registry.action;

public sealed interface RegistrationAction
        permits RegistryBatchAction, KeyMappingAction, ClientTooltipComponentAction,
        ColorItemAction, ColorBlockAction,
        EntityRendererAction, EntityModelLayerAction,
        ParticleProviderAction, FuelAction, EntityAttributeAction,
        SpawnPlacementAction, ListenAction, CreativeTabModifyAction, CreativeTabAppendStackAction,
        CommandRegisterAction, MenuAction {
}