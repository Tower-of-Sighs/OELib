package cc.sighs.oelib.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibNetwork {

    public static final String MOD_ID = "oelib_network";
    public static final String MOD_NAME = "OELib Network";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {}", MOD_NAME);
    }
}
