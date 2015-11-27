package com.edg.crimeonthemove;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrimeMapFragment extends Fragment implements OnMapReadyCallback {

    public interface OnFragmentInteractionListener {
        public void getDCCrimeData();
        public void getNovaCrimeData();
        public void getCountyOverlays(Map<String, String> params);
        public void getKMeansClusteringData(Map<String, String> params);
        public void getSpectralClusteringData(Map<String, String> params);
    }

    private static final String TAG = "CrimeMapFragment";

    private static final float DEFAULT_ZOOM_LEVEL = 8;

    public static CrimeMapFragment newInstance() {
        return new CrimeMapFragment();
    }

    public static String getFragmentTitle() {
        return "Crime Map";
    }

    private OnFragmentInteractionListener mListener;
    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private List<Polygon> mAreaOverlays;
    private List<Marker> mMarkers;
    private SharedPreferences mOptions;
    private Cache mCache;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAreaOverlays = new ArrayList<>(5);
        mMarkers = new ArrayList<>(3000);
        mCache = new Cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
        Button getDataButton = (Button) view.findViewById(R.id.button_get_crime_data);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)) {
                    mListener.getNovaCrimeData();
                    Toast.makeText(getActivity(), "Pulling NOVA data...", Toast.LENGTH_SHORT).show();
                }
                if (mOptions.getBoolean(Constants.DC_DATA_OPTION, false)) {
                    mListener.getDCCrimeData();
                    Toast.makeText(getActivity(), "Pulling DC data...", Toast.LENGTH_SHORT).show();
                }
                if (!mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)
                        && !mOptions.getBoolean(Constants.DC_DATA_OPTION, false)) {
                    Toast.makeText(getActivity(), "You must select a data set...", Toast.LENGTH_LONG).show();
                }
            }
        });
        Button getClusteringButton = (Button) view.findViewById(R.id.button_get_clustering);
        getClusteringButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Clustering onClick, clustering selection: " + mOptions.getInt(Constants.CLUSTERING_SELECTION, -1));
                Map<String, String> params = new HashMap<String, String>();
                if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.K_MEANS_SELECTED) {
                    mListener.getKMeansClusteringData(params);
                    Toast.makeText(getActivity(), "Running K-Means Clustering...", Toast.LENGTH_SHORT).show();
                } else if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.SPECTRAL_CLUSTERING_SELECTED) {
                    mListener.getSpectralClusteringData(params);
                    Toast.makeText(getActivity(), "Running Spectral Clustering...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "No clustering specified?", Toast.LENGTH_LONG).show();
                }
            }
        });
        Button getCountyOverlaysButton = (Button) view.findViewById(R.id.button_get_overlays);
        getCountyOverlaysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.getCountyOverlays(null);
            }
        });
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady");
        mGoogleMap = googleMap;
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(38.8824271026509, -77.0300120214052)));
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        mOptions = getActivity().getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void addCrimes(List<Map<String, String>> crimes) {
        mMarkers.clear();
        Map<String, String> crime;
        double latitude;
        double longitude;
        LatLng latLng;
        for (int i = 0; i < crimes.size(); i++) {
            crime = crimes.get(i);
            if (crime.get("x_cord").equals("null") || crime.get("y_cord").equals("null")) {
                Log.i(TAG, "NO GPS FOR CRIME: " + crime);
                continue;
            }
            longitude = Double.valueOf(crime.get("x_cord"));
            latitude = Double.valueOf(crime.get("y_cord"));
            latLng = new LatLng(latitude, longitude);
            Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                    .title(crime.get("address"))
                    .position(latLng));
            mMarkers.add(marker);
            if (i == 0) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                if (mGoogleMap.getCameraPosition().zoom < DEFAULT_ZOOM_LEVEL) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
                }
            }
        }
    }

    public void addAreaOverlay(Map<String, List<LatLng>> areaBoundaries,
            Map<String, Map<String, String>> areaStatistics) {
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).remove();
        }
        mAreaOverlays.clear();
        // TODO: Remove....
        mGoogleMap.clear();
        final int[] colors = {
                0x8FFFB300, // Vivid Yellow
                0x8F803E75, // Strong Purple
                0x8FFF6800, // Vivid Orange
                0x8FA6BDD7, // Very Light Blue
                0x8FC10020, // Vivid Red
                0x8FCEA262, // Grayish Yellow
                0x8F817066, // Medium Gray
                0x8F007D34, // Vivid Green
                0x8FF6768E, // Strong Purplish Pink
                0x8F00538A, // Strong Blue
                0x8FFF7A5C, // Strong Yellowish Pink
                0x8F53377A, // Strong Violet
                0x8FFF8E00, // Vivid Orange Yellow
                0x8FB32851, // Strong Purplish Red
                0x8FF4C800, // Vivid Greenish Yellow
                0x8F7F180D, // Strong Reddish Brown
                0x8F93AA00, // Vivid Yellowish Green
                0x8F593315, // Deep Yellowish Brown
                0x8FF13A13, // Vivid Reddish Orange
                0x8F232C16 // Dark Olive Green
        };
        //Polygon polygon;
        Marker marker;
        Map<String, String> specificAreaStatistics;
        String statisticsString;
        try {
            mCache.runQuery(new Cache.CountyQuery(getContext()) {
                @Override
                public void useData(String countyName, List<LatLng> countyOutline) {
                    // Take hash code and mod by color length, this should produce random enough color
                    // dispersion to be visually appealing.
                    int colorKey = countyName.hashCode() % colors.length;
                    Polygon polygon = mGoogleMap.addPolygon(new PolygonOptions()
                            .addAll(countyOutline)
                            .fillColor(colors[colorKey])
                            .strokeColor(colors[colorKey])
                            .zIndex(20));
                    mAreaOverlays.add(polygon);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSONException IN addAreaOverlay IN CrimeMapFragment");
        }
        //for (String key : areaBoundaries.keySet()) {
            /*
            // Handle coloring for integer strings and regular strings
            if (isInteger(key)
                    && Integer.parseInt(key) < colors.length && Integer.parseInt(key) > -1) {
                polygon = mGoogleMap.addPolygon(new PolygonOptions()
                        .addAll(areaBoundaries.get(key))
                        .fillColor(colors[Integer.parseInt(key)])
                        .strokeColor(colors[Integer.parseInt(key)])
                        .zIndex(20));
            } else {
                // Take hash code and mod by color length, this should produce random enough color
                // dispersion to be visually appealing.
                int colorKey = key.hashCode() % colors.length;
                polygon = mGoogleMap.addPolygon(new PolygonOptions()
                        .addAll(areaBoundaries.get(key))
                        .fillColor(colors[colorKey])
                        .strokeColor(colors[colorKey])
                        .zIndex(20));
            }
            mAreaOverlays.add(polygon);
            */

            /*
            // If there are statistics, display them
            Log.i(TAG, "areaStatistics: " + areaStatistics);
            if (areaStatistics.size() > 0) {
                specificAreaStatistics = areaStatistics.get(key);
                statisticsString = "";
                for (String statisticsKey : specificAreaStatistics.keySet()) {
                    statisticsString += specificAreaStatistics.get(statisticsKey);
                    statisticsString += "\n";
                }
                marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(areaBoundaries.get(key).get(0))
                        .title("Cluster " + key)
                        .snippet(statisticsString));
                marker.showInfoWindow();
            }
            */
        //}
    }

    private static boolean isInteger(String inputData) {
        if (inputData == null) {
            return false;
        }
        inputData = inputData.trim();
        if (inputData.isEmpty()) {
            return false;
        }
        for (int i = 0; i < inputData.length(); i++) {
            if (inputData.charAt(i) >= '0' && inputData.charAt(i) <= '9') {
                continue;
            } else if (i == 0 && inputData.charAt(i) == '-') {
                continue;
            }
            return false;
        }
        return true;
    }
}