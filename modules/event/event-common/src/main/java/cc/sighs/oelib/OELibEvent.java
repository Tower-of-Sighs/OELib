package cc.sighs.oelib;

import cc.sighs.oelib.event.EventAutoRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibEvent {

    public static final String MOD_ID = "oelib_event";
    public static final String MOD_NAME = "OELib Event";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        EventAutoRegistration.registerAllListeners();
    }
}
