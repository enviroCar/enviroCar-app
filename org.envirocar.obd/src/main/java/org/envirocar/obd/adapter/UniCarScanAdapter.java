package org.envirocar.obd.adapter;

public class UniCarScanAdapter extends OBDLinkAdapter{
    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("OBDII") || deviceName.contains("ELM327");
    }
}
