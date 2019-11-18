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
package org.envirocar.app.test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;

import junit.framework.Assert;

import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.remote.serde.TermsOfUseSerde;
import org.json.JSONException;
import org.junit.Test;

public class TermsOfUseTest extends ResourceLoadingTestCase {


	@Test
	public void testTermsOfUseInstanceParsing() throws JSONException, IOException {
		JsonElement gson = new Gson().fromJson(readJsonAsset("/terms_of_use_instance_mockup.json"), JsonElement.class);
		TermsOfUse tou = new TermsOfUseSerde().deserialize(gson, null, null);
		
		Assert.assertTrue("Unexpected issuedDate", tou.getIssuedDate().equals("2022-06-09"));
		Assert.assertTrue("Unexpected contents", tou.getContents().equals("v50..."));
	}
	
}
