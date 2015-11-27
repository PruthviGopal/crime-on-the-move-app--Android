package com.edg.crimeonthemove;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends FragmentActivity
        implements CrimeMapFragment.OnFragmentInteractionListener,
        DataSettingsFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    private static final String CURRENT_FRAGMENT_INDEX_KEY = "CurrentFragmentIndexKey";

    private static final int CRIME_MAP_FRAGMENT_INDEX = 0;

    /**
     * Used to store the last screen title.
     */
    private CharSequence mTitle;

    /**
     * Navigation Drawer.
     */
    private DrawerLayout mDrawerLayout;

    /**
     * ActionBar Drawer Toggle for the Navigation Drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * ListView for the DrawerLayout.
     */
    private ListView mDrawerList;

    /**
     * List of FragmentInfos representing the fragments which are available from the nav drawer.
     */
    private List<FragmentInfo> mFragments;

    /**
     * Index of currently displayed (or to display) fragment.
     */
    private int mCurrentFragmentIndex;

    private WebServiceClient mWebServiceClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize state variables
        initializeDefaultOptions();
        mCurrentFragmentIndex = CRIME_MAP_FRAGMENT_INDEX;
        if (savedInstanceState != null) {
            mCurrentFragmentIndex = savedInstanceState.getInt(CURRENT_FRAGMENT_INDEX_KEY);
        }

        // Navigation Drawer setup
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mFragments = new ArrayList<FragmentInfo>(4);
        mFragments.add(new FragmentInfo(CrimeMapFragment.getFragmentTitle()) {
            @Override
            public Fragment getInstance() {
                return CrimeMapFragment.newInstance();
            }
        });

        mFragments.add(new FragmentInfo(DataSettingsFragment.getFragmentTitle()) {
            @Override
            public Fragment getInstance() {
                return DataSettingsFragment.newInstance();
            }
        });

        // Set up UI elements
        setupNavDrawer();
        initFragment(mCurrentFragmentIndex);

        // Create web service client
        mWebServiceClient = new WebServiceClient();
    }

    /**
     * Sets up the navigation drawer view items etc.
     */
    private void setupNavDrawer() {
        mDrawerList = (ListView) findViewById(R.id.list_view_nav_drawer);
        mDrawerList.setAdapter(new NavigationDrawerAdapter());
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerList.setItemChecked(position, true);
                mDrawerLayout.closeDrawers();
                initFragment(position);
                mCurrentFragmentIndex = position;
            }
        });
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
    }

    /**
     * Handles fragment transactions for fragments in the Navigation Drawer.
     */
    private void initFragment(int fragmentIndex) {
        FragmentInfo fragmentInfo = mFragments.get(fragmentIndex);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragmentInfo.getInstance(), fragmentInfo.getTag())
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragmentInfo.getInstance(), fragmentInfo.getTag())
                    .addToBackStack(fragmentInfo.getTag())
                    .commit();
        }
        mTitle = fragmentInfo.getTag();
    }

    private void initializeDefaultOptions() {
        SharedPreferences preferences = getSharedPreferences(Constants.DATA_OPTIONS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.getBoolean(Constants.NOVA_DATA_OPTION, false)) {
            editor.putBoolean(Constants.NOVA_DATA_OPTION, true);
        }
        if (preferences.getBoolean(Constants.DC_DATA_OPTION, false)) {
            editor.putBoolean(Constants.DC_DATA_OPTION, true);
        }
        if (preferences.getInt(Constants.CLUSTERING_SELECTION, -1) == -1) {
            editor.putInt(Constants.CLUSTERING_SELECTION, Constants.K_MEANS_SELECTED);
        }
        if (preferences.getInt(Constants.K_MEANS_CLUSTERS_OPTION, -1) == -1) {
            editor.putInt(Constants.K_MEANS_CLUSTERS_OPTION, 4);
        }
        if (preferences.getInt(Constants.SPECTRAL_CLUSTERS_OPTION, -1) == -1) {
            editor.putInt(Constants.SPECTRAL_CLUSTERS_OPTION, 4);
        }
        editor.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_FRAGMENT_INDEX_KEY, mCurrentFragmentIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void getDCCrimeData() {
        mWebServiceClient.getDCCrimes(new WebServiceClient.RawDataCommunicatorInterface() {
            @Override
            public void useResults(List<Map<String, String>> jsonList) {
                FragmentInfo fragmentInfo = mFragments.get(mCurrentFragmentIndex);
                if (fragmentInfo.getTag().equals(CrimeMapFragment.getFragmentTitle())) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    CrimeMapFragment crimeMapFragment = (CrimeMapFragment) fragmentManager
                            .findFragmentByTag(fragmentInfo.getTag());
                    crimeMapFragment.addCrimes(jsonList);
                }
            }
        });
    }

    @Override
    public void getNovaCrimeData() {
        mWebServiceClient.getNovaCrimes(new WebServiceClient.RawDataCommunicatorInterface() {
            @Override
            public void useResults(List<Map<String, String>> jsonList) {
                FragmentInfo fragmentInfo = mFragments.get(mCurrentFragmentIndex);
                if (fragmentInfo.getTag().equals(CrimeMapFragment.getFragmentTitle())) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    CrimeMapFragment crimeMapFragment = (CrimeMapFragment) fragmentManager
                            .findFragmentByTag(fragmentInfo.getTag());
                    crimeMapFragment.addCrimes(jsonList);
                }
            }
        });
    }

    @Override
    public void getKMeansClusteringData(Map<String, String> params) {
        mWebServiceClient.getKMeans(new WebServiceClient.GeographicAreaResultsCommunicatorInterface() {
            @Override
            public void useResults(Map<String, List<LatLng>> jsonMapOfLists,
                    Map<String, Map<String, String>> areaStatistics) {
                Log.v(TAG, "getKMeansClusteringData.useResults()");
                FragmentInfo fragmentInfo = mFragments.get(CRIME_MAP_FRAGMENT_INDEX);//mCurrentFragmentIndex);
                if (fragmentInfo.getTag().equals(CrimeMapFragment.getFragmentTitle())) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    CrimeMapFragment crimeMapFragment = (CrimeMapFragment) fragmentManager
                            .findFragmentByTag(fragmentInfo.getTag());
                    crimeMapFragment.addAreaOverlay(jsonMapOfLists, areaStatistics);
                }
            }
        }, params);
    }

    @Override
    public void getSpectralClusteringData(Map<String, String> params) {
        mWebServiceClient.getSpectralClustering(new WebServiceClient.GeographicAreaResultsCommunicatorInterface() {
            @Override
            public void useResults(Map<String, List<LatLng>> jsonMapOfLists,
                    Map<String, Map<String, String>> areaStatistics) {
                Log.v(TAG, "getSpectralClusteringData.useResults()");
                FragmentInfo fragmentInfo = mFragments.get(mCurrentFragmentIndex);
                if (fragmentInfo.getTag().equals(CrimeMapFragment.getFragmentTitle())) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    CrimeMapFragment crimeMapFragment = (CrimeMapFragment) fragmentManager
                            .findFragmentByTag(fragmentInfo.getTag());
                    crimeMapFragment.addAreaOverlay(jsonMapOfLists, areaStatistics);
                }
            }
        }, params);
    }

    @Override
    public void getCountyOverlays(Map<String, String> params) {
        mWebServiceClient.getCountyOutlines(this, new WebServiceClient.GeographicAreaResultsCommunicatorInterface() {
            @Override
            public void useResults(Map<String, List<LatLng>> jsonMapOfLists,
                    Map<String, Map<String, String>> areaStatistics) {
                FragmentInfo fragmentInfo = mFragments.get(mCurrentFragmentIndex);
                if (fragmentInfo.getTag().equals(CrimeMapFragment.getFragmentTitle())) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    CrimeMapFragment crimeMapFragment = (CrimeMapFragment) fragmentManager
                            .findFragmentByTag(fragmentInfo.getTag());
                    crimeMapFragment.addCountyOverlay(null, null);
                }
            }
        }, params);
    }

    /**
     * Adapter for the NavigationDrawer.
     */
    private class NavigationDrawerAdapter extends BaseAdapter {
        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return mFragments.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return mFragments.get(position).getInstance();
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(getApplicationContext());
            textView.setTextSize(20);
            textView.setText(mFragments.get(position).getTag());
            return textView;
        }
    }

    /**
     * Helper class which holds information about fragments and can instantiate them.
     */
    private abstract class FragmentInfo {

        private String mTag;

        public FragmentInfo(String tag) {
            mTag = tag;
        }

        public String getTag() {
            return mTag;
        }

        public abstract Fragment getInstance();
    }
}
