package org.envirocar.app.model.service.utils;

import android.util.Log;

/**
 * @author dewall
 */
public class EnvirocarServiceUtils {

    /**
     * Searches a given link string for the 'rel=last' value. A link string can look like this:
     * <p/>
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
}
