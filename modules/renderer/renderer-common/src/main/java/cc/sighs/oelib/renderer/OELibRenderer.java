package cc.sighs.oelib.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibRenderer {

    public static final String MOD_ID = "oelib_renderer";
    public static final String MOD_NAME = "OELib Renderer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);
    }
}
