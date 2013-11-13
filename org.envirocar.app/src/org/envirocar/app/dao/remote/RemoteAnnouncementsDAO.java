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
package org.envirocar.app.dao.remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.dao.AnnouncementsDAO;
import org.envirocar.app.dao.cache.CacheAnnouncementsDAO;
import org.envirocar.app.dao.exception.AnnouncementsRetrievalException;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Announcement;
import org.envirocar.app.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteAnnouncementsDAO extends BaseRemoteDAO implements AnnouncementsDAO {

	private static final Logger logger = Logger.getLogger(RemoteAnnouncementsDAO.class);
	private CacheAnnouncementsDAO cache;

	public RemoteAnnouncementsDAO(CacheAnnouncementsDAO cacheAnnouncementsDAO) {
		this.cache = cacheAnnouncementsDAO;
	}

	@Override
	public List<Announcement> getAllAnnouncements() throws AnnouncementsRetrievalException {
		
		try {
			HttpGet get = new HttpGet(ECApplication.BASE_URL+"/announcements");
			InputStream response = super.retrieveHttpContent(get);
			String content = Util.consumeInputStream(response).toString();
		
			if (cache != null) {
				try {
					cache.storeAllAnnouncements(content);
				}
				catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			
			JSONObject parentObject = new JSONObject(content);
			
			return Announcement.fromJsonList(parentObject);
		} catch (IOException e) {
			throw new AnnouncementsRetrievalException(e);
		} catch (JSONException e) {
			throw new AnnouncementsRetrievalException(e);
		} catch (NotConnectedException e) {
			throw new AnnouncementsRetrievalException(e);
		} catch (UnauthorizedException e) {
			throw new AnnouncementsRetrievalException(e);
		}
	}

}
