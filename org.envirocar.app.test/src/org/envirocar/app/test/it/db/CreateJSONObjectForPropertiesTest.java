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
package org.envirocar.app.test.it.db;

import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Measurement.PropertyKey;
import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;

public class CreateJSONObjectForPropertiesTest extends InstrumentationTestCase {

	private DbAdapterImpl dbAdapter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		DbAdapterImpl.init(getInstrumentation().getTargetContext());
		this.dbAdapter = (DbAdapterImpl) DbAdapterImpl.instance();
	}
	
	public void testJsonCreation() throws JSONException {
		Measurement m = new Measurement(2.0, 3.0);
		m.setProperty(PropertyKey.CONSUMPTION, 35.5);
		JSONObject result = this.dbAdapter.createJsonObjectForProperties(m);
		
		assertEquals(result.getDouble(PropertyKey.CONSUMPTION.name()), 35.5);
	}
	
}
