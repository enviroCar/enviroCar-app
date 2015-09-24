package org.envirocar.app.model.dao.service.utils;

import android.util.Log;

import com.google.common.base.Preconditions;

import org.apache.http.HttpStatus;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;

import retrofit.Response;

/**
 * @author dewall
 */
public class EnvirocarServiceUtils {

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
        if (httpStatusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            if (httpStatusCode == HttpStatus.SC_UNAUTHORIZED ||
                    httpStatusCode == HttpStatus.SC_FORBIDDEN) {
                throw new UnauthorizedException("Authentication failed: " + httpStatusCode + "; "
                        + error);
            } else if (httpStatusCode == HttpStatus.SC_CONFLICT) {
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

        // If the body is null, then return 0. Otherwise, return 1.
        return response.body() != null ? 1 : 0;
    }
}
