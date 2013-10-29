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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.envirocar.app.dao.AnnouncementsDAO;
import org.envirocar.app.dao.AnnouncementsRetrievalException;
import org.envirocar.app.dao.CacheDirectoryProvider;
import org.envirocar.app.model.Announcement;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

public class CacheAnnouncementsDAO implements AnnouncementsDAO {

	public static final String CACHE_FILE_NAME = "announcements";
	private CacheDirectoryProvider cacheProvider;

	public CacheAnnouncementsDAO(CacheDirectoryProvider cacheDirectoryProvider) {
		this.cacheProvider = cacheDirectoryProvider;
	}

	@Override
	public List<Announcement> getAllAnnouncements() throws AnnouncementsRetrievalException {
		JSONObject content;
		try {
			content = Util.readJsonContents(new File(this.cacheProvider.getBaseFolder(), CACHE_FILE_NAME));
			return Announcement.fromJsonList(content);
		} catch (IOException e) {
			throw new AnnouncementsRetrievalException(e);
		} catch (JSONException e) {
			throw new AnnouncementsRetrievalException(e);
		}
		
	}

	public void storeAllAnnouncements(String content) throws IOException {
		File file = new File(this.cacheProvider.getBaseFolder(), CACHE_FILE_NAME);
		Util.saveContentsToFile(content, file);
	}

}
