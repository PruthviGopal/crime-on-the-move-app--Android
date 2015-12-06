package com.edg.crimeonthemove;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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

import java.util.Arrays;
import java.util.HashMap;
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

    // End static fields/methods--------------------------------------------------------------------

    private Cache mCache;
    private Handler mMainHandler;

    public WebServiceClient() {
        int waitTime = 1000 * 800;
        client.setConnectTimeout(waitTime);
        client.setResponseTimeout(waitTime);
        mCache = new Cache();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get all crimes in DC.
     */
    public void getDCCrimes(final Context context, final RawDataCommunicatorInterface resultsCommunicator) {
        final JsonHttpResponseHandler firstDcCrimesResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                try {
                    JSONArray dcCrimesRows = response.getJSONArray("dc_crimes");
                    mCache.insertDcCrimeData(context, dcCrimesRows);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getDCCrimes");
            }
        };
        final JsonHttpResponseHandler dcCrimesResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                try {
                    JSONArray dcCrimesRows = response.getJSONArray("dc_crimes");
                    mCache.insertDcCrimeData(context, dcCrimesRows);
                    resultsCommunicator.useResults(null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getDCCrimes");
            }
        };

        WebServiceClient.get("dc-crimes-checksum", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                try {
                    int serverDCCrimeChecksum = response.getInt("count");
                    int clientDCCrimeChecksum = mCache.dcCrimeRowsChecksum(context);

                    if (serverDCCrimeChecksum != clientDCCrimeChecksum) {
                        Log.i(TAG, "Checksum mismatch! Retrieving DC crime data from server!");
                        mCache.wipeDcCrimeRows(context);
                        WebServiceClient.get("dc-crime", null, firstDcCrimesResponseHandler);
                        WebServiceClient.get("dc-crime-2", null, dcCrimesResponseHandler);
                    } else {
                        Log.i(TAG, "Checksum match!!!! Retrieving DC crime data from cache!");
                        resultsCommunicator.useResults(null);
                    }
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
                Log.w(TAG, "Request failure in getDCCrimesChecksum");
            }
        });
    }

    /**
     * Get all crimes in Nova.
     */
    public void getNovaCrimes(final Context context, final RawDataCommunicatorInterface resultsCommunicator) {
        final JsonHttpResponseHandler novaCrimesResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                Log.i(TAG, "onSuccess with JSONObject");
                class InsertNovaCrimeData extends AsyncTask<JSONObject, Integer, Long> {
                    @Override
                    protected Long doInBackground(JSONObject... params) {
                        try {
                            JSONArray novaCrimesRows = response.getJSONArray("nova_crimes");
                            mCache.insertNovaCrimeData(context, novaCrimesRows);
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    resultsCommunicator.useResults(null);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "error in async task in getNovaCrime cache insertion.");
                        }
                        return 1L;
                    }
                }
                new InsertNovaCrimeData().execute();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.i(TAG, "onSuccess with JSONArray");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCrimes");
            }
        };

        WebServiceClient.get("nova-crimes-checksum", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int serverNovaCrimeChecksum = response.getInt("count");
                    int clientNovaCrimeChecksum = mCache.novaCrimeRowsChecksum(context);

                    if (serverNovaCrimeChecksum != clientNovaCrimeChecksum) {
                        Log.i(TAG, "Checksum mismatch! Retrieving nova crime data from server!");
                        mCache.wipeNovaCrimeRows(context);
                        WebServiceClient.get("nova-crime", null, novaCrimesResponseHandler);
                    } else {
                        Log.i(TAG, "Checksum match!!!! Retrieving nova crime data from cache!");
                        resultsCommunicator.useResults(null);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines--checksum"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines--checksum"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + response);
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
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + response);
            }
        };

        // Perform the checksum request, if the checksum doesn't match, pull new data
        WebServiceClient.get("nova-counties-checksum", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int serverNovaCountyChecksum = response.getInt("count");
                    int clientNovaCountyChecksum = mCache.novaCountyRowsChecksum(context);

                    if (serverNovaCountyChecksum != clientNovaCountyChecksum) {
                        Log.i(TAG, "Checksum mismatch! Retrieving nova county data from server!");
                        mCache.wipeCountyRows(context);
                        WebServiceClient.get("nova-counties", requestParameters, novaCountiesResponse);
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
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getNovaCountyOutlines--checksum"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
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
                Map<String, List<LatLng>> clusterConvexHulls = new HashMap<>();
                Map<String, Map<String, String>> clusterStatistics = new HashMap<>();
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
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject response) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
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
                Map<String, List<LatLng>> clusterConvexHulls = new HashMap<>();
                Map<String, Map<String, String>> clusterStatistics = new HashMap<>();
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
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
                        + "\nMessage: " + message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                    JSONObject response) {
                Log.w(TAG, "Request failure in getSpectralClustering"
                        + "\nStatusCode: " + statusCode
                        + "\nHeaders: " + Arrays.toString(headers)
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