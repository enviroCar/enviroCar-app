package org.envirocar.obd.service;

/**
 * @author dewall
 */
public interface BluetoothConstants {

    int MESSAGE_DEVICE_NAME = 100;
    int MESSAGE_WRITE = 101;
    int MESSAGE_READ = 102;
    int MESSAGE_STATE_CHANGE = 103;

    String ACTION_BROADCAST = "bluetooth_action_broadcast";
}
