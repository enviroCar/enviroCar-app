package org.envirocar.obd.adapter.async;

import org.envirocar.obd.adapter.ResponseQuirkWorkaround;

/**
 * Created by matthes on 17.12.15.
 */
public class PIDSupportedQuirk implements ResponseQuirkWorkaround {

    private static final byte[] PREFIX = "B70".getBytes();

    @Override
    public boolean shouldWaitForNextTokenLine(byte[] byteArray) {
        if (byteArray.length > 3) {
            for (int i = 0; i < PREFIX.length; i++) {
                if (byteArray[i] != PREFIX[i]) {
                    return false;
                }
            }
        }

        return false;
    }


}
