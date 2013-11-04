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

import java.util.List;

import org.apache.http.client.methods.HttpDelete;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.application.User;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.NotConnectedException;
import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.storage.Track;

public class RemoteTrackDAO extends BaseRemoteDAO implements TrackDAO, AuthenticatedDAO {

	@Override
	public void deleteTrack(String remoteID) throws NotLoggedInException, NotConnectedException {
		User user = UserManager.instance().getUser();
		
		if (user == null) {
			throw new NotLoggedInException();
		}
		
		HttpDelete request = new HttpDelete(ECApplication.BASE_URL+"/users/"+
				user.getUsername()+"/tracks/" + remoteID);

		super.executeHttpRequest(request);
	}

	@Override
	public void storeTrack(Track track) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Track> getAllTracks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Track getTrack(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
