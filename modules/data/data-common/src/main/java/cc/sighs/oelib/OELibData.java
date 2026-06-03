package cc.sighs.oelib;

import cc.sighs.oelib.data.DataRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibData {

    public static final String MOD_ID = "oelib_data";
    public static final String MOD_NAME = "OELib Data";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        DataRegistry.initialize();
        DataRegistry.initializeExpressionEngine();
    }
}
