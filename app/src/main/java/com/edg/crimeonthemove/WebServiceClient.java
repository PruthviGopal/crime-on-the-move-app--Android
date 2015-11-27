package com.edg.crimeonthemove;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebServiceClient {

    public interface RawDataCommunicatorInterface {
        void useResults(List<Map<String, String>> jsonList);
    }

    public interface GeographicAreaResultsCommunicatorInterface {
        void useResults(Map<String, List<LatLng>> jsonMapOfLists,
                        Map<String, Map<String, String>> areaStatistics);
    }

    private static final String TAG = "WebServiceClient";
    private static final String BASE_URL = "http://52.23.201.83:5000/spatialdb/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static String getAbsoluteUrl(String url) {
        return BASE_URL + url;
    }

    private static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        Log.i(TAG, "RequestParams: " + params);
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static List<Map<String, String>> jsonArrayToList(JSONArray jsonArray) throws JSONException {
        List<Map<String, String>> jsonList = new ArrayList<Map<String, String>>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            Map<String, String> jsonMap = new HashMap<String, String>();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Iterator<String> jsonIt = jsonObject.keys();
            while (jsonIt.hasNext()) {
                String key = jsonIt.next();
                jsonMap.put(key, jsonObject.getString(key));
            }
            jsonList.add(jsonMap);
        }
        return jsonList;
    }

    private Cache mCache;

    public WebServiceClient() {
        int waitTime = 1000 * 800;
        client.setConnectTimeout(waitTime);
        client.setResponseTimeout(waitTime);
        mCache = new Cache();
    }

    /**
     * Get the number of crimes in DC.
     */
    public void getDCCount() {
        WebServiceClient.get("dc-count", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                Log.i(TAG, "JSONObject: " + response);

                int dcCount = -1;
                try {
                    dcCount = response.getInt("count");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "DC Count: " + dcCount);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getDCCount");
            }
        });
    }

    /**
     * Get all crimes in DC.
     */
    public void getDCCrimes(final RawDataCommunicatorInterface resultsCommunicator) {
        WebServiceClient.get("dc-crime", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                Log.i(TAG, "JSONObject: " + response);
                try {
                    JSONArray dcCrimesRows = response.getJSONArray("dc_crimes");
                    Log.i(TAG, "dcCrimesRows: " + dcCrimesRows);
                    List<Map<String, String>> jsonList = jsonArrayToList(dcCrimesRows);
                    resultsCommunicator.useResults(jsonList);
                    Log.i(TAG, "jsonList: " + jsonList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getDCCrimes");
            }
        });
    }

    /**
     * Get all crimes in DC.
     */
    public void getNovaCrimes(final RawDataCommunicatorInterface resultsCommunicator) {
        WebServiceClient.get("nova-crime", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                Log.i(TAG, "JSONObject: " + response);
                try {
                    JSONArray dcCrimesRows = response.getJSONArray("nova_crimes");
                    Log.i(TAG, "novaCrimesRows: " + dcCrimesRows);
                    List<Map<String, String>> jsonList = jsonArrayToList(dcCrimesRows);
                    resultsCommunicator.useResults(jsonList);
                    Log.i(TAG, "jsonList: " + jsonList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCrimes");
            }
        });
    }

    public void getCountyOutlines(final Context context,
            final GeographicAreaResultsCommunicatorInterface resultsCommunicator,
            Map<String, String> params) {

        Log.i(TAG, "getCountyOutlines called");
        Log.i(TAG, "Params: " + params);
        final RequestParams requestParameters = new RequestParams(params);
        Log.i(TAG, "ResuestParameters: " + requestParameters);
        // Set up handler for the main request to get nova counties
        final JsonHttpResponseHandler novaCountiesResponse = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                try {
                    mCache.insertNovaCountyData(context, response);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "JSONException IN onSuccess IN getCountyOutlines!");
                }
                resultsCommunicator.useResults(null, null);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + response);
            }
        };

        // Perform the checksum request, if the checksum doesn't match, pull new data
        WebServiceClient.get("counties_checksum", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int serverNovaCountyChecksum = response.getInt("count");
                    int clientNovaCountyChecksum = mCache.novaCountyRowsCheckSum(context);

                    if (serverNovaCountyChecksum != clientNovaCountyChecksum) {
                        Log.i(TAG, "Checksum mismatch! Retrieving nova county data from server!");
                        mCache.wipeCountyRows(context);
                        WebServiceClient.get("nova_counties", requestParameters, novaCountiesResponse);
                    } else {
                        Log.i(TAG, "Checksum match!!!! Retrieving nova county data from cache!");
                        resultsCommunicator.useResults(null, null);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines--checksum"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines--checksum"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + response);
            }
        });
    }

    /**
     * Get the clustering results from K-Means.
     */
    public void getKMeans(final GeographicAreaResultsCommunicatorInterface resultsCommunicator,
            Map<String, String> params) {
        Log.i(TAG, "getKMeans called");
        Log.i(TAG, "Params: " + params);
        RequestParams requestParameters = new RequestParams(params);
        Log.i(TAG, "ResuestParameters: " + requestParameters);
        WebServiceClient.get("clustering/kmeans", requestParameters, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                Log.i(TAG, "JSONObject: " + response);
                Map<String, List<LatLng>> clusterConvexHulls = new HashMap<String, List<LatLng>>();
                Map<String, Map<String, String>> clusterStatistics
                        = new HashMap<String, Map<String, String>>();
                try {
                    Utils.areaResultsProcessor(clusterConvexHulls, clusterStatistics, response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                resultsCommunicator.useResults(clusterConvexHulls, clusterStatistics);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + response);
            }
        });
    }

    /**
     * Get the clustering results from Spectral Clustering.
     */
    public void getSpectralClustering(final GeographicAreaResultsCommunicatorInterface resultsCommunicator,
            Map<String, String> params) {
        Log.i(TAG, "getSpectralClustering called");
        RequestParams requestParameters = new RequestParams(params);
        WebServiceClient.get("clustering/spectral-clustering", requestParameters, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                Log.i(TAG, "JSONObject: " + response);
                Map<String, List<LatLng>> clusterConvexHulls = new HashMap<String, List<LatLng>>();
                Map<String, Map<String, String>> clusterStatistics
                        = new HashMap<String, Map<String, String>>();
                try {
                    Utils.areaResultsProcessor(clusterConvexHulls, clusterStatistics, response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                resultsCommunicator.useResults(clusterConvexHulls, clusterStatistics);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
                Log.i(TAG, "JSONArray: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                    JSONObject response) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + headers
                        + "\nMessage: " + response);
            }
        });
    }

    /**
     * Get all crime in a particular DC ward.
     */
    public void getWard() {
        return;
    }

    /**
     * Get all crime in a particular county.
     */
    public void getCounty() {
        return;
    }

    /**
     * Get all crimes of a particular type. (split method?)
     */
    public void getParticularCrimeTypes() {
        return;
    }

    /**
     * Take a pair of LatLng coordinates defining a rectangle, return all data points that fall
     * within.
     *
     * @param latLngBounds pair of LatLng coordinates defining a rectangle.
     */
    public void getLocalDataPoints(LatLngBounds latLngBounds) {
        return;
    }
}