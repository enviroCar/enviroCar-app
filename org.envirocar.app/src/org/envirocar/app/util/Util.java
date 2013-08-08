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
package org.envirocar.app.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.envirocar.app.logging.Logger;

import android.os.Environment;

public class Util {

	private static final Logger logger = Logger.getLogger(Util.class);
	public static final String EXTERNAL_SUB_FOLDER = "enviroCar";

	/**
	 * Create a file in the .enviroCar folder of the
	 * external storage.
	 * 
	 * @param fileName the name of the new file
	 * @return the resulting file
	 * @throws IOException
	 */
	public static File createFileOnExternalStorage(String fileName)
			throws IOException {
		File directory = new File(Environment.getExternalStorageDirectory()
				+ File.separator + EXTERNAL_SUB_FOLDER);
		if (!directory.exists()) {
			directory.mkdir();
		}
		if (!directory.isDirectory()) {
			throw new IOException(directory.getAbsolutePath()
					+ " is not a directory!");
		}
		File f = new File(directory, fileName);
		f.createNewFile();
		if (!f.isFile()) {
			throw new IOException(fileName + " is not a file!");
		}
		return f;
	}

	/**
	 * Zips a list of files into the target archive.
	 * 
	 * @param files the list of files of the target archive
	 * @param target the target filename
	 */
	public static void zip(List<File> files, String target) {
		ZipOutputStream zos = null;
		try {
			FileOutputStream dest = new FileOutputStream(target);

			zos = new ZipOutputStream(
					new BufferedOutputStream(dest));
			
			for (File f : files) {
				byte[] bytes = readFileContents(f).toByteArray();
				ZipEntry entry = new ZipEntry(f.getName());
				zos.putNextEntry(entry);
				zos.write(bytes);
				zos.closeEntry();				
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
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
	 * Read the byte contents of a file into
	 * a {@link ByteArrayOutputStream}.
	 * 
	 * @param f the file to read
	 * @return the contents as {@link ByteArrayOutputStream}
	 * @throws IOException
	 */
	public static ByteArrayOutputStream readFileContents(File f) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(f);

	        byte[] buff = new byte[8000];
	        int bytesRead = 0;

	        ByteArrayOutputStream bao = new ByteArrayOutputStream();

	        while((bytesRead = in.read(buff)) != -1) {
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


	public static final String NEW_LINE_CHAR = System
	.getProperty("line.separator");

}
