package com.edg.crimeonthemove;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataSettingsFragment extends Fragment {

    private static final String TAG = "DataSettingsFragment";
    private static final String SELECTED_NOVA_CRIME_TYPES_KEY = "SelectedNovaCrimeTypesKey";
    private static final String SELECTED_DC_CRIME_TYPES_KEY = "SelectedDcCrimeTypesKey";

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

        // DC Crime types to include
        if (!preferences.contains(Constants.SELECTED_DC_CRIME_TYPES_OPTION) || force) {
            // null corresponds to all types
            editor.putStringSet(Constants.SELECTED_DC_CRIME_TYPES_OPTION, null);
        }
        // NOVA Crime types to include
        if (!preferences.contains(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION) || force) {
            // null corresponds to all types
            editor.putStringSet(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, null);
        }

        // Cluster Markers Option
        if (!preferences.contains(Constants.CLUSTER_MARKERS_OPTION) || force) {
            editor.putBoolean(Constants.CLUSTER_MARKERS_OPTION, true);
        }

        editor.apply();
    }

    //~Fields---------------------------------------------------------------------------------------
    private Cache mCache;
    private SharedPreferences mOptions;
    private Set<String> mSelectedDcCrimeTypes;
    private Set<String> mSelectedNovaCrimeTypes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCache = new Cache();
        if (savedInstanceState == null) {
            mSelectedNovaCrimeTypes = new HashSet<>();
            mSelectedDcCrimeTypes = new HashSet<>();
        } else {
            if (mSelectedDcCrimeTypes.contains(SELECTED_DC_CRIME_TYPES_KEY)) {
                // noinspection ConstantConditions
                mSelectedDcCrimeTypes = new HashSet<>(
                        savedInstanceState.getStringArrayList(SELECTED_DC_CRIME_TYPES_KEY));
            }
            if (mSelectedDcCrimeTypes.contains(SELECTED_NOVA_CRIME_TYPES_KEY)) {
                // noinspection ConstantConditions
                mSelectedNovaCrimeTypes = new HashSet<>(
                    savedInstanceState.getStringArrayList(SELECTED_NOVA_CRIME_TYPES_KEY));
            }

        }
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
        RadioButton radioButton = null;
        switch (mOptions.getInt(Constants.CLUSTERING_SELECTION, -1)) {
            case Constants.K_MEANS_SELECTED:
                radioButton = (RadioButton) clusteringAlgorithmRadioGroup
                        .findViewById(R.id.radio_button_k_means);
                break;
            case Constants.SPECTRAL_CLUSTERING_SELECTED:
                radioButton = (RadioButton) clusteringAlgorithmRadioGroup
                        .findViewById(R.id.radio_button_spectral_clustering);
                break;
            /*
            case Constants.AFFINITY_PROPAGATION_SELECTED:
                radioButton = (RadioButton) clusteringAlgorithmRadioGroup
                        .findViewById(R.id.radio_button_affinity_propagation);
                break;
            */
            default:
                Log.e(TAG, "PROBLEM in initializing clustering selection radio buttons, default case hit!");
                break;
        }
        try {
            //noinspection ConstantConditions
            radioButton.setChecked(true);
        } catch (NullPointerException exception) {
            exception.printStackTrace();
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
                    /*
                    case R.id.radio_button_affinity_propagation:
                        editor.putInt(Constants.CLUSTERING_SELECTION, Constants.AFFINITY_PROPAGATION_SELECTED);
                        break;
                    */
                    default:
                        Log.e(TAG, "PROBLEM IN onCheckedChanged for clustering algorithm selection, default case hit!");
                        break;
                }
                editor.apply();
            }
        });

        // Clustering features selection (crime type)
        // CRIME CATEGORY SELECTION-----------------------------------------------------------------
        // DC
        Set<String> currentNovaCrimeTypes = mOptions.getStringSet(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, new HashSet<String>());
        Set<String> currentDcCrimeTypes = mOptions.getStringSet(Constants.SELECTED_DC_CRIME_TYPES_OPTION, new HashSet<String>());
        final CheckBox mildSeverityCheckBox = (CheckBox) view.findViewById(R.id.checkbox_mild_crime_severity);
        if (currentNovaCrimeTypes.containsAll(Arrays.asList(Constants.NOVA_MILD_SEVERITY_CRIMES))
                && currentDcCrimeTypes.containsAll(Arrays.asList(Constants.DC_MILD_SEVERITY_CRIMES))) {
            mildSeverityCheckBox.setChecked(true);
        }
        final CheckBox moderateSeverityCheckBox = (CheckBox) view.findViewById(R.id.checkbox_moderate_crime_severity);
        if (currentNovaCrimeTypes.containsAll(Arrays.asList(Constants.NOVA_MODERATE_SEVERITY_CRIMES))
                && currentDcCrimeTypes.containsAll(Arrays.asList(Constants.DC_MODERATE_SEVERITY_CRIMES))) {
            moderateSeverityCheckBox.setChecked(true);
        }
        final CheckBox extremeSeverityCheckBox = (CheckBox) view.findViewById(R.id.checkbox_extreme_crime_severity);
        if (currentNovaCrimeTypes.containsAll(Arrays.asList(Constants.NOVA_EXTREME_SEVERITY_CRIMES))
                && currentDcCrimeTypes.containsAll(Arrays.asList(Constants.DC_EXTREME_SEVERITY_CRIMES))) {
            extremeSeverityCheckBox.setChecked(true);
        }
        final CompoundButton.OnCheckedChangeListener severityListener
                = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.remove(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION);
                editor.remove(Constants.SELECTED_DC_CRIME_TYPES_OPTION);
                HashSet<String> novaCrimes = new HashSet<>();
                HashSet<String> dcCrimes = new HashSet<>();
                // Mild
                if ((buttonView.getId() != mildSeverityCheckBox.getId()
                            && mildSeverityCheckBox.isChecked())
                        || (buttonView.getId() == mildSeverityCheckBox.getId() && isChecked)) {
                    novaCrimes.addAll(Arrays.asList(Constants.NOVA_MILD_SEVERITY_CRIMES));
                    dcCrimes.addAll(Arrays.asList(Constants.DC_MILD_SEVERITY_CRIMES));
                }
                // Moderate
                if ((buttonView.getId() != moderateSeverityCheckBox.getId()
                            && moderateSeverityCheckBox.isChecked())
                        || (buttonView.getId() == moderateSeverityCheckBox.getId() && isChecked)) {
                    novaCrimes.addAll(Arrays.asList(Constants.NOVA_MODERATE_SEVERITY_CRIMES));
                    dcCrimes.addAll(Arrays.asList(Constants.DC_MODERATE_SEVERITY_CRIMES));
                }
                // Extreme
                if ((buttonView.getId() != extremeSeverityCheckBox.getId()
                        && extremeSeverityCheckBox.isChecked())
                        || (buttonView.getId() == extremeSeverityCheckBox.getId() && isChecked)) {
                    novaCrimes.addAll(Arrays.asList(Constants.NOVA_EXTREME_SEVERITY_CRIMES));
                    dcCrimes.addAll(Arrays.asList(Constants.DC_EXTREME_SEVERITY_CRIMES));
                }

                editor.putStringSet(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, novaCrimes);
                editor.putStringSet(Constants.SELECTED_DC_CRIME_TYPES_OPTION, dcCrimes);
                editor.apply();
            }
        };
        mildSeverityCheckBox.setOnCheckedChangeListener(severityListener);
        moderateSeverityCheckBox.setOnCheckedChangeListener(severityListener);
        extremeSeverityCheckBox.setOnCheckedChangeListener(severityListener);

        // SPECIFIC CRIME SELECTION-----------------------------------------------------------------
        // DC
        Button setDcClusteringFeatures = (Button) view.findViewById(R.id.button_set_dc_clustering_features);
        final AlertDialog.Builder dcAlertBuilder = new AlertDialog.Builder(getContext());
        dcAlertBuilder.setMultiChoiceItems(Constants.DC_CRIME_TYPES, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    mSelectedDcCrimeTypes.add(Constants.DC_CRIME_TYPES[which]);
                } else {
                    mSelectedDcCrimeTypes.remove(Constants.DC_CRIME_TYPES[which]);
                }
            }
        });
        dcAlertBuilder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG, "dcAlertBuilder:PostiveButton:onClick: " + mSelectedDcCrimeTypes);
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putStringSet(Constants.SELECTED_DC_CRIME_TYPES_OPTION, new HashSet<>(mSelectedDcCrimeTypes));
                editor.apply();
                mSelectedDcCrimeTypes.clear();
            }
        });
        dcAlertBuilder.setNeutralButton("Reset Options and Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putStringSet(Constants.SELECTED_DC_CRIME_TYPES_OPTION, null);
                editor.apply();
                mSelectedDcCrimeTypes.clear();
            }
        });
        dcAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mSelectedDcCrimeTypes.clear();
            }
        });
        setDcClusteringFeatures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dcAlertBuilder.create().show();
            }
        });
        // NOVA
        Button setNovaClusteringFeatures = (Button) view.findViewById(R.id.button_set_nova_clustering_features);
        final AlertDialog.Builder novaAlertBuilder = new AlertDialog.Builder(getContext());
        novaAlertBuilder.setMultiChoiceItems(Constants.NOVA_CRIME_TYPES, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    mSelectedNovaCrimeTypes.add(Constants.NOVA_CRIME_TYPES[which]);
                } else {
                    mSelectedNovaCrimeTypes.remove(Constants.NOVA_CRIME_TYPES[which]);
                }
            }
        });
        novaAlertBuilder.setNeutralButton("Reset Options and Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putStringSet(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, null);
                editor.apply();
                mSelectedNovaCrimeTypes.clear();
            }
        });
        novaAlertBuilder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putStringSet(Constants.SELECTED_NOVA_CRIME_TYPES_OPTION, new HashSet<>(mSelectedNovaCrimeTypes));
                editor.apply();
                mSelectedNovaCrimeTypes.clear();
            }
        });
        novaAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mSelectedNovaCrimeTypes.clear();
            }
        });
        setNovaClusteringFeatures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                novaAlertBuilder.create().show();
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

        // Enable/Disable visual marker clustering
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