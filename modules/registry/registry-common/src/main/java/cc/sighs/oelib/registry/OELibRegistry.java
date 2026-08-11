package cc.sighs.oelib.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibRegistry {

    public static final String MOD_ID = "oelib_registry";
    public static final String MOD_NAME = "OELib Registry";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);    }
}