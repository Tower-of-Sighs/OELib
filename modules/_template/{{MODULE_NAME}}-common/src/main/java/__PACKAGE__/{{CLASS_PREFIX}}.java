package {{PACKAGE}};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class {{CLASS_PREFIX}} {

    public static final String MOD_ID = "{{FULL_MOD_ID}}";
    public static final String MOD_NAME = "{{FULL_MOD_NAME}}";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);
    }
}
