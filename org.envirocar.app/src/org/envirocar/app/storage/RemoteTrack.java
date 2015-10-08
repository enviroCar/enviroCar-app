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
//package org.envirocar.app.storage;
//
//import java.util.ArrayList;
//
//public class RemoteTrack extends Track {
//
//	private String remoteID;
//
//	public RemoteTrack(String remoteID) {
//		this.remoteID = remoteID;
//		super.setupProperties();
//	}
//
//	public String getRemoteID() {
//		return remoteID;
//	}
//
//	public void setRemoteID(String remoteID) {
//		this.remoteID = remoteID;
//	}
//
//	@Override
//	public String toString() {
//		return "Remote Track / id: "+remoteID +" / Name: "+getName();
//	}
//
//
//	@Override
//	public boolean equals(Object o) {
//		if (this == o) return true;
//		if (o == null || getClass() != o.getClass()) return false;
//
//		RemoteTrack that = (RemoteTrack) o;
//
//        return remoteID != null && that.remoteID != null && remoteID.equals(that.remoteID);
//	}
//
//    /**
//     *
//     * @return
//     */
//    public boolean isDownloaded(){
//        return getCar() != null;
//    }
//
//    public void copyVariables(Track other){
//        this.name = other.name;
//        this.description = other.description;
//        this.measurements = new ArrayList<>(other.getMeasurements());
//        this.car = other.car;
//        this.consumptionAlgorithm = other.consumptionAlgorithm;
//        this.consumptionPerHour = other.consumptionPerHour;
//        this.status = other.status;
//        this.lazyLoadingMeasurements = other.lazyLoadingMeasurements;
//        this.startTime = other.startTime;
//        this.endTime = other.endTime;
//        this.metadata = other.metadata;
//    }
//}
