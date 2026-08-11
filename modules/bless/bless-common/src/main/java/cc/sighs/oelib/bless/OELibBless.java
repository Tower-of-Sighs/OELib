package cc.sighs.oelib.bless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibBless {

    public static final String MOD_ID = "oelib_bless";
    public static final String MOD_NAME = "OELib Bless";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);    }
}