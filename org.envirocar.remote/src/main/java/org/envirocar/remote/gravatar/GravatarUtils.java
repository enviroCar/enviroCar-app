package org.envirocar.remote.gravatar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.common.base.Preconditions;

import org.apache.commons.compress.utils.IOUtils;
import org.envirocar.core.logging.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author dewall
 */
public class GravatarUtils {
    private static final Logger LOG = Logger.getLogger(GravatarUtils.class);
    private static final String GRAVATAR_URL = "http://www.gravatar.com/avatar/";

    /**
     * from https://de.gravatar.com/site/implement/images/java/
     *
     * @param array
     * @return
     */
    private static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i]
                    & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    /**
     * from https://de.gravatar.com/site/implement/images/java/
     *
     * @param message
     * @return
     */
    private static String md5Hex(String message) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            LOG.warn(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param mail the mail address.
     * @return the GRAVATAR_URL of an image for a given mail address.
     */
    private static String getUrl(String mail) {
        String hash = new String(md5Hex(mail.toLowerCase().trim()));
        String params = "";
        return GRAVATAR_URL + hash + ".jpg" + params;
    }

    /**
     * @param email
     * @return
     * @throws Exception
     */
    public static byte[] download(String email) throws IOException {
        Preconditions.checkState(email != null && !email.isEmpty() && !email.equals(""),
                "Mail cannot be null or empty.");

        InputStream is = null;
        try {
            URL url = new URL(getUrl(email));
            is = url.openStream();
            return IOUtils.toByteArray(is);
        } catch (FileNotFoundException e) {
            LOG.warn(e.getMessage(), e);
            return null;
        } catch (MalformedURLException e) {
            LOG.warn(e.getMessage(), e);
            return null;
        } finally {
            if(is != null)
            is.close();
        }
    }

    /**
     * Downloads the bitmap and decodes the downloaded byte array to a bitmap.
     *
     * @param mail the mail to download the gravatar bitmap for.
     * @return the gravatar bitmap for a given mail.
     * @throws Exception
     */
    public static Bitmap downloadBitmap(String mail) throws IOException {
        byte[] dataArray = download(mail);
        Bitmap bitmap = BitmapFactory.decodeByteArray(dataArray, 0, dataArray.length);
        return bitmap;
    }
}
