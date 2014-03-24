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
package org.envirocar.app.dao.cache;

import java.io.IOException;
import java.util.List;

import org.envirocar.app.dao.AnnouncementsDAO;
import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.dao.exception.AnnouncementsRetrievalException;
import org.envirocar.app.model.Announcement;
import org.json.JSONException;

public class CacheAnnouncementsDAO extends AbstractCacheDAO implements AnnouncementsDAO {

	public static final String CACHE_FILE_NAME = "announcements";

	public CacheAnnouncementsDAO(CacheDirectoryProvider cacheDirectoryProvider) {
		super(cacheDirectoryProvider);
	}

	@Override
	public List<Announcement> getAllAnnouncements() throws AnnouncementsRetrievalException {
		try {
			return Announcement.fromJsonList(readCache(CACHE_FILE_NAME));
		} catch (IOException e) {
			throw new AnnouncementsRetrievalException(e);
		} catch (JSONException e) {
			throw new AnnouncementsRetrievalException(e);
		}
		
	}

	public void storeAllAnnouncements(String content) throws IOException {
		storeCache(CACHE_FILE_NAME, content);
	}

}
