package {{PACKAGE}}.forge;

import {{PACKAGE}}.{{CLASS_PREFIX}};
import net.minecraftforge.fml.common.Mod;

@Mod({{CLASS_PREFIX}}.MOD_ID)
public class {{CLASS_PREFIX}}Forge {

    public {{CLASS_PREFIX}}Forge() {
        {{CLASS_PREFIX}}.init();
    }
}
