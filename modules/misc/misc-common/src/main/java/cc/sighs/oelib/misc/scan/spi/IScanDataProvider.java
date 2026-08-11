package cc.sighs.oelib.misc.scan.spi;

import cc.sighs.oelib.misc.scan.OELScanData;

public interface IScanDataProvider {
    default void preload() {}

    OELScanData getScanData();
}
