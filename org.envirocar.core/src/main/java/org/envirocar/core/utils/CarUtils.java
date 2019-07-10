/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.utils;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * @author dewall
 */
public class CarUtils {
    private static final Logger logger = Logger.getLogger(CarUtils.class);

    public static Car instantiateCar(String object) {
        if (object == null) return null;

        ObjectInputStream ois = null;
        try {
            Base64InputStream b64 = new Base64InputStream(new ByteArrayInputStream(object
                    .getBytes()), Base64.DEFAULT);
            ois = new ObjectInputStream(b64);
            Object carObject = ois.readObject();
            if (carObject instanceof Car) {
                return (Car) carObject;
            }
        } catch (StreamCorruptedException e) {
            logger.warn(e.getMessage(), e);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
            return null;
        } catch (ClassCastException e) {
            logger.warn(e.getMessage(), e);
            return null;
        } finally {
            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
        }
        return null;
    }

    public static String serializeCar(Car car) {
        ObjectOutputStream oos = null;
        Base64OutputStream b64 = null;
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(byteArrayOut);
            oos.writeObject(car);
            oos.flush();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b64 = new Base64OutputStream(out, Base64.DEFAULT);
            b64.write(byteArrayOut.toByteArray());
            b64.flush();
            b64.close();
            out.flush();
            out.close();

            String result = new String(out.toByteArray());
            return result;
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (oos != null)
                try {
                    b64.close();
                    oos.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
        }
        return null;
    }

    public static String carToStringWithLinebreak(Car car) {
        StringBuilder sb = new StringBuilder();
        sb.append(car.getManufacturer());
        sb.append(" - ");
        sb.append(car.getModel());
        sb.append("\n");
        sb.append(car.getConstructionYear());
        sb.append(", ");
        sb.append(car.getFuelType());
        sb.append(", ");
        sb.append(car.getEngineDisplacement());
        sb.append("cc");
        return sb.toString();
    }

    /**
     * Returns true if the current remote id of the car starts with the temporary prefix
     *
     * @param car the car to check
     * @return true if the car has been uploaded.
     */
    public static boolean isCarUploaded(Car car) {
        if (car.getId() != null) {
            return !car.getId().startsWith(Car.TEMPORARY_SENSOR_ID);
        }
        return false;
    }

}
