package org.envirocar.app.util;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;

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
            Base64InputStream b64 = new Base64InputStream(new ByteArrayInputStream(object.getBytes()), Base64.DEFAULT);
            ois = new ObjectInputStream(b64);
            Car car = (Car) ois.readObject();
            return car;
        } catch (StreamCorruptedException e) {
            logger.warn(e.getMessage(), e);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
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
}
