package com.edg.crimeonthemove;

import android.app.Activity;
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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

public class DataSettingsFragment extends Fragment {

    public interface OnFragmentInteractionListener {}

    private static final String TAG = "DataSettingsFragment";

    public static DataSettingsFragment newInstance() {
        return new DataSettingsFragment();
    }

    public static String getFragmentTitle() {
        return "Data Settings";
    }

    private OnFragmentInteractionListener mListener;
    private SharedPreferences mOptions;

    // Options UI objects
    private CheckBox mNovaData;
    private CheckBox mDCData;

    private RadioGroup mClusteringAlgorithm;

    private SeekBar mKMeansClusters;
    private SeekBar mSpectralClusters;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_settings, container, false);

        mNovaData = (CheckBox) view.findViewById(R.id.checkbox_nova_data);
        mNovaData.setChecked(mOptions.getBoolean(Constants.NOVA_DATA_OPTION, false));
        mNovaData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putBoolean(Constants.NOVA_DATA_OPTION, isChecked);
                editor.apply();
            }
        });
        mDCData = (CheckBox) view.findViewById(R.id.checkbox_dc_data);
        mDCData.setChecked(mOptions.getBoolean(Constants.DC_DATA_OPTION, false));
        mDCData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putBoolean(Constants.DC_DATA_OPTION, isChecked);
                editor.apply();
            }
        });

        mClusteringAlgorithm = (RadioGroup) view.findViewById(R.id.radio_group_clustering_algorithm_choice);
        mClusteringAlgorithm.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = mOptions.edit();
                Log.i(TAG, "checkedId: " + checkedId);
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

        mKMeansClusters = (SeekBar) view.findViewById(R.id.seek_bar_k_means_clusters);
        mKMeansClusters.setMax(Constants.MAX_CLUSTERS);
        mKMeansClusters.setProgressDrawable(getSeekBarBackground(mKMeansClusters));
        mKMeansClusters.setProgress(mOptions.getInt(Constants.K_MEANS_CLUSTERS_OPTION, -1));
        mKMeansClusters.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putInt(Constants.K_MEANS_CLUSTERS_OPTION, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mSpectralClusters = (SeekBar) view.findViewById(R.id.seek_bar_spectral_clustering_clusters);
        mSpectralClusters.setMax(Constants.MAX_CLUSTERS);
        mSpectralClusters.setProgressDrawable(getSeekBarBackground(mSpectralClusters));
        mSpectralClusters.setProgress(mOptions.getInt(Constants.SPECTRAL_CLUSTERS_OPTION, -1));
        mSpectralClusters.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SharedPreferences.Editor editor = mOptions.edit();
                editor.putInt(Constants.SPECTRAL_CLUSTERS_OPTION, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }

    private Drawable getSeekBarBackground(SeekBar seekBar) {
        seekBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = seekBar.getMeasuredWidth();
        int height = seekBar.getMeasuredHeight();
        Log.i(TAG, "seekBar width: " + width + "  seekbar height: " + height);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mOptions = activity.getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}