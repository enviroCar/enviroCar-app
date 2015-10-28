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
        File directory = cacheDirectoryProvider.getBaseFolder();

        File f = new File(directory, cachedFile);

        if (f.isFile()) {
            JsonObject tou = Util.readJsonContents(f);
            return tou;
        }

        throw new IOException(String.format("Could not read file %s", cachedFile));
    }

    public boolean cacheFileExists(String cachedFile) {
        File directory = cacheDirectoryProvider.getBaseFolder();

        File f = new File(directory, cachedFile);

        return f != null && f.isFile();
    }

    protected void storeCache(String cacheFileName, String content) throws IOException {
        File file = new File(this.cacheDirectoryProvider.getBaseFolder(), cacheFileName);
        Util.saveContentsToFile(content, file);
    }

}
