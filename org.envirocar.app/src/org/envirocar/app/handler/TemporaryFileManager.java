/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.handler;

import android.content.Context;

import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class TemporaryFileManager {

    private static final Logger logger = Logger.getLogger(TemporaryFileManager.class);

    private final Context context;
    private final List<File> temporaryFiles = new ArrayList<File>();


    /**
     * Constructor that only injects the variables.
     *
     * @param context the application context.
     */
    @Inject
    public TemporaryFileManager(@InjectApplicationScope Context context) {
        this.context = context;
    }

    /**
     *
     */
    public void shutdown() {
        for (File f : this.temporaryFiles) {
            try {
                f.delete();
            } catch (RuntimeException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public File createTemporaryFile() {
        File result = new File(Util.resolveCacheFolder(context), UUID.randomUUID().toString());

        addTemporaryFile(result);

        return result;
    }

    private synchronized void addTemporaryFile(File result) {
        this.temporaryFiles.add(result);
    }

}
