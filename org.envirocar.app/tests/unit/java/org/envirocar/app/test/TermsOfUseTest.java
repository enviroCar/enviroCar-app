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

import junit.framework.Assert;

import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.json.JSONException;
import org.json.JSONObject;

public class TermsOfUseTest extends ResourceLoadingTestCase {

	
	public void testTermsOfUseParsing() throws JSONException, IOException {
		JSONObject json = new JSONObject(readJsonAsset("terms_of_use_mockup.json"));
		TermsOfUse tou = TermsOfUse.fromJson(json);
		
		Assert.assertTrue("Unexpected element count.", tou.getInstances().size() == 2);
		Assert.assertTrue("Unexpected element id.", tou.getInstances().get(0).getId().equals(
				"524bd2b6ff0bb8917e6f1665"));
		Assert.assertTrue("Unexpected element id.", tou.getInstances().get(1).getId().equals(
				"524bd1b0ff0bb8917e6f1663"));
	}
	

	public void testTermsOfUseInstanceParsing() throws JSONException, IOException {
		JSONObject json = new JSONObject(readJsonAsset("terms_of_use_instance_mockup.json"));
		TermsOfUseInstance tou = TermsOfUseInstance.fromJson(json);
		
		Assert.assertTrue("Unexpected issuedDate", tou.getIssuedDate().equals("2022-06-09"));
		Assert.assertTrue("Unexpected contents", tou.getContents().equals("v50..."));
	}
	
}
