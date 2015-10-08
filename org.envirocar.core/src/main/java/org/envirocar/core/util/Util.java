/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.core.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.json.JSONException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Util {
    private static final Logger logger = Logger.getLogger(Util.class);
    public static final String NEW_LINE_CHAR = System
            .getProperty("line.separator");
    public static final String EXTERNAL_SUB_FOLDER = "enviroCar";
    private static ISO8601DateFormat jacksonFormat = new ISO8601DateFormat();

    /**
     * Create a file in the .enviroCar folder of the external storage.
     *
     * @param fileName the name of the new file
     * @return the resulting file
     * @throws IOException
     */
    public static File createFileOnExternalStorage(String fileName)
            throws IOException {
        File directory = resolveExternalStorageBaseFolder();

        File f = new File(directory, fileName);
        f.createNewFile();
        if (!f.isFile()) {
            throw new IOException(fileName + " is not a file!");
        }
        return f;
    }

    public static File resolveCacheFolder(Context ctx) {
        return ctx.getCacheDir();
    }

    public static File resolveExternalStorageBaseFolder() throws IOException {
        File directory = new File(Environment.getExternalStorageDirectory()
                + File.separator + EXTERNAL_SUB_FOLDER);

        if (!directory.exists()) {
            directory.mkdir();
        }
        if (!directory.isDirectory()) {
            throw new IOException(directory.getAbsolutePath()
                    + " is not a directory!");
        }

        return directory;
    }

    /**
     * Zips a list of files into the target archive.
     *
     * @param files  the list of files of the target archive
     * @param target the target filename
     * @throws IOException
     */
    public static void zipNative(List<File> files, String target) throws IOException {
        ZipOutputStream zos = null;
        try {
            File targetFile = new File(target);
            FileOutputStream dest = new FileOutputStream(targetFile);

            zos = new ZipOutputStream(new BufferedOutputStream(dest));

            for (File f : files) {
                byte[] bytes = readFileContents(f).toByteArray();
                ZipEntry entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                zos.write(bytes);
                zos.closeEntry();
            }

        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (zos != null)
                    zos.close();
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }

    }

    /**
     * Zips the set of files. It uses the Android native Zip mechanism
     * or Apache Common Compress for older Android versions (creating
     * malformed archives).
     *
     * @param files  the list of files of the target archive
     * @param target the target filename
     * @throws IOException
     */
    public static void zip(List<File> files, String target) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            zipInteroperable(files, target);
        } else {
            zipNative(files, target);
        }
    }

    /**
     * Android devices up to version 2.3.7 have a bug in the native
     * Zip archive creation, making the archive unreadable with some
     * programs.
     *
     * @param files  the list of files of the target archive
     * @param target the target filename
     * @throws IOException
     */
    public static void zipInteroperable(List<File> files, String target) throws IOException {
        ZipArchiveOutputStream aos = null;
        OutputStream out = null;
        try {
            File tarFile = new File(target);
            out = new FileOutputStream(tarFile);

            try {
                aos = (ZipArchiveOutputStream) new ArchiveStreamFactory()
                        .createArchiveOutputStream(ArchiveStreamFactory.ZIP,
                                out);
            } catch (ArchiveException e) {
                throw new IOException(e);
            }

            for (File file : files) {
                ZipArchiveEntry entry = new ZipArchiveEntry(file, file.getName());
                entry.setSize(file.length());
                aos.putArchiveEntry(entry);
                IOUtils.copy(new FileInputStream(file), aos);
                aos.closeArchiveEntry();
            }

        } catch (IOException e) {
            throw e;
        } finally {
            aos.finish();
            out.close();
        }
    }

    /**
     * Read the byte contents of a file into a {@link ByteArrayOutputStream}.
     *
     * @param f the file to read
     * @return the contents as {@link ByteArrayOutputStream}
     * @throws IOException
     */
    public static ByteArrayOutputStream readFileContents(File f)
            throws IOException {
        InputStream in = null;
        in = new FileInputStream(f);

        return readStreamContents(in);
    }

    public static ByteArrayOutputStream readStreamContents(InputStream in) throws IOException {
        try {
            byte[] buff = new byte[8000];
            int bytesRead = 0;

            ByteArrayOutputStream bao = new ByteArrayOutputStream();

            while ((bytesRead = in.read(buff)) != -1) {
                bao.write(buff, 0, bytesRead);
            }

            bao.flush();
            return bao;
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null)
                in.close();
        }
    }

    public static JsonObject readJsonContents(File f) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(f));

        CharSequence content = consumeBufferedReader(bufferedReader);

        JsonObject tou = new JsonParser().parse(content.toString()).getAsJsonObject();
        return tou;
    }

    public static CharSequence consumeInputStream(InputStream in) throws IOException {
        Scanner sc = new Scanner(in, "UTF-8");

        StringBuilder sb = new StringBuilder();
        String sep = System.getProperty("line.separator");
        while (sc.hasNext()) {
            sb.append(sc.nextLine());
            sb.append(sep);
        }
        sc.close();

        return sb;
    }

    private static CharSequence consumeBufferedReader(
            BufferedReader bufferedReader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line = "";

        while ((line = bufferedReader.readLine()) != null) {
            content.append(line);
        }

        bufferedReader.close();
        return content;
    }

    @SuppressLint("NewApi")
    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task, P... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    /**
     * Transform ISO 8601 string to Calendar.
     *
     * @param iso8601string
     * @return
     * @throws ParseException
     */
    public static long isoDateToLong(final String iso8601string) throws ParseException {
        //		Date date = isoDateFormat.parse(iso8601string.replace("Z", "+00:00"));
        //		return date.getTime();
        return jacksonFormat.parse(iso8601string).getTime();
    }

    /**
     * Returns the distance of two points in kilometers.
     *
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return distance in km
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] res = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, res);
        return res[0];
    }

    /**
     * Returns the distance of two measurements in kilometers.
     *
     * @param m1 first {@link Measurement}
     * @param m2 second {@link Measurement}
     * @return distance in km
     */
    public static double getDistance(Measurement m1, Measurement m2) {
        return getDistance(m1.getLatitude(), m1.getLongitude(), m2.getLatitude(), m2.getLongitude
                ());
    }

    public static String longToIsoDate(long time) {
        return jacksonFormat.format(new Date(time));
    }

    public static void saveContentsToFile(String content, File f) throws IOException {
        if (!f.exists()) {
            f.createNewFile();
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                f, false));

        bufferedWriter.write(content);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    /**
     * method to get the current version
     */
    public static String getVersionString(Context ctx) {
        StringBuilder out = new StringBuilder("Version ");
        try {
            out.append(getVersionStringShort(ctx));
            out.append(" (");
            out.append(ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode);
            out.append("), ");
        } catch (NameNotFoundException e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(
                    ctx.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            out.append(SimpleDateFormat.getInstance().format(new Date(time)));
            zf.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }

        return out.toString();
    }

    public static String getVersionStringShort(Context ctx) throws NameNotFoundException {
        return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
    }

    public static FileWithMetadata saveTrackAndReturnFile(Track t, boolean obfuscate) throws
            JSONException, IOException {
        //        File log = new File(resolveExternalStorageBaseFolder(), "enviroCar-track-"+t
        // .getTrackId()+".json");
        //        return new StreamTrackEncoder().createTrackJsonAsFile(t, obfuscate, log);#
        return null;
    }
}
