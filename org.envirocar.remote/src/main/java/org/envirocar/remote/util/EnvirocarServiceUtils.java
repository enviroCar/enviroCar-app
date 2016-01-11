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
package org.envirocar.remote.util;

import android.util.Log;

import com.google.common.base.Preconditions;

import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.Response;

/**
 * @author dewall
 */
public class EnvirocarServiceUtils {
    private static final Logger LOG = Logger.getLogger(EnvirocarServiceUtils.class);

    public static final int HTTP_MULTIPLE_CHOICES = 300;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;

    /**
     * Searches a given link string for the 'rel=last' value. A link string can look like this:
     * <p>
     * <https://envirocar.org/api/stable/sensors/?limit=100&page=3>;rel=last;type=application/json,
     * <https://envirocar.org/api/stable/sensors/?limit=100&page=2>;rel=next;type=application/json
     *
     * @param linkString the link string to search for the specific value.
     * @return the number of pages, which was encoded in the link string.
     */
    public static final Integer getLastRelValueOfLink(String linkString) {
        if (linkString == null || linkString.isEmpty() || linkString.equals("")) {
            Log.w("EnvirocarServiceUtils", "Input string was null or empty");
            return 0;
        }

        // Split the input string at the komma
        String[] split = linkString.split(",");

        // iterate through the array and try to find 'rel=last'
        for (String line : split) {
            if (line.contains("rel=last")) {
                String[] params = line.split(";");
                if (params != null && params.length > 0) {
                    // When found, then resolve the page value.
                    return resolvePageValue(params[0]);
                }
            }
        }

        // Not found
        Log.w("EnvirocarServiceUtils",
                "rel=last not found in the input string. Therefore, return 0");
        return 0;
    }

    /**
     * Extracts the page value contained in the url of the link header.
     *
     * @param sourceUrl the url to scan for the page value.
     * @return the page value of the url;
     */
    private static final Integer resolvePageValue(String sourceUrl) {
        String url;

        // if the string starts with < and ends with >, then cut these chars.
        if (sourceUrl.startsWith("<")) {
            url = sourceUrl.substring(1, sourceUrl.length() - 1);
        } else {
            url = sourceUrl;
        }

        if (url.contains("?")) {
            int index = url.indexOf("?") + 1;
            if (index != url.length()) {
                String params = url.substring(index, url.length());
                for (String kvp : params.split("&")) {
                    if (kvp.startsWith("page")) {
                        return Integer.parseInt(kvp.substring(kvp.indexOf("page") + 5));
                    }
                }
            }
        }
        return null;
    }

    public static final void assertStatusCode(int httpStatusCode, String error) throws
            UnauthorizedException, NotConnectedException, ResourceConflictException {
        if (httpStatusCode >= HTTP_MULTIPLE_CHOICES) {
            if (httpStatusCode == HTTP_UNAUTHORIZED ||
                    httpStatusCode == HTTP_FORBIDDEN) {
                throw new UnauthorizedException("Authentication failed: " + httpStatusCode + "; "
                        + error);
            } else if (httpStatusCode == HTTP_CONFLICT) {
                throw new ResourceConflictException(error);
            } else {
                throw new NotConnectedException("Unsupported Server response: " + httpStatusCode
                        + "; " + error);
            }
        }
    }

    /**
     * Checks whether the response's corresponding page has a next page or not.
     *
     * @param response the response of a request.
     * @return true if the response does not correspond to that of the last page.
     */
    public static final boolean hasNextPage(Response<?> response) {
        Preconditions.checkNotNull(response, "Response input cannot be null!");

        // Get the header als multimap.
        Map<String, List<String>> headerListMap = response.headers().toMultimap();
        if (headerListMap.containsKey("Link")) {

            // Iterate over all Link entries in the link header and if there exist one containing a
            // "rel=last", then return true;
            for (String header : headerListMap.get("Link")) {
                if (header.contains("rel=last")) {
                    return true;
                }
            }
        }
        // Otherwise, return false.
        return false;
    }

    /**
     * Resolves the number of pages encoded in the link header.
     *
     * @param response the response of a call.
     * @return the number of pages.
     */
    public static final int resolvePageCount(Response<?> response) {
        Preconditions.checkNotNull(response, "Input response cannot be null!");

        // Get the header als multimap.
        Map<String, List<String>> headerListMap = response.headers().toMultimap();
        if (headerListMap.containsKey("Link")) {
            for (String header : headerListMap.get("Link")) {
                if (header.contains("rel=last")) {
                    String[] params = header.split(";");
                    if (params != null && params.length > 0) {
                        String sourceUrl = params[0];

                        // if the string starts with < and ends with >, then cut these chars.
                        if (sourceUrl.startsWith("<")) {
                            sourceUrl = sourceUrl.substring(1, sourceUrl.length() - 1);
                        }

                        if (sourceUrl.contains("?")) {
                            int index = sourceUrl.indexOf("?") + 1;
                            if (index != sourceUrl.length()) {
                                String parames = sourceUrl.substring(index, sourceUrl.length());
                                // find the "page=..." substring
                                for (String kvp : parames.split("&")) {
                                    if (kvp.startsWith("page")) {
                                        // Parse the value as integer.
                                        return Integer.parseInt(kvp.substring(
                                                kvp.indexOf("page") + 5));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // If the body is null, then return 0. Otherwise, return 1. // TODO
        return response.body() != null ? 0 : 0;
    }

    /**
     * Resolves the remote location of an successful upload response.
     *
     * @param response the successful response type.
     * @return the remote location of the uploaded entity.
     */
    public static String resolveRemoteLocation(Response<?> response) {
        Preconditions.checkNotNull(response, "Response type can not be null.");
        Preconditions.checkState(response.isSuccess(), "Response has to be succesful to be able " +
                "to resolve the uploaded location");

        // Get all headers in order to find out the location of the uploaded car.
        Map<String, List<String>> headerListMap = response.headers().toMultimap();

        // Get the header of the location
        String location = "";
        if (headerListMap.containsKey("Location")) {
            for (String locationHeader : headerListMap.get("Location")) {
                location += locationHeader;
            }
        }

        // Returns the location
        return location;
    }

    public static String resolveRemtoteID(String remoteLocation) {
        String[] split = remoteLocation.split("/");

        String remoteID = split[split.length - 1];
        if (remoteID == "" || remoteID.isEmpty()) {
            remoteID = split[split.length-2];
        }

        return remoteID;
    }


    /**
     * resolve all not obfuscated measurements of a track.
     * <p>
     * This returns all measurements, if obfuscation is disabled. Otherwise
     * measurements within the first and last minute and those within the start/end
     * radius of 250 m are ignored (only if they are in the beginning/end of the track).
     *
     * @param track
     * @return
     */
    public static Track getNonObfuscatedMeasurements(Track track, boolean obfuscate) {
        List<Measurement> measurements = track.getMeasurements();


        if (obfuscate) {
            boolean wasAtLeastOneTimeNotObfuscated = false;
            ArrayList<Measurement> privateCandidates = new ArrayList<Measurement>();
            ArrayList<Measurement> nonPrivateMeasurements = new ArrayList<Measurement>();
            for (Measurement measurement : measurements) {
                try {
                    /*
                     * ignore early and late
					 */
                    if (isTemporalObfuscationCandidate(measurement, track)) {
                        continue;
                    }

					/*
                     * ignore distance
					 */
                    if (isSpatialObfuscationCandidate(measurement, track)) {
                        if (wasAtLeastOneTimeNotObfuscated) {
                            privateCandidates.add(measurement);
                            nonPrivateMeasurements.add(measurement);
                        }
                        continue;
                    }

					/*
                     * we may have found obfuscation candidates in the middle of the track
					 * (may cross start or end point) in a PRIOR iteration
					 * of this loop. these candidates can be removed now as we are again
					 * out of obfuscation scope
					 */
                    if (wasAtLeastOneTimeNotObfuscated) {
                        privateCandidates.clear();
                    } else {
                        wasAtLeastOneTimeNotObfuscated = true;
                    }

                    nonPrivateMeasurements.add(measurement);
                } catch (NoMeasurementsException e) {
                    LOG.warn(e.getMessage(), e);
                }

            }
            /*
             * the private candidates which have made it until here
			 * shall be ignored
			 */
            nonPrivateMeasurements.removeAll(privateCandidates);
            track.setMeasurements(privateCandidates);
        }

        return track;
    }

    /**
     * TODO a circular criteria could lead to
     *
     * @param measurement
     * @param track
     * @return
     */
    private static boolean isSpatialObfuscationCandidate(Measurement measurement, Track track)
            throws NoMeasurementsException {
        return (Util.getDistance(track.getFirstMeasurement(), measurement) <= 0.25)
                || (Util.getDistance(track.getLastMeasurement(), measurement) <= 0.25);
    }

    private static boolean isTemporalObfuscationCandidate(Measurement measurement, Track track)
            throws
            NoMeasurementsException {
        return (measurement.getTime() - track.getStartTime() <= 60000 ||
                track.getEndTime() - measurement.getTime() <= 60000);
    }
}
