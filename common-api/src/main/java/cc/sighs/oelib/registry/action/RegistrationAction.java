package cc.sighs.oelib.registry.action;

public sealed interface RegistrationAction
        permits ClientTooltipComponentAction, ColorBlockAction, ColorItemAction, CommandRegisterAction, CreativeTabAppendStackAction, CreativeTabModifyAction, EntityAttributeAction, EntityModelLayerAction, EntityRendererAction, FuelAction, KeyMappingAction, ListenAction, ParticleProviderAction, RegistryBatchAction, RenderTypeBlocksAction, RenderTypeFluidsAction, SpawnPlacementAction, TradeVillagerAction, TradeWandererAction {
}