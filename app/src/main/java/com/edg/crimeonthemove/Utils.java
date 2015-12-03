package com.edg.crimeonthemove;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class with static methods.
 * Right now it only has JSON utilities.....There may be a rename in this class' future.
 */
public class Utils {

    private static final String TAG = "Utils";

    /**
     * Takes a Map<Integer, List<LatLng>> to represent a mapping from area IDs to points of a boundary
     * and a JSONObject which is retrieved from area results. Parses the area results
     * from JSONObject and places them in areaBoundaries (the Map<Integer, List<LatLng>>.
     * Throws a JSONException which may occur while parsing the JSONObject, response.
     *
     * @param areaBoundaries Map<String, List<LatLng>> to represent a mapping
     *                           from cluster IDs to convex hulls.
     * @param areaStatistics Map<String, Map<String, String>> to represent a mapping
     *                          from area IDs to specifically marked statistics.
     * @param response a JSONObject which is to be parsed.
     * @throws JSONException
     */
    public static void areaResultsProcessor(Map<String, List<LatLng>> areaBoundaries,
            Map<String, Map<String, String>> areaStatistics, JSONObject response)
            throws JSONException {
        // Parse out arrays of points describing boundaries
        JSONObject convexHullsObject = response.getJSONObject("area_outline");
        Iterator<String> keyIt = convexHullsObject.keys();
        String key;
        JSONArray jsonAreaPoints;
        while (keyIt.hasNext()) {
            key = keyIt.next();
            jsonAreaPoints = convexHullsObject.getJSONArray(key);
            List<LatLng> areaBoundaryPoints = new ArrayList<>();
            parseArea(areaBoundaryPoints, jsonAreaPoints);
            areaBoundaries.put(key, areaBoundaryPoints);
        }

        // Parse out statistics for each area, if present, if not, no problem
        try {
            JSONObject statisticsObject = response.getJSONObject("statistics");
            JSONObject jsonClusterStatistics;
            keyIt = statisticsObject.keys();
            while (keyIt.hasNext()) {
                key = keyIt.next();
                jsonClusterStatistics = statisticsObject.getJSONObject(key);
                Map<String, String> specificClusterStatistics = new HashMap<>();
                parseStatistics(specificClusterStatistics, jsonClusterStatistics);
                areaStatistics.put(key, specificClusterStatistics);
            }
        } catch (JSONException exception) {
            Log.i(TAG, "No statistics with this area.");
            exception.printStackTrace();
        }
    }

    /**
     * Takes a List<LatLng> to represent points of a boundary
     * and a JSONArray assumed to be holding y_cord and x_cord fields. Parses the area results
     * from the JSONArray and places them in areaBoundary (the List<LatLng>).
     * Throws a JSONException which may occur while parsing the JSONObject, response.
     *
     * @param areaBoundary List<LatLng> to represent a set of points defining a boundary.
     * @param jsonAreaPoints an array of points in a JSONArray to be parsed.
     * @throws JSONException
     */
    public static void parseArea(List<LatLng> areaBoundary, JSONArray jsonAreaPoints)
            throws JSONException {
        // Parse out the list of points defining an area.
        JSONObject jsonPoint;
        for (int i = 0; i < jsonAreaPoints.length(); i++) {
            jsonPoint = jsonAreaPoints.getJSONObject(i);
            double latitude = Double.parseDouble(jsonPoint.getString("y_cord"));
            double longitude = Double.parseDouble(jsonPoint.getString("x_cord"));
            areaBoundary.add(new LatLng(latitude, longitude));
        }
    }

    /**
     *
     * @param statistics
     * @param jsonStatistics
     */
    public static void parseStatistics(Map<String, String> statistics, JSONObject jsonStatistics)
            throws JSONException {
        Iterator<String> clusterStatisticsKeyIt = jsonStatistics.keys();
        String clusterStatisticsKey;
        String clusterStatistic;
        while (clusterStatisticsKeyIt.hasNext()) {
            clusterStatisticsKey = clusterStatisticsKeyIt.next();
            clusterStatistic = jsonStatistics.getString(clusterStatisticsKey);
            statistics.put(clusterStatisticsKey, clusterStatistic);
        }
    }
}
