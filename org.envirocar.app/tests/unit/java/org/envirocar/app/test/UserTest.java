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
package org.envirocar.app.test;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.envirocar.app.model.User;
import org.json.JSONException;
import org.junit.Test;


public class UserTest extends ResourceLoadingTestCase {

	@Test
	public void testUserParsing() throws IOException, JSONException {
		InputStream is = getInstrumentation().getContext().getAssets().open("user_mockup.json");
		User user = User.fromJson(readJson(is));
		
		Assert.assertTrue("missing touVersion", user.getTouVersion() != null);
		Assert.assertTrue("unexpected acceptedTermsOfUseVersion", user.getTouVersion().equals("2013-10-02"));
		Assert.assertTrue("unexpected username", user.getUsername().equals("matthes"));
	}
	
}
