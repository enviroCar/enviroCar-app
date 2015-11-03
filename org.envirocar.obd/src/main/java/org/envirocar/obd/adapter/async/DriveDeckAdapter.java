package org.envirocar.obd.adapter.async;

/**
 * Created by matthes on 03.11.15.
 */
public class DriveDeckAdapter extends AsyncAdapter {
    @Override
    public boolean supportsDevice(String deviceName) {
        return deviceName.contains("DRIVEDECK");
    }
}
