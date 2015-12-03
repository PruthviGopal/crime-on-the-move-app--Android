package com.edg.crimeonthemove;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used to store constants for application which are globally (or at least broadly) useful.
 */
public class Constants {

    // App Constants
    public static final String DATA_OPTIONS_NAME = "CrimeOnTheMoveDataOptions";

    public static final String NOVA_DATA_OPTION = "NovaDataEnabledOption";
    public static final String DC_DATA_OPTION = "DCDataEnabledOption";

    public static final String ALEXANDRIA_COUNTY_CRIME_OPTION = "AlexandriaCountyCrimeOption";
    public static final String ARLINGTON_COUNTY_CRIME_OPTION = "ArlingtonCountyCrimeOption";
    public static final String FALLS_CHURCH_COUNTY_CRIME_OPTION = "FallsChurchCountyCrimeOption";
    public static final String FAIRFAX_COUNTY_CRIME_OPTION = "FairfaxCountyCrimeOption";
    public static final String FAIRFAX_CITY_CRIME_OPTION = "FairfaxCityCrimeOption";
    public static final String LOUDOUN_COUNTY_CRIME_OPTION = "LoudounCountyCrimeOption";
    public static final String[] COUNTY_CRIME_OPTIONS = {
            ALEXANDRIA_COUNTY_CRIME_OPTION,
            ARLINGTON_COUNTY_CRIME_OPTION,
            FAIRFAX_CITY_CRIME_OPTION,
            FAIRFAX_COUNTY_CRIME_OPTION,
            FALLS_CHURCH_COUNTY_CRIME_OPTION,
            LOUDOUN_COUNTY_CRIME_OPTION
    };
    public static final Map<String, String> COUNTY_NAMES = new HashMap<>(6);
    static {
        COUNTY_NAMES.put(ALEXANDRIA_COUNTY_CRIME_OPTION, "Alexandria");
        COUNTY_NAMES.put(ARLINGTON_COUNTY_CRIME_OPTION, "Arlington");
        COUNTY_NAMES.put(FAIRFAX_CITY_CRIME_OPTION, "Fairfax City");
        COUNTY_NAMES.put(FAIRFAX_COUNTY_CRIME_OPTION, "Fairfax");
        COUNTY_NAMES.put(FALLS_CHURCH_COUNTY_CRIME_OPTION, "Falls Church");
        COUNTY_NAMES.put(LOUDOUN_COUNTY_CRIME_OPTION, "Loudoun");
    }

    public static final String NUM_CLUSTERS_OPTION = "NumClustersOption";

    public static final String CLUSTERING_SELECTION = "ClusteringVarietySelection";
    public static final int K_MEANS_SELECTED = 0;
    public static final int SPECTRAL_CLUSTERING_SELECTED = 1;

    public static final int MAX_CLUSTERS = 10;

    // Server Side Parameters
    public static final String BACK_END_NUM_CLUSTERS_PARAM = "num_clusters";
    public static final String BACK_END_NOVA_DATA_PARAM = "nova_data";
    public static final String BACK_END_DC_DATA_PARAM = "dc_data";
}
