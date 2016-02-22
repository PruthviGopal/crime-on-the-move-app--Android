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

    public static final String CLUSTER_MARKERS_OPTION = "ClusterMarkersOption";

    public static final String CLUSTERING_SELECTION = "ClusteringVarietySelection";
    public static final int K_MEANS_SELECTED = 0;
    public static final int SPECTRAL_CLUSTERING_SELECTED = 1;
    public static final int AFFINITY_PROPAGATION_SELECTED = 2;

    public static final int MAX_CLUSTERS = 10;

    public static final String SELECTED_DC_CRIME_TYPES_OPTION = "SelectedDcCrimeTypesOption";
    public static final String SELECTED_NOVA_CRIME_TYPES_OPTION = "SelectedNovaCrimeTypesOption";

    // Types of crimes which can be selected upon in DC (also covers EVERY type of crime for DC)
    public static final String[] DC_CRIME_TYPES = {
            "MOTOR VEHICLE THEFT",
            "ROBBERY",
            "THEFT/OTHER",
            "BURGLARY",
            "SEX ABUSE",
            "HOMICIDE",
            "THEFT F/AUTO",
            "ARSON",
            "ASSAULT W/DANGEROUS WEAPON"
    };
    // Types of crimes which can be selected upon in NOVA (DOES NOT cover EVERY type of crime for NOVA)
    public static final String[] NOVA_CRIME_TYPES = {
            "larceny",
            "motor vehicle theft",
            "burglary",
            "robbery",
            "destruction of property",
            "larceny from a vehicle",
            "malicious wounding",
            "assault",
            "armed robbery",
            "drunk in public",
            "vandalism",
            "indecent exposure",
            "grand larceny",
            "suspicious event",
            "peeping tom",
            "hit and run",
            "felonious assault",
            "fraud",
            "larceny from building",
            "sexual assault",
            "abduction",
            "bank robbery",
            "unlawful entry",
            "larceny shoplifting",
            "battery",
            "robbery by force",
            "graffiti",
            "sexual battery",
            "tampering with a vehicle",
            "petit larceny",
            "damage to property",
            "exposure",
            "homicide",
            "assault by mob",
            "driving under the influence",
            "breaking and entering",
            "driving while intoxicated",
            "trespassing",
            "fatal crash",
            "carjacking",
            "disorderly conduct",
            "simple assault",
            "arson",
            "residential burgalry",
            "burglary of occupied dwelling",
            "found property",
            "domestic dispute"
    };

    // DC crime categories
    public static final String[] DC_MILD_SEVERITY_CRIMES = {
            "THEFT/OTHER"
    };
    public static final String[] DC_MODERATE_SEVERITY_CRIMES = {
            "MOTOR VEHICLE THEFT",
            "ROBBERY",
            "THEFT F/AUTO",
            "BURGLARY"
    };
    public static final String[] DC_EXTREME_SEVERITY_CRIMES = {
            "SEX ABUSE",
            "HOMICIDE",
            "ARSON",
            "ASSAULT W/DANGEROUS WEAPON"
    };


    // Nova crime categories
    public static final String[] NOVA_MILD_SEVERITY_CRIMES = {
            "drunk in public",
            "other violations",
            "vandalism",
            "suspicious event",
            "graffiti",
            "trespassing",
            "disorderly conduct",
            "found property",
            "domestic dispute"
    };
    public static final String[] NOVA_MODERATE_SEVERITY_CRIMES = {
            "grand larceny",
            "larceny",
            "motor vehicle theft",
            "burglary",
            "robbery",
            "destruction of property",
            "larceny from a vehicle",
            "indecent exposure",
            "peeping tom",
            "hit and run",
            "fraud",
            "larceny from building",
            "unlawful entry",
            "larceny shoplifting",
            "tampering with a vehicle",
            "petit larceny",
            "damage to property",
            "exposure",
            "driving under the influence",
            "animal",
            "breaking and entering",
            "driving while intoxicated",
            "fatal crash",
            "residential burgalry",
            "pursuit"
    };
    public static final String[] NOVA_EXTREME_SEVERITY_CRIMES = {
            "battery",
            "simple assault",
            "assault",
            "malicious wounding",
            "armed robbery",
            "felonious assault",
            "sexual assault",
            "abduction",
            "bank robbery",
            "robbery by force",
            "sexual battery",
            "homicide",
            "assault by mob",
            "carjacking",
            "arson",
            "burglary of occupied dwelling"
    };

    // Server Side Parameters
    public static final String BACK_END_NUM_CLUSTERS_PARAM = "num_clusters";
    public static final String BACK_END_NOVA_DATA_PARAM = "nova_data";
    public static final String BACK_END_DC_DATA_PARAM = "dc_data";
    public static final String BACK_END_CRIME_TYPES_PARAM = "crimes_considered";
    public static final String BACK_END_NOVA_CRIME_TYPES_PARAM = "nova";
    public static final String BACK_END_DC_CRIME_TYPES_PARAM = "dc";
    public static final String BACK_END_CRIME_OUTLINES_PARAM = "outlines";
    public static final String BACK_END_CLUSTERING_CRIME_OUTLINES_PARAM = "area_outline";
    public static final String BACK_END_FAILURE_KEY = "failure";
}
