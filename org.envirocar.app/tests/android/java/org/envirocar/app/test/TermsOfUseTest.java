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

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;

import junit.framework.Assert;

import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.remote.serializer.TermsOfUseSerializer;
import org.json.JSONException;
import org.junit.Test;

public class TermsOfUseTest extends ResourceLoadingTestCase {


	@Test
	public void testTermsOfUseInstanceParsing() throws JSONException, IOException {
		JsonElement gson = new Gson().fromJson(readJsonAsset("/terms_of_use_instance_mockup.json"), JsonElement.class);
		TermsOfUse tou = new TermsOfUseSerializer().deserialize(gson, null, null);
		
		Assert.assertTrue("Unexpected issuedDate", tou.getIssuedDate().equals("2022-06-09"));
		Assert.assertTrue("Unexpected contents", tou.getContents().equals("v50..."));
	}
	
}
