package com.edg.crimeonthemove;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrimeMapFragment extends Fragment implements OnMapReadyCallback {

    public interface OnFragmentInteractionListener {
        void getDCCrimeData();
        void getNovaCrimeData();
        void getCountyOverlays(Map<String, String> params);
        void getKMeansClusteringData(Map<String, String> params);
        void getSpectralClusteringData(Map<String, String> params);
        void getCountyCrime(String[] counties);
    }

    private static final String TAG = "CrimeMapFragment";

    // Bundle Keys
    private static final String AREA_OVERLAYS_BUNDLE_KEY = "AreaOverlaysBundleKey";
    private static final String SELECTED_OVERLAY_INDEX_BUNDLE_KEY = "SelectedOverlayIndexBundleKey";

    // Static fields
    private static final float DEFAULT_ZOOM_LEVEL = 8;
    private static final int[] colors = {
            0x8FFFB300, // Vivid Yellow
            0x8F803E75, // Strong Purple
            0x8FFF6800, // Vivid Orange
            0x8FA6BDD7, // Very Light Blue -- TODO: This one has gotta go
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
            0x8F232C16  // Dark Olive Green
    };
    private static final int highlightedColorIncrement = 0x40000000;

    public static CrimeMapFragment newInstance() {
        return new CrimeMapFragment();
    }

    public static String getFragmentTitle() {
        return "Crime Map";
    }

    /**
     * Class which wraps an overlay containing:
     *  a Polygon on the GoogleMap
     *  the name of the overlay
     *  the color of the polygon overlaid
     *  the LatLng points making up the Polygon
     *  A List of Lists of LatLng points which define a holes
     *      in a polygon (if empty there are no holes)
     *  A Map from strings to strings which contains statistics about the area.
     *  Implements parcelable.
     */
    public static class OverlayWrapper implements Parcelable {
        public final String name;
        public int color;
        public Polygon polygon;
        public final List<LatLng> polygonPoints;
        public List<List<LatLng>> holes;
        public final Map<String, String> statistics;

        private boolean isHighlighted;

        public OverlayWrapper(String name, int color, Polygon polygon, Map<String, String> statistics) {
            this.name = name;
            this.color = color;
            isHighlighted = false;
            this.polygon = polygon;
            this.polygonPoints = Collections.unmodifiableList(polygon.getPoints());
            this.statistics = statistics;
            holes = new ArrayList<>(1);
        }

        public List<LatLng> getPoints() {
            return polygonPoints;
        }

        public void remove() {
            polygon.remove();
        }

        public void highlightOverlay() {
            if (!isHighlighted) {
                polygon.setFillColor(polygon.getFillColor() + highlightedColorIncrement);
                polygon.setStrokeColor(polygon.getStrokeColor() + highlightedColorIncrement);
                color = polygon.getFillColor();
                isHighlighted = true;
            }
        }

        public void unhighlightOverlay() {
            if (isHighlighted) {
                polygon.setFillColor(polygon.getFillColor() - highlightedColorIncrement);
                polygon.setStrokeColor(polygon.getStrokeColor() - highlightedColorIncrement);
                color = polygon.getFillColor();
                isHighlighted = false;
            }
        }

        public void restorePolygons(GoogleMap googleMap) {
            polygon = googleMap.addPolygon(new PolygonOptions()
                    .addAll(polygonPoints)
                    .fillColor(color)
                    .strokeColor(color)
                    .zIndex(20));
        }

        // Parcelable interface implementation
        public final Creator<OverlayWrapper> CREATOR = new Creator<OverlayWrapper>() {
            @Override
            public OverlayWrapper createFromParcel(Parcel in) {
                return new OverlayWrapper(in);
            }

            @Override
            public OverlayWrapper[] newArray(int size) {
                return new OverlayWrapper[size];
            }
        };

        /**
         * Describe the kinds of special objects contained in this Parcelable's
         * marshalled representation.
         *
         * @return a bitmask indicating the set of special object types marshalled
         * by the Parcelable.
         */
        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Flatten this object in to a Parcel.
         *
         * @param dest  The Parcel in which the object should be written.
         * @param flags Additional flags about how the object should be written.
         *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
         */
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeList(polygonPoints);
            dest.writeMap(statistics);
            dest.writeList(holes);
            dest.writeInt(color);
            dest.writeInt(isHighlighted ? 1 : 0);
        }

        /**
         * Parcel constructor.
         *
         * @param in Parcel.
         */
        public OverlayWrapper(Parcel in) {
            polygonPoints = new ArrayList<>();
            holes = new ArrayList<>(1);
            statistics = new HashMap<>(5);

            this.name = in.readString();
            // No class loader needed since LatLng implements Parcelable
            in.readList(polygonPoints, null);
            in.readMap(statistics, null);
            in.readList(holes, null);
            color = in.readInt();
            isHighlighted = in.readInt() == 1;
        }
    }

    //~Begin non-static class fields/members--------------------------------------------------------
    private OnFragmentInteractionListener mListener;
    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private ArrayList<OverlayWrapper> mAreaOverlays;
    private int mSelectedOverlayIndex;
    private ArrayList<Marker> mMarkers;
    private SharedPreferences mOptions;
    private Cache mCache;

    private final HandlerThread mHandlerThread = new HandlerThread("UI Offloader");
    private Handler mHandler;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Restore state
        if (savedInstanceState != null) {
            Log.i(TAG, "onCreate: Restoring from savedInstanceState!");
            mAreaOverlays = savedInstanceState.getParcelableArrayList(AREA_OVERLAYS_BUNDLE_KEY);
            mSelectedOverlayIndex = savedInstanceState.getInt(SELECTED_OVERLAY_INDEX_BUNDLE_KEY);
            // Remove from bundle because it's breaking other restore instance states
            // with an unknown class exception...that's kind of stupid though...
            savedInstanceState.remove(AREA_OVERLAYS_BUNDLE_KEY);
        } else {
            mAreaOverlays = new ArrayList<>(7);
            mSelectedOverlayIndex = -1;
        }
        mMarkers = new ArrayList<>(3000);
        mCache = new Cache();

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_crime_map, container, false);

        // MapView stuff
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

                Map<String, String> params = new HashMap<>();

                // Data to cluster on
                // Nova
                if (mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)) {
                    List<String> counties = new ArrayList<>(Constants.COUNTY_CRIME_OPTIONS.length);
                    for (int i = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        String countyOption = Constants.COUNTY_CRIME_OPTIONS[i];
                        if (mOptions.getBoolean(countyOption, false)) {
                            counties.add(Constants.COUNTY_NAMES.get(countyOption));
                        }
                    }
                    params.put(Constants.BACK_END_NOVA_DATA_PARAM, new JSONArray(counties).toString());
                }
                // DC
                if (mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)) {
                    params.put(Constants.BACK_END_DC_DATA_PARAM, Constants.BACK_END_DC_DATA_PARAM);
                }

                // Number of clusters
                int numClusters = mOptions.getInt(Constants.NUM_CLUSTERS_OPTION, -1);

                // Type of clustering
                if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.K_MEANS_SELECTED) {
                    params.put(Constants.BACK_END_NUM_CLUSTERS_PARAM, String.valueOf(numClusters));
                    mListener.getKMeansClusteringData(params);
                    Toast.makeText(getActivity(), "Running K-Means Clustering...", Toast.LENGTH_SHORT).show();
                } else if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.SPECTRAL_CLUSTERING_SELECTED) {
                    params.put(Constants.BACK_END_NUM_CLUSTERS_PARAM, String.valueOf(numClusters));
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
        // TODO: Merge this into regular get data
        Button getCrimeForCounty = (Button) view.findViewById(R.id.button_get_crime_for_county);
        getCrimeForCounty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Reconsider what I'm doing here (with using an array that is...) after testing
                int totalTrue = 0;
                String[] countySelections = new String[Constants.COUNTY_CRIME_OPTIONS.length];
                for (int i = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                    String countyOption = Constants.COUNTY_CRIME_OPTIONS[i];
                    if (mOptions.getBoolean(countyOption, false)) {
                        countySelections[totalTrue] = Constants.COUNTY_NAMES.get(countyOption);
                        totalTrue += 1;
                    }
                }
                countySelections = Arrays.copyOf(countySelections, totalTrue);
                mListener.getCountyCrime(countySelections);
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
        Log.d(TAG, "mAreaOverlays:size(): " + mAreaOverlays.size());
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).restorePolygons(mGoogleMap);
        }
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng clickPoint) {
                int selectedOverlay = getSelectedOverlay(clickPoint);
                if (selectedOverlay != -1) {
                    // Do stuff with info box
                    Log.i(TAG, "SingleClick: Selecting overlay with name: "
                            + mAreaOverlays.get(selectedOverlay).name);
                }
            }
        });
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng clickPoint) {
                int selectedOverlay = getSelectedOverlay(clickPoint);
                if (selectedOverlay != -1) {
                    // Mark this overlay as selected
                    Log.i(TAG, "LongClick: Selecting overlay with name: "
                            + mAreaOverlays.get(selectedOverlay).name);
                    // unhighlight previous selection (including if we are "re-selecting")
                    if (mSelectedOverlayIndex != -1) {
                        mAreaOverlays.get(mSelectedOverlayIndex).unhighlightOverlay();
                    }
                    // Only highlight if new selection was not previous selection
                    if (selectedOverlay != mSelectedOverlayIndex) {
                        mAreaOverlays.get(selectedOverlay).highlightOverlay();
                    }
                    mSelectedOverlayIndex = selectedOverlay;
                }
            }
        });
    }

    /**
     * Uses the fact that a triangle's signed area will be positive if lineStart, lineEnd, point
     * are in counter-clockwise order to discover the side of the line that the point is on.
     * Return > 0 for left
     * Return = 0 for on
     * Return < 0 for right
     *
     * @param point LatLng representing point
     * @param lineStart LatLng representing the starting point of a line.
     * @param lineEnd LatLng representing the ending point of a line.
     * @return Return > 0 for left
     *         Return = 0 for on
     *         Return < 0 for right
     */
    private double isLeft(LatLng point, LatLng lineStart, LatLng lineEnd) {
        return (lineEnd.longitude - lineStart.longitude) * (point.latitude - lineStart.latitude)
                - (point.longitude - lineStart.longitude) * (lineEnd.latitude - lineStart.latitude);
    }

    /**
     * Checks if the LatLng point is within the polygon defined by a List<LatLng>.
     * Uses the winding number test to check.
     * Returns true if there is a hit (point is within polygon).
     *
     * @param polygon A List of LatLng objects representing points making up a polygon.
     * @param point A LatLng object representing a point.
     * @return true if point is inside polygon, false otherwise.
     */
    private boolean windingNumberHitCheck(List<LatLng> polygon, LatLng point) {
        // longitude x
        // latitude y
        int windingNumber = 0;
        for (int i = 0; i < (polygon.size() - 1); i++) {
            if (polygon.get(i).latitude <= point.latitude) { // Rule #1
                if (polygon.get(i + 1).latitude > point.latitude) { // Upward crossing
                    if (isLeft(point, polygon.get(i), polygon.get(i + 1)) > 0) { // Rule #4 (left)
                        windingNumber++;
                    }
                }
            } else {
                if (polygon.get(i + 1).latitude <= point.latitude) { // Rule #2 (downward crossing)
                    if (isLeft(point, polygon.get(i), polygon.get(i + 1)) < 0) { // Rule #4 (right)
                        windingNumber--;
                    }
                }
            }
        }
        return windingNumber != 0;
    }

    /**
     * Takes a List of "holes" in some polygon and a point and returns true if the point is not
     * contained in any of the "holes".
     * Essentially I'm checking if the point is contained in any of the N passed polygons
     * and returning true if point is NOT contained in any of the polygons.
     *
     * @param holes List<List<LatLng>> a List of lists of LatLng objects defining polygons (holes).
     * @param point a LatLng point.
     * @return true if point is not contained in any of the polygons in holes.
     */
    private boolean noHoleHits(List<List<LatLng>> holes, LatLng point) {
        for (int i = 0; i < holes.size(); i++) {
            // If there's a hit, return false
            if (windingNumberHitCheck(holes.get(i), point)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes a LatLng point (clickPoint) and determines which overlay in mAreaOverlays point is
     * contained in (which one was clicked). Returns the index of the corresponding overlay.
     * If not overlay was selected, returns -1.
     * @param clickPoint the LatLng point on the map which was clicked.
     * @return the index of the overlay clicked, or -1 if no overlay was clicked.
     */
    private int getSelectedOverlay(LatLng clickPoint) {
        int selectedOverlay = -1;
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            if (windingNumberHitCheck(mAreaOverlays.get(i).getPoints(), clickPoint)
                    && (mAreaOverlays.get(i).holes.size() == 0
                        || noHoleHits(mAreaOverlays.get(i).holes, clickPoint))) {
                selectedOverlay = i;
                break;
            }
        }
        return selectedOverlay;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        mMapView.onResume();
        mOptions = getActivity().getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
        outState.putParcelableArrayList(AREA_OVERLAYS_BUNDLE_KEY, mAreaOverlays);
        outState.putInt(SELECTED_OVERLAY_INDEX_BUNDLE_KEY, mSelectedOverlayIndex);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mMapView.onDestroy();
        mHandlerThread.quit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void addNovaCrimes(String[] countyConstraints) {
        Log.i(TAG, "addNovaCrimes()");
        Log.i(TAG, "countyConstraints: " + Arrays.toString(countyConstraints));
        mMarkers.clear();
        Toast.makeText(getContext(), "Placing crime markers, this may take awhile.....", Toast.LENGTH_LONG).show();
        final Cache.NovaCrimeQuery novaCrimeQuery = new Cache.NovaCrimeQuery(getContext(), countyConstraints) {
            private int i = 0;
            @Override
            public void useData(final double latitude, final double longitude, final String address) {
                final LatLng latLng = new LatLng(latitude, longitude);
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .title(address)
                                .position(latLng));
                        mMarkers.add(marker);
                        if (i == 0) {
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            if (mGoogleMap.getCameraPosition().zoom < DEFAULT_ZOOM_LEVEL) {
                                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
                            }
                        }
                    }
                });
                i++;
            }
        };
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCache.runQuery(novaCrimeQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getContext(), "Done placing crime markers!!!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "FINISHED PLACING NOVA CRIME MARKERS!");
            }
        });
    }

    public void addDcCrimes() {
        Log.v(TAG, "addDcCrimes()");
        mMarkers.clear();
        Toast.makeText(getContext(), "Placing DC crime markers, this may take awhile.....", Toast.LENGTH_LONG).show();
        final Cache.DcCrimeQuery dcCrimeQuery = new Cache.DcCrimeQuery(getContext()) {
            private int i = 0;
            @Override
            public void useData(final double latitude, final double longitude, final String address) {
                final LatLng latLng = new LatLng(latitude, longitude);
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .title(address)
                                .position(latLng));
                        mMarkers.add(marker);
                        if (i == 0) {
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            if (mGoogleMap.getCameraPosition().zoom < DEFAULT_ZOOM_LEVEL) {
                                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
                            }
                        }
                    }
                });
                i++;
            }
        };
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCache.runQuery(dcCrimeQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "FINISHED PLACING DC CRIME MARKERS!");
                Toast.makeText(getContext(), "Done placing crime markers!!!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addAreaOverlay(Map<String, List<LatLng>> areaBoundaries,
            Map<String, Map<String, String>> areaStatistics) {

        // TODO: I need to figure out how I'm REALLY going to be managing these overlays....
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).remove();
        }
        mAreaOverlays.clear();
        // TODO: Remove....
        mGoogleMap.clear();

        //Polygon polygon;
        Marker marker;
        Map<String, String> specificAreaStatistics;
        String statisticsString;
        Polygon polygon;
        for (String key : areaBoundaries.keySet()) {
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
            if (areaStatistics.get(key) != null) {
                mAreaOverlays.add(new OverlayWrapper("Cluster " + key, 0, polygon, areaStatistics.get(key)));
            } else {
                mAreaOverlays.add(new OverlayWrapper("Cluster " + key, 0, polygon, null));
            }
        }
    }

    public void addCountyOverlay() {
        // TODO: I need to figure out how I'm REALLY going to be managing these overlays....
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).remove();
        }
        mAreaOverlays.clear();
        // TODO: Remove....
        mGoogleMap.clear();

        Toast.makeText(getContext(), "Drawing areas, this may take awhile.....", Toast.LENGTH_LONG).show();
        final Cache.NovaCountyQuery novaCountyQuery = new Cache.NovaCountyQuery(getContext()) {
            @Override
            public void useData(final String countyName, final List<LatLng> countyOutline,
                    final Map<String, String> countyStatistics) {
                // Take hash code and mod by color length, this should produce random enough color
                // dispersion to be visually appealing.
                final int colorKey = ((Math.abs(countyName.hashCode()) % colors.length) * 57 + 13) % colors.length;
                // Run UI rendering on the main thread
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Polygon polygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(countyOutline)
                                .fillColor(colors[colorKey])
                                .strokeColor(colors[colorKey])
                                .zIndex(20));
                        mAreaOverlays.add(new OverlayWrapper(countyName, colors[colorKey],
                                polygon, countyStatistics));
                    }
                });
            }
        };

        final Runnable overlayHoleFindingTask = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Starting overlay hole finding task....");
                // Add holes to county overlays if there are overlaps (which there should be....)
                List<LatLng> polygon;
                LatLng point1;
                LatLng point2;
                LatLng point3;
                OverlayWrapper curOverlay;
                for (int i = 0; i < mAreaOverlays.size(); i++) {
                    polygon = mAreaOverlays.get(i).getPoints();
                    for (int j = 0; j < mAreaOverlays.size(); j++) {
                        if (j == i) {
                            continue;
                        }
                        // Check if the first point, middle point, and last point are in polygon
                        // If so, then the jth overlay is considered to be contained
                        // within the ith overlay, and thus a hole should be included in the polygon
                        // This is an approximation, to properly check I should iterate over each point
                        // or apply a different algorithm.
                        // However, for my uses, this will almost certainly suffice and will also
                        // provide a higher degree of performance.
                        point1 = mAreaOverlays.get(j).getPoints().get(0);
                        point2 = mAreaOverlays.get(j).getPoints().get(mAreaOverlays.get(j).getPoints().size() / 2);
                        point3 = mAreaOverlays.get(j).getPoints().get(mAreaOverlays.get(j).getPoints().size() - 1);
                        if (windingNumberHitCheck(polygon, point1)
                                && windingNumberHitCheck(polygon, point2)
                                && windingNumberHitCheck(polygon, point3)) {
                            Log.d(TAG, "Adding hole in county: " + mAreaOverlays.get(i).name
                                    + " using county: " + mAreaOverlays.get(j).name);
                            curOverlay = mAreaOverlays.get(i);
                            curOverlay.holes.add(mAreaOverlays.get(j).getPoints());
                        }
                    }
                }
                // Remove holes on main thread
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OverlayWrapper curOverlay;
                        for (int i = 0; i < mAreaOverlays.size(); i++) {
                            curOverlay = mAreaOverlays.get(i);
                            if (!curOverlay.holes.isEmpty()) {
                                curOverlay.polygon.setHoles(curOverlay.holes);
                            }
                        }
                    }
                });
                Toast.makeText(getContext(), "Done drawing areas!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "FINISHED overlay hole finding task!");
            }
        };

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCache.runQuery(novaCountyQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "JSONException IN addAreaOverlay IN CrimeMapFragment");
                }
                mHandler.post(overlayHoleFindingTask);
            }
        });
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