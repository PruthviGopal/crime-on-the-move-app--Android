package com.edg.crimeonthemove;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CrimeMapFragment extends Fragment implements OnMapReadyCallback {

    public interface OnFragmentInteractionListener {
        void getDCCrimeData();
        void getNovaCrimeData(String[] counties);
        void getDCAndNovaCrime(String[] novaCounties);
        void getAreaCrimeData(List<List<LatLng>> area);
        void getCountyOverlays(Map<String, String> params);
        void getKMeansClusteringData(Map<String, String> params);
        void getSpectralClusteringData(Map<String, String> params);
        void getAffinityPropagationData(Map<String, String> params);
    }

    private static final String TAG = "CrimeMapFragment";

    // Bundle Keys
    private static final String AREA_OVERLAYS_BUNDLE_KEY = "AreaOverlaysBundleKey";
    private static final String CRIME_MARKERS_BUNDLE_KEY = "CrimeMarkersBundleKey";
    private static final String SELECTED_OVERLAY_INDEX_BUNDLE_KEY = "SelectedOverlayIndexBundleKey";

    // Static fields
    private static final String MARKER_SNIPPET_HEADER = "MarkerSnippetHeader: ";
    private static final String POLYGON_SNIPPET_HEADER = "PolygonSnippetHeader: ";

    private static final float DEFAULT_ZOOM_LEVEL = 9;//8;
    private static final int[] colors = {
            0x8FFFB300, // Vivid Yellow
            0x8F803E75, // Strong Purple
            0x8FFF6800, // Vivid Orange
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

    public static class MarkerClusterItem implements ClusterItem {

        private final LatLng mPosition;

        public MarkerClusterItem(LatLng position) {
            mPosition = position;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }

    public static class MarkerWrapper implements Parcelable {

        public Marker marker;

        public MarkerWrapper(Marker newMarker) {
            marker = newMarker;
        }

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
            dest.writeDouble(marker.getPosition().latitude);
            dest.writeDouble(marker.getPosition().longitude);
        }

        // Parcelable interface implementation
        public final Creator<MarkerWrapper> CREATOR = new Creator<MarkerWrapper>() {
            @Override
            public MarkerWrapper createFromParcel(Parcel in) {
                return new MarkerWrapper(in);
            }

            @Override
            public MarkerWrapper[] newArray(int size) {
                return new MarkerWrapper[size];
            }
        };

        public MarkerWrapper(Parcel in) {
            double latitude = in.readDouble();
            double longitude = in.readDouble();
        }
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
        public Marker marker;
        public final List<LatLng> polygonPoints;
        public List<List<LatLng>> holes;
        public final Map<String, String> statistics;

        private boolean isHighlighted;
        private MarkerOptions markerOptions;

        public OverlayWrapper(String name, int color, Polygon polygon, Marker marker, Map<String, String> statistics) {
            this.name = name;
            this.color = color;
            isHighlighted = false;
            this.polygon = polygon;
            this.marker = marker;
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
            if (holes.size() > 0) {
                polygon = googleMap.addPolygon(new PolygonOptions()
                        .addAll(polygonPoints)
                        .fillColor(color)
                        .addHole(holes.get(0))
                        .strokeColor(color)
                        .zIndex(20));
            } else {
                polygon = googleMap.addPolygon(new PolygonOptions()
                        .addAll(polygonPoints)
                        .fillColor(color)
                        .strokeColor(color)
                        .zIndex(20));
            }
            if (markerOptions != null) {
                Log.i(TAG, "RESTORED INVISIBLE MARKER TOO!");
                marker = googleMap.addMarker(markerOptions);
            } else if (marker != null) {
                marker = googleMap.addMarker(new MarkerOptions()
                        .title(marker.getTitle())
                        .snippet(marker.getSnippet())
                        .alpha(0)
                        .icon(getCircleMarkerIcon())
                        .position(marker.getPosition()));

            }
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
            // Marker
            dest.writeString(marker.getTitle());
            dest.writeString(marker.getSnippet());
            dest.writeDouble(marker.getPosition().latitude);
            dest.writeDouble(marker.getPosition().longitude);
        }

        /**
         * Parcel constructor.
         *
         * @param in Parcel.
         */
        public OverlayWrapper(Parcel in) {
            Log.i(TAG, "OVERLAY WRAPPER PARCELABLE CONSTRUCTOR!");
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
            String title = in.readString();
            String snippet = in.readString();
            double latitude = in.readDouble();
            double longitude = in.readDouble();

            markerOptions = new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .icon(getCircleMarkerIcon())
                    .alpha(0)
                    .title(title)
                    .snippet(snippet);
        }
    }

    //~Begin non-static class fields/members--------------------------------------------------------
    private OnFragmentInteractionListener mListener;
    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private ClusterManager<MarkerClusterItem> mClusterManager;
    private ArrayList<MarkerWrapper> mMarkers;
    private ArrayList<OverlayWrapper> mAreaOverlays;
    private int mSelectedOverlayIndex;
    private SharedPreferences mOptions;
    private Cache mCache;

    private final HandlerThread mHandlerThread = new HandlerThread("UI Offloader");
    private Handler mHandler;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Task used to find holes that should be made in overlays
     */
    private final Runnable mOverlayHoleFindingTask = new Runnable() {
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
                            && windingNumberHitCheck(polygon, point3)
                            && !mAreaOverlays.get(i).holes.contains(mAreaOverlays.get(j).getPoints())) {
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
            Log.i(TAG, "FINISHED overlay hole finding task!");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Restore state
        if (savedInstanceState != null) {
            Log.i(TAG, "onCreate: Restoring from savedInstanceState!");
            mAreaOverlays = savedInstanceState.getParcelableArrayList(AREA_OVERLAYS_BUNDLE_KEY);
            mSelectedOverlayIndex = savedInstanceState.getInt(SELECTED_OVERLAY_INDEX_BUNDLE_KEY);
            mMarkers = savedInstanceState.getParcelableArrayList(CRIME_MARKERS_BUNDLE_KEY);
            // Remove from bundle because it's breaking other restore instance states
            // with an unknown class exception...that's kind of stupid though...
            savedInstanceState.remove(AREA_OVERLAYS_BUNDLE_KEY);
            savedInstanceState.remove(CRIME_MARKERS_BUNDLE_KEY);
        } else {
            mAreaOverlays = new ArrayList<>(7);
            mSelectedOverlayIndex = -1;
            mMarkers = new ArrayList<>(50);
        }
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

        Button getSelectedOverlayDataButton = (Button) view.findViewById(R.id.button_get_selected_overlay_data);
        getSelectedOverlayDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedOverlayIndex != -1) {
                    Log.i(TAG, "Pulling crime in selected area....");
                    List<LatLng> area = mAreaOverlays.get(mSelectedOverlayIndex).getPoints();
                    List<List<LatLng>> areas = new ArrayList<>();
                    areas.add(area);
                    mListener.getAreaCrimeData(areas);
                } else {
                    Toast.makeText(getContext(),
                            "No overlay is selected!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*
        Button getDataButton = (Button) view.findViewById(R.id.button_get_crime_data);
        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // NOVA and DC Data
                if (mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)
                        && mOptions.getBoolean(Constants.DC_DATA_OPTION, false)) {
                    int activeCountiesCount = 0;
                    for (int i = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        if (mOptions.getBoolean(Constants.COUNTY_CRIME_OPTIONS[i], false)) {
                            activeCountiesCount++;
                        }
                    }
                    String[] selectedCounties = new String[activeCountiesCount];
                    for (int i = 0, j = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        if (mOptions.getBoolean(Constants.COUNTY_CRIME_OPTIONS[i], false)) {
                            selectedCounties[j] = Constants.COUNTY_NAMES.get(Constants.COUNTY_CRIME_OPTIONS[i]);
                            j++;
                        }
                    }
                    mListener.getDCAndNovaCrime(selectedCounties);
                    Toast.makeText(getActivity(), "Pulling NOVA and DC data...",
                            Toast.LENGTH_SHORT).show();
                    // NOVA Data
                } else if (mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false)) {
                    int activeCountiesCount = 0;
                    for (int i = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        if (mOptions.getBoolean(Constants.COUNTY_CRIME_OPTIONS[i], false)) {
                            activeCountiesCount++;
                        }
                    }
                    String[] selectedCounties = new String[activeCountiesCount];
                    for (int i = 0, j = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        if (mOptions.getBoolean(Constants.COUNTY_CRIME_OPTIONS[i], false)) {
                            selectedCounties[j] = Constants.COUNTY_NAMES.get(Constants.COUNTY_CRIME_OPTIONS[i]);
                            j++;
                        }
                    }
                    mListener.getNovaCrimeData(selectedCounties);
                    Toast.makeText(getActivity(), "Pulling NOVA data...", Toast.LENGTH_SHORT).show();
                    // DC Data
                } else if (mOptions.getBoolean(Constants.DC_DATA_OPTION, false)) {
                    mListener.getDCCrimeData();
                    Toast.makeText(getActivity(), "Pulling DC data...", Toast.LENGTH_SHORT).show();
                    // No Data....
                } else {
                    Toast.makeText(getActivity(),
                            "You must select a data set...", Toast.LENGTH_LONG).show();
                }
            }
        });
        */
        Button clearMarkersButton = (Button) view.findViewById(R.id.button_clear_markers);
        clearMarkersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearMarkers();
            }
        });

        Button getClusteringButton = (Button) view.findViewById(R.id.button_get_clustering);
        getClusteringButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Clustering onClick, clustering selection: "
                        + mOptions.getInt(Constants.CLUSTERING_SELECTION, -1));

                Map<String, String> params = new HashMap<>();

                // If nothing is selected, fall back to check boxes
                if (mSelectedOverlayIndex == -1) {
                    // Data to cluster on
                    // Nova counties from settings checkboxes
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
                    // DC from settings checkbox
                    if (mOptions.getBoolean(Constants.DC_DATA_OPTION, false)) {
                        params.put(Constants.BACK_END_DC_DATA_PARAM, Constants.BACK_END_DC_DATA_PARAM);
                    }
                    // Include nova and dc data, you never know what's outlined!
                    /// Add every Nova county name!
                } else {
                    List<String> counties = new ArrayList<>(Constants.COUNTY_CRIME_OPTIONS.length);
                    for (int i = 0; i < Constants.COUNTY_CRIME_OPTIONS.length; i++) {
                        String countyOption = Constants.COUNTY_CRIME_OPTIONS[i];
                        counties.add(Constants.COUNTY_NAMES.get(countyOption));
                    }
                    params.put(Constants.BACK_END_NOVA_DATA_PARAM, new JSONArray(counties).toString());
                    params.put(Constants.BACK_END_DC_DATA_PARAM, Constants.BACK_END_DC_DATA_PARAM);
                }

                // Crime Types to cluster on
                JSONObject jsonCrimesConsidered = new JSONObject();

                // NOVA Crime Types
                Set<String> novaCrimeTypesSet = new HashSet<>();
                novaCrimeTypesSet = mOptions.getStringSet(
                        Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, novaCrimeTypesSet);
                Log.i(TAG, "novaCrimeTypesSet: " + novaCrimeTypesSet);
                // If no crime types are selected, send nothing, which is null, which means everything
                if (novaCrimeTypesSet.size() > 0) {
                    String[] novaCrimeTypesArray = new String[novaCrimeTypesSet.size()];
                    novaCrimeTypesSet.toArray(novaCrimeTypesArray);
                    try {
                        JSONArray jsonNovaCrimeTypesArray = new JSONArray(novaCrimeTypesArray);
                        jsonCrimesConsidered.put(Constants.BACK_END_NOVA_CRIME_TYPES_PARAM, jsonNovaCrimeTypesArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // DC Crime Types
                Set<String> dcCrimeTypesSet = new HashSet<>();
                dcCrimeTypesSet = mOptions.getStringSet(
                        Constants.SELECTED_DC_CRIME_TYPES_OPTION, dcCrimeTypesSet);
                Log.i(TAG, "dcCrimeTypesSet: " + dcCrimeTypesSet);
                // If no crime types are selected, send nothing, which is null, which means everything
                if (dcCrimeTypesSet.size() > 0) {
                    String[] dcCrimeTypesArray = new String[dcCrimeTypesSet.size()];
                    dcCrimeTypesSet.toArray(dcCrimeTypesArray);
                    try {
                        JSONArray jsonDcCrimeTypesArray = new JSONArray(dcCrimeTypesArray);
                        jsonCrimesConsidered.put(Constants.BACK_END_DC_CRIME_TYPES_PARAM, jsonDcCrimeTypesArray.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                params.put(Constants.BACK_END_CRIME_TYPES_PARAM, jsonCrimesConsidered.toString());

                // Number of clusters
                int numClusters = mOptions.getInt(Constants.NUM_CLUSTERS_OPTION, -1);

                // Area to cluster on
                // TODO: Expand beyond K-Means (and perhaps beyond spectral?)
                if ((mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.K_MEANS_SELECTED
                            || mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.SPECTRAL_CLUSTERING_SELECTED)
                        && mSelectedOverlayIndex != -1) {
                    OverlayWrapper overlayWrapper = mAreaOverlays.get(mSelectedOverlayIndex);
                    List<LatLng> outline = overlayWrapper.getPoints();
                    // Format points and place into JSONArray
                    JSONArray jsonOutline = new JSONArray();
                    for (int i = 0; i < outline.size(); i++) {
                        try {
                            jsonOutline.put(i, "(" + outline.get(i).longitude
                                    + ", " + outline.get(i).latitude + ")");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    params.put(Constants.BACK_END_CLUSTERING_CRIME_OUTLINES_PARAM, jsonOutline.toString());
                }

                // Type of clustering
                if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.K_MEANS_SELECTED) {
                    params.put(Constants.BACK_END_NUM_CLUSTERS_PARAM, String.valueOf(numClusters));
                    mListener.getKMeansClusteringData(params);
                    if (mSelectedOverlayIndex == -1) {
                        clearOverlays();
                    } else {
                        removeOverlay(mSelectedOverlayIndex);
                    }
                    Toast.makeText(getActivity(), "Running K-Means Clustering...",
                            Toast.LENGTH_SHORT).show();
                } else if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.SPECTRAL_CLUSTERING_SELECTED) {
                    params.put(Constants.BACK_END_NUM_CLUSTERS_PARAM, String.valueOf(numClusters));
                    mListener.getSpectralClusteringData(params);
                    if (mSelectedOverlayIndex == -1) {
                        clearOverlays();
                    } else {
                        removeOverlay(mSelectedOverlayIndex);
                    }
                    Toast.makeText(getActivity(), "Running Spectral Clustering...",
                            Toast.LENGTH_SHORT).show();
                } else if (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1) == Constants.AFFINITY_PROPAGATION_SELECTED) {
                    mListener.getAffinityPropagationData(params);
                    if (mSelectedOverlayIndex == -1) {
                        clearOverlays();
                    } else {
                        removeOverlay(mSelectedOverlayIndex);
                    }
                    Toast.makeText(getActivity(), "Running Affinity Propagation...",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "No clustering specified?",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        Button getCountyOverlaysButton = (Button) view.findViewById(R.id.button_get_overlays);
        getCountyOverlaysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.getCountyOverlays(null);
                clearOverlays();
                clearMarkers();
            }
        });
        return view;
    }

    private void removeOverlay(int overlayIndex) {
        if (overlayIndex < 0 || overlayIndex >= mAreaOverlays.size()) {
            Log.w(TAG, "Warning in removeOverlay, overlayIndex out of bounds: " + overlayIndex);
            return;
        }
        mAreaOverlays.get(overlayIndex).remove();
        mAreaOverlays.remove(overlayIndex);
        if (overlayIndex == mSelectedOverlayIndex) {
            mSelectedOverlayIndex = -1;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady");
        mGoogleMap = googleMap;
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(
                new LatLng(38.8824271026509, -77.0300120214052)));
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
        Log.d(TAG, "mAreaOverlays:size(): " + mAreaOverlays.size());
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).restorePolygons(mGoogleMap);
        }
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng clickPoint) {
                int selectedOverlayIndex = getSelectedOverlay(clickPoint);
                if (selectedOverlayIndex != -1) {
                    // Do stuff with info box
                    OverlayWrapper selectedOverlay = mAreaOverlays.get(selectedOverlayIndex);
                    Log.i(TAG, "SingleClick: Selecting overlay with name: "
                            + selectedOverlay.name);
                    // Hide info window if already shown
                    if (selectedOverlay.marker.isInfoWindowShown()) {
                        Log.i(TAG, "NOT showing info window");
                        mAreaOverlays.get(selectedOverlayIndex).marker.hideInfoWindow();
                        // Show info window if not shown
                    } else {
                        Log.i(TAG, "showing info window");
                        mAreaOverlays.get(selectedOverlayIndex).marker.showInfoWindow();
                    }
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
                        mSelectedOverlayIndex = selectedOverlay;
                        // If selectedOverlay == mSelectedOverlay, de-select
                    } else {
                        mSelectedOverlayIndex = -1;
                    }
                }
            }
        });
        // Custom InfoWindow
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                if (getContext() == null) {
                    return null;
                }
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                View view = layoutInflater.inflate(R.layout.info_window_view, null);
                LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.linear_layout_top);
                Log.i(TAG, "Marker snippet: " + marker.getSnippet());
                String snippet = marker.getSnippet();
                // Marker or polygon?
                if (snippet.contains(MARKER_SNIPPET_HEADER)) {
                    Log.i(TAG, "Marker Snippet Case");
                    snippet = snippet.replace(MARKER_SNIPPET_HEADER, "");
                    TextView titleTextView = new TextView(getContext());
                    titleTextView.setText(marker.getTitle());
                    titleTextView.setTextSize(25);
                    linearLayout.addView(titleTextView);
                    TextView snippetTextView = new TextView(getContext());
                    snippetTextView.setText(snippet);
                    snippetTextView.setTextSize(20);
                    linearLayout.addView(snippetTextView);
                } else if (snippet.contains(POLYGON_SNIPPET_HEADER)) {
                    Log.i(TAG, "POlyGOn Snippet Case");
                    snippet = snippet.replace(POLYGON_SNIPPET_HEADER, "");
                    String[] topFiveCrimes
                            = snippet.replace("[", "").replace("]", "").replace("\"", "").split(",");
                    TextView textView;
                    String crime;
                    textView = new TextView(getContext());
                    textView.setText(marker.getTitle() + ": Top 5 Crimes:");
                    textView.setTextSize(25);
                    linearLayout.addView(textView);
                    for (int i = 0; i < topFiveCrimes.length; i++) {
                        crime = (i + 1) + ": ";
                        crime += topFiveCrimes[i];
                        textView = new TextView(getContext());
                        textView.setText(crime);
                        linearLayout.addView(textView);
                    }
                } else {
                    Log.e(TAG, "No particular type of marker...: " + snippet);
                }
                return view;
            }
        });
        // Enable/disable clustering on markers
        if (mOptions.getBoolean(Constants.CLUSTER_MARKERS_OPTION, false)) {
            mClusterManager = new ClusterManager<>(getContext(), mGoogleMap);
            mGoogleMap.setOnCameraChangeListener(mClusterManager);
            mGoogleMap.setOnMarkerClickListener(mClusterManager);
        }
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
        mOptions = getActivity().getSharedPreferences(
                Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
        outState.putParcelableArrayList(AREA_OVERLAYS_BUNDLE_KEY, mAreaOverlays);
        outState.putInt(SELECTED_OVERLAY_INDEX_BUNDLE_KEY, mSelectedOverlayIndex);
        outState.putParcelableArrayList(CRIME_MARKERS_BUNDLE_KEY, mMarkers);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mMapView.onPause();
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).marker.hideInfoWindow();
        }
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

    /**
     * Add crimes in Nova from the cache, subject to constraints on counties.
     *
     * @param countyConstraints array of counties which should be included.
     */
    public void addNovaCrimes(String[] countyConstraints) {
        Log.i(TAG, "addNovaCrimes()");
        Log.i(TAG, "countyConstraints: " + Arrays.toString(countyConstraints));
        Toast.makeText(getContext(), "Placing NOVA crime markers, this may take awhile.....",
                Toast.LENGTH_LONG).show();
        final Cache.NovaCrimeQuery novaCrimeQuery = new Cache.NovaCrimeQuery(getContext(), countyConstraints) {
            private int i = 0;
            @Override
            public void useData(final double latitude, final double longitude,
                    final String address, final String offense) {
                final LatLng latLng = new LatLng(latitude, longitude);
                if (latitude == 0.0) {
                    Log.d(TAG, "terrible LatLng: " + latLng);
                }
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mOptions.getBoolean(Constants.CLUSTER_MARKERS_OPTION, false)) {
                            Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                    .title(offense)
                                    .snippet(MARKER_SNIPPET_HEADER + address)
                                    .position(latLng));
                            mMarkers.add(new MarkerWrapper(marker));
                        } else {
                            mClusterManager.addItem(new MarkerClusterItem(latLng));
                        }
                        if (i == 0) {
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            if (mGoogleMap.getCameraPosition().zoom < DEFAULT_ZOOM_LEVEL) {
                                mGoogleMap.moveCamera(
                                        CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
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
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Done placing crime markers!!!",
                            Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "FINISHED PLACING NOVA CRIME MARKERS!");
            }
        });
    }

    /**
     * Add crimes in DC from the cache.
     */
    public void addDcCrimes() {
        Log.v(TAG, "addDcCrimes()");
        Toast.makeText(getContext(), "Placing DC crime markers, this may take awhile.....",
                Toast.LENGTH_LONG).show();
        final Cache.DcCrimeQuery dcCrimeQuery = new Cache.DcCrimeQuery(getContext()) {
            private int i = 0;
            @Override
            public void useData(final double latitude, final double longitude,
                    final String address, final String offense) {
                final LatLng latLng = new LatLng(latitude, longitude);
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .title(offense)
                                .snippet(MARKER_SNIPPET_HEADER + address)
                                .position(latLng));
                        mMarkers.add(new MarkerWrapper(marker));
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

    /**
     * Place crime markers in the areas defined by the List of Lists of LatLng objects.
     *
     * @param areas List of Lists of LatLng objects which define an area.
     */
    public void addAreaCrime(List<List<LatLng>> areas) {
        Log.v(TAG, "addAreaCrime");
        // Compute areaIds
        List<String> areaIds = new ArrayList<>();
        for (int i = 0; i < areas.size(); i++) {
            try {
                areaIds.add(Cache.computeAreaId(areas.get(i)));
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(getContext(), "Placing area markers...this will take a moment",
                Toast.LENGTH_SHORT).show();
        final Cache.AreaCrimeQuery areaCrimeQuery = new Cache.AreaCrimeQuery(getContext(), areaIds) {
            private int i = 0;
            @Override
            void useData(double latitude, double longitude,
                    final String address, final String offense) {
                final LatLng latLng = new LatLng(latitude, longitude);
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .title(offense)
                                .snippet(MARKER_SNIPPET_HEADER + address)
                                .position(latLng));
                        mMarkers.add(new MarkerWrapper(marker));
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
                    mCache.runQuery(areaCrimeQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "FINISHED PLACING AREA CRIME MARKERS!");
                Toast.makeText(getContext(), "Done placing area crime markers!!!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add a generic overlay of areas defined by Map<String, List<LatLng>>.
     *
     * @param areaBoundaries the boundaries of the overlays.
     * @param areaStatistics statistics relating to each overlay.
     */
    public void addAreaOverlay(Map<String, List<LatLng>> areaBoundaries,
            Map<String, Map<String, String>> areaStatistics) {
        Marker marker = null;
        Map<String, String> specificAreaStatistics;
        String statisticsString;
        Polygon polygon;
        Random random = new Random();
        for (String key : areaBoundaries.keySet()) {
            // Handle coloring for integer strings and regular strings
            int colorKey;
            if (isInteger(key)
                    && Integer.parseInt(key) < colors.length && Integer.parseInt(key) > -1) {
                colorKey = Math.abs((Integer.parseInt(key) + random.nextInt()) % colors.length);
                polygon = mGoogleMap.addPolygon(new PolygonOptions()
                        .addAll(areaBoundaries.get(key))
                        .fillColor(colors[colorKey])
                        .strokeColor(colors[colorKey])
                        .zIndex(20));
            } else {
                // Take hash code and mod by color length, this should produce random enough color
                // dispersion to be visually appealing.
                colorKey = Math.abs((key.hashCode() + random.nextInt()) % colors.length);
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
                LatLng centerPoint = findCenter(areaBoundaries.get(key));
                marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(centerPoint)
                        .icon(getCircleMarkerIcon())
                        .alpha(0)
                        .title("Cluster " + key)
                        .snippet(POLYGON_SNIPPET_HEADER + statisticsString));
            }
            if (areaStatistics.get(key) != null) {
                mAreaOverlays.add(new OverlayWrapper("Cluster " + key, colors[colorKey], polygon, marker,
                        areaStatistics.get(key)));
            } else {
                mAreaOverlays.add(new OverlayWrapper("Cluster " + key, colors[colorKey], polygon, null, null));
            }
        }
    }

    /**
     * Sets up a BitmapDescriptor in the shape of a circle.
     * The size is set to 5.
     *
     * @return BitmapDescriptor with the circle drawn in it.
     */
    private static BitmapDescriptor getCircleMarkerIcon() {
        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
        shapeDrawable.setIntrinsicHeight(5);
        shapeDrawable.setIntrinsicWidth(5);
        Bitmap bitmap = Bitmap.createBitmap(shapeDrawable.getIntrinsicWidth(),
                shapeDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        shapeDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Finds the center point of the bounding box around the polygon defined by polygonPoints.
     * Returns the center point as a LatLng object.
     *
     * @param polygonPoints the List of LatLng objects defining the polygon whose center is to be found.
     * @return the center point as a LatLng object.
     */
    private LatLng findCenter(List<LatLng> polygonPoints) {
        // Find min/max points of bounding box for polygons
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        LatLng latLng;
        for (int i = 0; i < polygonPoints.size(); i++) {
            latLng = polygonPoints.get(i);
            // Y
            if (latLng.latitude < minY) {
                minY = latLng.latitude;
            } else if (latLng.latitude > maxY) {
                maxY = latLng.latitude;
            }
            // X
            if (latLng.longitude < minX) {
                minX = latLng.longitude;
            } else if (latLng.longitude > maxX) {
                maxX = latLng.longitude;
            }
        }
        return new LatLng(minY + (maxY - minY) / 2, minX + (maxX - minX) / 2);
    }

    /**
     * Clears all overlays currently drawn on the GoogleMap.
     */
    public void clearOverlays() {
        for (int i = 0; i < mAreaOverlays.size(); i++) {
            mAreaOverlays.get(i).remove();
        }
        mAreaOverlays.clear();
        mSelectedOverlayIndex = -1;
    }

    /**
     * Clears all markers currently drawn on the GoogleMap.
     */
    public void clearMarkers() {
        Log.i(TAG, "clearMarkers called...");
        for (int i = 0; i < mMarkers.size(); i++) {
            mMarkers.get(i).marker.remove();
        }
        mMarkers.clear();
    }

    /**
     * Add overlays for counties in Virginia.
     */
    public void addCountyOverlay() {
        final Cache.NovaCountyQuery novaCountyQuery = new Cache.NovaCountyQuery(getContext()) {
            @Override
            public void useData(final String countyName, final List<LatLng> countyOutline,
                    final Map<String, String> countyStatistics) {
                // Run UI rendering on the main thread
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Polygon polygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(countyOutline)
                                .fillColor(getCountyAndDcColor(countyName))
                                .strokeColor(getCountyAndDcColor(countyName))
                                .zIndex(20));
                        LatLng centerPoint = findCenter(countyOutline);
                        String statisticsString = "";
                        for (String statisticsKey : countyStatistics.keySet()) {
                            statisticsString += countyStatistics.get(statisticsKey);
                            statisticsString += "\n";
                        }
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(centerPoint)
                                .icon(getCircleMarkerIcon())
                                .alpha(0)
                                .title(countyName)
                                .snippet(POLYGON_SNIPPET_HEADER + statisticsString));
                        mAreaOverlays.add(new OverlayWrapper(countyName,
                                getCountyAndDcColor(countyName), polygon, marker,
                                countyStatistics));
                    }
                });
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
                mHandler.removeCallbacks(mOverlayHoleFindingTask);
                mHandler.post(mOverlayHoleFindingTask);
            }
        });
    }

    /**
     * Add overlays for DC (potentially wards if that information surfaces...).
     */
    public void addDcOverlay() {
        final Cache.DcOutlineQuery dcOutlineQuery = new Cache.DcOutlineQuery(getContext()) {
            @Override
            public void useData(final String wardName, final List<LatLng> wardOutline,
                                final Map<String, String> wardStatistics) {
                // Run UI rendering on the main thread
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Polygon polygon = mGoogleMap.addPolygon(new PolygonOptions()
                                .addAll(wardOutline)
                                .fillColor(getCountyAndDcColor(wardName))
                                .strokeColor(getCountyAndDcColor(wardName))
                                .zIndex(20));
                        LatLng centerPoint = findCenter(wardOutline);
                        String statisticsString = "";
                        for (String statisticsKey : wardStatistics.keySet()) {
                            statisticsString += wardStatistics.get(statisticsKey);
                            statisticsString += "\n";
                        }
                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(centerPoint)
                                .icon(getCircleMarkerIcon())
                                .alpha(0)
                                .title(wardName)
                                .snippet(POLYGON_SNIPPET_HEADER + statisticsString));
                        mAreaOverlays.add(new OverlayWrapper(wardName,
                                getCountyAndDcColor(wardName), polygon, marker,
                                wardStatistics));
                    }
                });
            }
        };

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCache.runQuery(dcOutlineQuery);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "JSONException IN addAreaOverlay IN CrimeMapFragment");
                }
                mHandler.removeCallbacks(mOverlayHoleFindingTask);
                mHandler.post(mOverlayHoleFindingTask);
            }
        });
    }

    /**
     * Static assignment from counties and DC to colors for overlays.
     *
     * Colors:
     * int[] colors = {
     * 0:   0x8FFFB300, // Vivid Yellow
     * 1:   0x8F803E75, // Strong Purple
     * 2:   0x8FFF6800, // Vivid Orange
     * 3:   0x8FC10020, // Vivid Red
     * 4:   0x8FCEA262, // Grayish Yellow
     * 5:   0x8F817066, // Medium Gray
     * 6:   0x8F007D34, // Vivid Green
     * 7:   0x8FF6768E, // Strong Purplish Pink
     * 8:   0x8F00538A, // Strong Blue
     * 9:   0x8FFF7A5C, // Strong Yellowish Pink
     * 10:  0x8F53377A, // Strong Violet
     * 11:  0x8FFF8E00, // Vivid Orange Yellow
     * 12:  0x8FB32851, // Strong Purplish Red
     * 13:  0x8FF4C800, // Vivid Greenish Yellow
     * 14:  0x8F7F180D, // Strong Reddish Brown
     * 15:  0x8F93AA00, // Vivid Yellowish Green
     * 16:  0x8F593315, // Deep Yellowish Brown
     * 17:  0x8FF13A13, // Vivid Reddish Orange
     * 18:  0x8F232C16  // Dark Olive Green
     *};
     *
     * @param overlayAreaName the name of the county or DC.
     * @return the color assigned to each color or DC.
     */
    private static int getCountyAndDcColor(String overlayAreaName) {
        switch (overlayAreaName) {
            case "Alexandria":
                return colors[0]; // Vivid Yellow
            case "Arlington":
                return colors[3]; // Vivid Red
            case "District of Columbia":
                return colors[8]; // Strong Blue
            case "Falls Church":
                return colors[6]; // Vivid Green
            case "Fairfax":
                return colors[18]; // Dark Olive Green
            case "Fairfax City":
                return colors[7]; // Strong Purplish Pink
            case "Loudoun":
                return colors[12]; // Strong Purplish Red
            default:
                throw new IllegalArgumentException("Unknown overlay name passed " +
                        "to getCountyAndDcColor, overlayAreaName was: " + overlayAreaName);
        }
    }

    /**
     * Check if the input string is an Integer.
     *
     * @param inputData the string to check for integer-ness (not a word).
     * @return true if inputData is integer, false otherwise.
     */
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