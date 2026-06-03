package {{PACKAGE}}.fabric;

import {{PACKAGE}}.{{CLASS_PREFIX}};
import net.fabricmc.api.ModInitializer;

public class {{CLASS_PREFIX}}Fabric implements ModInitializer {

    @Override
    public void onInitialize() {
        {{CLASS_PREFIX}}.init();
    }
}
