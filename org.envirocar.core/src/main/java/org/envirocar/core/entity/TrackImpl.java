/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.core.entity;

import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProcessor;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.core.util.TrackMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class TrackImpl implements Track, TrackStatisticsProvider {
    private static final Logger LOG = Logger.getLogger(TrackImpl.class);
    private TrackStatisticsProcessor STATISTICS_PROCESSOR;

    protected TrackId trackID;
    protected String remoteID;
    protected String name;
    protected String description;
    protected Car car;
    protected Long lastModified;
    protected Long startTime;
    protected Long endTime;
    protected TrackMetadata metadata;
    protected Track.TrackStatus trackStatus = Track.TrackStatus.ONGOING;
    protected List<Measurement> measurements = new ArrayList<Measurement>();
    protected DownloadState downloadState;

    protected boolean isLazyLoadingMeasurements = false;

    protected Double distanceOfTrack;
    protected Double consumptionPerHour;
    protected Double co2Average;
    protected Double literPerHundredKm;
    protected Double gramsPerKm;

    /**
     * Default constructor with downloaded state.
     */
    public TrackImpl() {
        this(DownloadState.DOWNLOADED);
    }

    /**
     * Constructor.
     *
     * @param downloadState the state of the track whether it is a re
     */
    public TrackImpl(DownloadState downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public TrackId getTrackID() {
        return trackID;
    }

    @Override
    public void setTrackID(TrackId trackID) {
        this.trackID = trackID;
    }

    @Override
    public String getRemoteID() {
        return remoteID;
    }

    @Override
    public void setRemoteID(String remoteID) {
        this.remoteID = remoteID;
    }

    @Override
    public boolean isLazyLoadingMeasurements() {
        return false;
    }

    @Override
    public void setLazyMeasurements(boolean lazyLoadingMeasurements) {
        this.isLazyLoadingMeasurements = lazyLoadingMeasurements;
    }

    @Override
    public Track carbonCopy() {
        Track track = new TrackImpl(downloadState);
        track.setTrackID(trackID);
        track.setRemoteID(remoteID);
        track.setName(name);
        track.setDescription(description);
        track.setCar(car);
        track.setStartTime(startTime);
        track.setEndTime(endTime);
        track.setMetadata(metadata);
        track.setTrackStatus(trackStatus);
        track.setMeasurements(new ArrayList<>(measurements));
        track.setLazyMeasurements(isLazyLoadingMeasurements);
        return track;
    }

    @Override
    public boolean isDownloaded() {
        return car != null;
    }

    @Override
    public boolean isDownloading() {
        return downloadState == DownloadState.DOWNLOADING;
    }

    @Override
    public DownloadState getDownloadState() {
        return this.downloadState;
    }

    @Override
    public void setDownloadState(DownloadState downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Car getCar() {
        return car;
    }

    @Override
    public void setCar(Car car) {
        this.car = car;
        this.STATISTICS_PROCESSOR = new TrackStatisticsProcessor(car.getFuelType());
    }

    @Override
    public boolean isLocalTrack() {
        return remoteID == null;
    }

    @Override
    public boolean isRemoteTrack() {
        return !isLocalTrack();
    }

    @Override
    public Long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public Long getStartTime() throws NoMeasurementsException {
        if (startTime == null) {
            setStartTime(getFirstMeasurement().getTime());
        }
        return startTime;
    }

    @Override
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    @Override
    public Long getEndTime() throws NoMeasurementsException {
        if (endTime == null) {
            setEndTime(getLastMeasurement().getTime());
        }
        return endTime;
    }

    @Override
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    @Override
    public long getDuration() throws NoMeasurementsException {
        return getEndTime() - getStartTime();
    }

    @Override
    public TrackStatus getTrackStatus() {
        return trackStatus;
    }

    @Override
    public void setTrackStatus(TrackStatus trackStatus) {
        this.trackStatus = trackStatus;
    }

    @Override
    public boolean isFinished() {
        return this.trackStatus == TrackStatus.FINISHED;
    }

    @Override
    public TrackMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(TrackMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public TrackMetadata updateMetadata(TrackMetadata metadata) {
        if (this.metadata != null) {
            this.metadata.merge(metadata);
        } else {
            setMetadata(metadata);
        }

        return this.metadata;
    }

    @Override
    public Measurement getFirstMeasurement() throws NoMeasurementsException {
        if (measurements.isEmpty()) {
            throw new NoMeasurementsException("Track with no measurements!");
        }
        return measurements.get(0);
    }

    @Override
    public Measurement getLastMeasurement() throws NoMeasurementsException {
        if (measurements.isEmpty()) {
            throw new NoMeasurementsException("Track with no measurements!");
        }
        return measurements.get(measurements.size() - 1);
    }

    @Override
    public List<Measurement> getMeasurements() {
        return measurements;
    }

    @Override
    public void setMeasurements(List<Measurement> measurements) {
        //        Preconditions.checkState(measurements != null && measurements.size() > 0, "A
        // track is not" +
        //                " allowed to have empty measuremnts");
        this.measurements = measurements;
    }

    @Override
    public int compareTo(Track another) {
        if (downloadState == DownloadState.REMOTE) {
            if (another.getDownloadState() == DownloadState.REMOTE) {
                lastModified.compareTo(another.getLastModified());
            } else {
                return 1;
            }
        } else {
            if (another.getDownloadState() == DownloadState.REMOTE) {
                return -1;
            }
        }

        try {
            if (another.getStartTime() == null && another.getEndTime() == null) {
                /*
                 * we cannot assume any ordering
				 */
                return 0;
            }
        } catch (NoMeasurementsException e) {
            return 0;
        }

        try {
            if (this.getStartTime() == null) {
                /*
                 * no measurements, this is probably a relatively new track
				 */
                return -1;
            }
        } catch (NoMeasurementsException e) {
            return -1;
        }

        try {
            if (another.getStartTime() == null) {
                /*
                 * no measurements, that is probably a relatively new track
				 */
                return 1;
            }
        } catch (NoMeasurementsException e) {
            return 1;
        }

        try {
            return (this.getStartTime() < another.getStartTime() ? 1 : -1);
        } catch (NoMeasurementsException e) {
            return 0;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        if (!(o instanceof TrackImpl))
            return false;

        TrackImpl track = (TrackImpl) o;
        if (remoteID != null && track.getRemoteID() != null) {
            return remoteID.equals(track.getRemoteID());
        } else if (trackID != null && track.trackID != null) {
            return trackID.getId() == track.trackID.getId();
        } else {
            if (trackID != null ? !trackID.equals(track.trackID) : track.trackID != null)
                return false;
            if (name != null ? !name.equals(track.name) : track.name != null) return false;
            if (description != null ? !description.equals(track.description) : track.description !=
                    null)
                return false;
            if (car != null ? !car.equals(track.car) : track.car != null) return false;
            if (startTime != null ? !startTime.equals(track.startTime) : track.startTime != null)
                return false;
            if (endTime != null ? !endTime.equals(track.endTime) : track.endTime != null)
                return false;
            return !(metadata != null ? !metadata.equals(track.metadata) : track.metadata != null);
        }
    }


    @Override
    public int hashCode() {
        int result = trackID != null ? trackID.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (car != null ? car.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }


    @Override
    public double getDistanceOfTrack() {
        if (distanceOfTrack == null) {
            distanceOfTrack = STATISTICS_PROCESSOR.computeDistanceOfTrack(getMeasurements());
        }
        return distanceOfTrack;
    }

    @Override
    public double getFuelConsumptionPerHour() throws FuelConsumptionException {
        if (consumptionPerHour == null) {
            consumptionPerHour = STATISTICS_PROCESSOR.getFuelConsumptionPerHour(getMeasurements());
        }
        return consumptionPerHour;
    }

    @Override
    public double getCO2Average() throws FuelConsumptionException {
        if (co2Average == null) {
            co2Average = STATISTICS_PROCESSOR.getCO2Average(getMeasurements());
        }
        return co2Average;
    }

    @Override
    public double getLiterPerHundredKm() throws FuelConsumptionException, NoMeasurementsException {
        if (literPerHundredKm == null) {
            literPerHundredKm = STATISTICS_PROCESSOR.getLiterPerHundredKm(
                    getFuelConsumptionPerHour(), getDuration(), getDistanceOfTrack());
        }
        return literPerHundredKm;
    }

    @Override
    public double getGramsPerKm() throws FuelConsumptionException, NoMeasurementsException,
            UnsupportedFuelTypeException {
        if (gramsPerKm == null) {
            gramsPerKm = STATISTICS_PROCESSOR.getGramsPerKm(getLiterPerHundredKm(), getCar()
                    .getFuelType());
        }
        return gramsPerKm;
    }

}
