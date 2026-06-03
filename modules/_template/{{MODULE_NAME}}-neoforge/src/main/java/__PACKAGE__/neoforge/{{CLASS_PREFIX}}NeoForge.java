package {{PACKAGE}}.forge;

import {{PACKAGE}}.{{CLASS_PREFIX}};
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod({{CLASS_PREFIX}}.MOD_ID)
public class {{CLASS_PREFIX}}NeoForge {

    public {{CLASS_PREFIX}}NeoForge(IEventBus modEventBus, ModContainer modContainer) {
        {{CLASS_PREFIX}}.init();
    }
}
