package com.edg.crimeonthemove;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

public class DataSettingsFragment extends Fragment {

    private static final String TAG = "DataSettingsFragment";

    public static DataSettingsFragment newInstance() {
        return new DataSettingsFragment();
    }

    public static String getFragmentTitle() {
        return "Data Settings";
    }

    /**
     * Initializes all the default options.
     *
     * @param context used to access the SharedPreferences.
     */
    public static void initializeDefaultOptions(Context context) {
        initializeDefaultOptions(context, false);
    }

    /**
     * Initializes all the default options.
     *
     * @param context used to access the SharedPreferences.
     * @param force if true all default options are set, overriding any existing options.
     */
    public static void initializeDefaultOptions(Context context, boolean force) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        // Nova data selection
        if (!preferences.contains(Constants.NOVA_DATA_OPTION) || force) {
            editor.putBoolean(Constants.NOVA_DATA_OPTION, true);
        }
        // DC data selection
        if (!preferences.contains(Constants.DC_DATA_OPTION) || force) {
            editor.putBoolean(Constants.DC_DATA_OPTION, true);
        }
        // Nova county selections
        for (String countyKey : Constants.COUNTY_CRIME_OPTIONS) {
            if (!preferences.contains(countyKey) || force) {
                editor.putBoolean(countyKey, true);
            }
        }

        // Clustering type selection
        if (!preferences.contains(Constants.CLUSTERING_SELECTION) || force) {
            editor.putInt(Constants.CLUSTERING_SELECTION, Constants.K_MEANS_SELECTED);
        }
        // Num clusters selection
        if (!preferences.contains(Constants.NUM_CLUSTERS_OPTION) || force) {
            editor.putInt(Constants.NUM_CLUSTERS_OPTION, 4);
        }

        // Cluster Markers Option
        if (!preferences.contains(Constants.CLUSTER_MARKERS_OPTION) || force) {
            editor.putBoolean(Constants.CLUSTER_MARKERS_OPTION, true);
        }

        editor.apply();
    }

    private Cache mCache;
    private SharedPreferences mOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCache = new Cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_data_settings, container, false);

        // Nova checkbox
        CheckBox mNovaData = (CheckBox) view.findViewById(R.id.checkbox_nova_data);
        setupCheckBox(mNovaData, Constants.NOVA_DATA_OPTION);

        // DC Checkbox
        CheckBox mDCData = (CheckBox) view.findViewById(R.id.checkbox_dc_data);
        setupCheckBox(mDCData, Constants.DC_DATA_OPTION);

        // County selection checkboxes
        CheckBox countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_alexandria);
        setupCheckBox(countyCheckBox, Constants.ALEXANDRIA_COUNTY_CRIME_OPTION);
        countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_arlington);
        setupCheckBox(countyCheckBox, Constants.ARLINGTON_COUNTY_CRIME_OPTION);
        countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_fairfax);
        setupCheckBox(countyCheckBox, Constants.FAIRFAX_COUNTY_CRIME_OPTION);
        countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_fairfax_city);
        setupCheckBox(countyCheckBox, Constants.FAIRFAX_CITY_CRIME_OPTION);
        countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_falls_church);
        setupCheckBox(countyCheckBox, Constants.FALLS_CHURCH_COUNTY_CRIME_OPTION);
        countyCheckBox = (CheckBox) view.findViewById(R.id.checkbox_county_loudoun);
        setupCheckBox(countyCheckBox, Constants.LOUDOUN_COUNTY_CRIME_OPTION);

        // Clustering algorithm selection
        RadioGroup clusteringAlgorithmRadioGroup
                = (RadioGroup) view.findViewById(R.id.radio_group_clustering_algorithm_choice);
        // Ensure radio button selections match current settings
        RadioButton radioButton;
        switch (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1)) {
            case Constants.K_MEANS_SELECTED:
                radioButton = (RadioButton) clusteringAlgorithmRadioGroup
                        .findViewById(R.id.radio_button_k_means);
                radioButton.setChecked(true);
                break;
            case Constants.SPECTRAL_CLUSTERING_SELECTED:
                radioButton = (RadioButton) clusteringAlgorithmRadioGroup
                        .findViewById(R.id.radio_button_spectral_clustering);
                radioButton.setChecked(true);
                break;
            default:
                Log.e(TAG, "PROBLEM in initializing clustering selection radio buttons, default case hit!");
                break;
        }
        // Setup on select behavior
        clusteringAlgorithmRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = mOptions.edit();
                switch (checkedId) {
                    case R.id.radio_button_k_means:
                        editor.putInt(Constants.CLUSTERING_SELECTION, Constants.K_MEANS_SELECTED);
                        break;
                    case R.id.radio_button_spectral_clustering:
                        editor.putInt(Constants.CLUSTERING_SELECTION, Constants.SPECTRAL_CLUSTERING_SELECTED);
                        break;
                    default:
                        Log.e(TAG, "PROBLEM IN onCheckedChanged for clustering algorithm selection, default case hit!");
                        break;
                }
                editor.apply();
            }
        });

        // Set/get the number of clusters using a seek bar
        SeekBar numClusters = (SeekBar) view.findViewById(R.id.seek_bar_num_clusters);
        numClusters.setMax(Constants.MAX_CLUSTERS);
        numClusters.setProgressDrawable(getSeekBarBackground(numClusters));
        numClusters.setProgress(mOptions.getInt(Constants.NUM_CLUSTERS_OPTION, -1));
        numClusters.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putInt(Constants.NUM_CLUSTERS_OPTION, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        CheckBox markerClusteringCheckBox = (CheckBox) view.findViewById(R.id.checkbox_marker_clustering);
        setupCheckBox(markerClusteringCheckBox, Constants.CLUSTER_MARKERS_OPTION);

        // Clear the cache
        Button clearCacheButton = (Button) view.findViewById(R.id.button_clear_cache);
        clearCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCache.clearCache(getContext());
            }
        });

        // Reset the options
        Button resetOptionsButton = (Button) view.findViewById(R.id.button_reset_settings);
        resetOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeDefaultOptions(getContext(), true);
            }
        });

        return view;
    }

    private void setupCheckBox(CheckBox checkBox, final String key) {
        checkBox.setChecked(mOptions.getBoolean(key, false));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putBoolean(key, isChecked);
                editor.apply();
            }
        });
    }

    /**
     * Set up the background for the passed in SeekBar.
     *
     * @param seekBar the SeekBar whose background is getting set, used for measurements.
     * @return a Drawable to go in the background of seekBar.
     */
    private Drawable getSeekBarBackground(SeekBar seekBar) {
        seekBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = seekBar.getMeasuredWidth();
        int height = seekBar.getMeasuredHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.WHITE);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.LTGRAY);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(15);
        textPaint.setTextScaleX((float) 0.7);

        final int offset = width / Constants.MAX_CLUSTERS;
        final int tickBoundaries = 20;
        final float numberShift = (float) 2.5;
        float tickPosition = 0;
        float textXPosition;
        for(int i = 0; i <= Constants.MAX_CLUSTERS; i++) {
            canvas.drawLine(tickPosition, tickBoundaries, tickPosition, (height - tickBoundaries), tickPaint);
            if (i == 0) {
                textXPosition = tickPosition - numberShift / 2;
            } else if (i == Constants.MAX_CLUSTERS) {
                textXPosition = tickPosition - 2 * numberShift;
            } else {
                textXPosition = tickPosition - numberShift;
            }
            canvas.drawText(Integer.toString(i), textXPosition, height, textPaint);
            tickPosition += offset;
        }
        return new BitmapDrawable(getResources(), bitmap);
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        mOptions = getActivity().getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mOptions = context.getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}