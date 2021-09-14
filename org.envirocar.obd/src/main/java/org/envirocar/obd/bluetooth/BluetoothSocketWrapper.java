/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd.bluetooth;

import android.bluetooth.BluetoothSocket;

import org.envirocar.core.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO JavaDoc
 */
public abstract class BluetoothSocketWrapper {
    private static final Logger LOG = Logger.getLogger(BluetoothSocketWrapper.class);

	public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract String getRemoteDeviceName();

    public abstract void connect() throws IOException;

    public abstract String getRemoteDeviceAddress();

    public abstract void close() throws IOException;

    public abstract BluetoothSocket getUnderlyingSocket();

    public void shutdown() {
        LOG.info("Shutting down bluetooth socket...");

        try {
            if (getInputStream() != null) {
                getInputStream().close();
            } else {
                LOG.warn("No socket InputStream found for closing");
            }

        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }

        try {
            if (getOutputStream() != null) {
                getOutputStream().close();
            } else {
                LOG.warn("No socket OutputStream found for closing");
            }

        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }

        try {
            close();
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }

        LOG.info("bluetooth socket down!");
    }
}
