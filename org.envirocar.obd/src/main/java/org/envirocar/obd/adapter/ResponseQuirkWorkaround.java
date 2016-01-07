package org.envirocar.obd.adapter;

public interface ResponseQuirkWorkaround {

    boolean shouldWaitForNextTokenLine(byte[] byteArray);

}
