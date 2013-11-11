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
package org.envirocar.app.test.dao;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.envirocar.app.dao.exception.TrackRetrievalException;
import org.envirocar.app.json.TrackDecoder;

import android.test.AndroidTestCase;

public class TrackDecoderTest extends AndroidTestCase {

	public void testTotalTrackCount() throws TrackRetrievalException {
		BasicHttpResponse response = new BasicHttpResponse(createStatusLine());
		response.setHeader("Link", "<https://envirocar.org/api/stable/users/matthes/tracks?limit=1&page=7>;rel=last;type=application/json, <https://envirocar.org/api/stable/users/matthes/tracks?limit=1&page=2>;rel=next;type=application/json");
		Integer count = new TrackDecoder().resolveTrackCount(response);
		
		Assert.assertTrue(count.intValue() == 7);
		
		response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 204, ""));
		response.setHeader("Link", "<https://envirocar.org/api/stable/users/matthes/tracks?page=6>;rel=last");
		count = new TrackDecoder().resolveTrackCount(response);
		
		Assert.assertTrue(count.intValue() == 6);

	}
	
	private StatusLine createStatusLine() {
		return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 204, "");
	}

	public void testLocationParsing() throws TrackRetrievalException {
		HttpResponse resp = new BasicHttpResponse(createStatusLine());
		
		resp.setHeader("Location", "http:/this.is.my.envirocar.server/api/tracks/1337-resource");
		
		String result = new TrackDecoder().resolveLocation(resp);
		
		Assert.assertTrue(result.equals("1337-resource"));
	}
	
}
