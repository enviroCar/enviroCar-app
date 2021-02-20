/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.core.dao;

import com.google.gson.JsonObject;

import org.envirocar.core.CacheDirectoryProvider;
import org.envirocar.core.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class AbstractCacheDAO {

    protected final CacheDirectoryProvider cacheDirectoryProvider;

    public AbstractCacheDAO() {
        cacheDirectoryProvider = null;
    }

    /**
     * Constructor.
     *
     * @param provider the directory provider.
     */
    public AbstractCacheDAO(CacheDirectoryProvider provider) {
        this.cacheDirectoryProvider = provider;
    }

    public JsonObject readCache(String cachedFile) throws IOException {
        assert cacheDirectoryProvider != null;
        File directory = cacheDirectoryProvider.getBaseFolder();

        File f = new File(directory, cachedFile);

        if (f.isFile()) {
            return Util.readJsonContents(f);
        }

        throw new IOException(String.format("Could not read file %s", cachedFile));
    }

    public boolean cacheFileExists(String cachedFile) {
        assert cacheDirectoryProvider != null;
        File directory = cacheDirectoryProvider.getBaseFolder();

        File f = new File(directory, cachedFile);

        return f.isFile();
    }

    protected void storeCache(String cacheFileName, String content) throws IOException {
        assert this.cacheDirectoryProvider != null;
        File file = new File(this.cacheDirectoryProvider.getBaseFolder(), cacheFileName);
        Util.saveContentsToFile(content, file);
    }

}
