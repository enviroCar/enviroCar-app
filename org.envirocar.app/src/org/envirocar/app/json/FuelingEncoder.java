///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.json;
//
//import org.envirocar.app.exception.InvalidObjectStateException;
//import org.envirocar.app.model.Fueling;
//import org.envirocar.app.util.Util;
//import org.json.JSONException;
//import org.json.JSONObject;
//
///**
// * Encode {@link Fueling} objects to JSON
// */
//public class FuelingEncoder {
//
//	private static final String COMMENT = "comment";
//	private static final String MISSED_FUEL_STOP = "missedFuelStop";
//	private static final String CAR = "car";
//	private static final String FUEL_TYPE = "fuelType";
//	private static final String COST = "cost";
//	private static final String MILEAGE = "mileage";
//	private static final String VOLUME = "volume";
//	private static final String TIME = "time";
//
//	public JSONObject createFuelingJson(Fueling fueling) throws InvalidObjectStateException, JSONException {
//		assertAllRequiredElementsPresent(fueling);
//
//		JSONObject result = new JSONObject();
//
//		if (fueling.getComment() != null) {
//			result.put(COMMENT, fueling.getComment());
//		}
//
//		result.put(MISSED_FUEL_STOP, fueling.isMissedFuelStop());
//		result.put(CAR, fueling.getCar().getId());
//		result.put(FUEL_TYPE, fueling.getCar().getFuelType().toString());
//		result.put(COST, fueling.getCost().toJson());
//		result.put(MILEAGE, fueling.getMileage().toJson());
//		result.put(VOLUME, fueling.getVolume().toJson());
//		result.put(TIME, Util.longToIsoDate(fueling.getTime().getTime()));
//
//		return result;
//	}
//
//	private void assertAllRequiredElementsPresent(Fueling fueling) throws InvalidObjectStateException {
//		if (fueling.getCost() != null && fueling.getMileage() != null
//				&& fueling.getVolume() != null && fueling.getTime() != null
//				&& fueling.getCar() != null) {
//			return;
//		}
//
//		throw new  InvalidObjectStateException(
//				"The Fueling object did not contain all required elements.");
//	}
//
//}
