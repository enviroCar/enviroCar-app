package org.envirocar.obd.service;

/**
 * @author dewall
 */
public enum BluetoothServiceState {
    SERVICE_STOPPED,
    SERVICE_DEVICE_DISCOVERY_PENDING,
    SERVICE_DEVICE_DISCOVERY_RUNNING,
    SERVICE_STARTING,
    SERVICE_STARTED,
    SERVICE_STOPPING;
}
