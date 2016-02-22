Crime on the Move Android Application

This is the Android app for the client-server application Crime on the Move.
It communicates with the Crime on the Move back end to grab crime data describing
crimes comitted in Northern Virginia and Washington D.C. The back end can be found here:
https://github.com/egaebel/crime-on-the-move--python-back-end

The Android app displays crime data as either GPS locations displayed on the
map or aggregated visualizations, with statistics and clustering options given.

The main use of this application is for users to be able to quickly interact
with data and "drill down" to find out specific information about areas of interest.
This is facilitated by providing a central Google Map object and then drawing 
various map overlays over counties using GPS points defining the borders which are
fetched from the server and then allowing
users to click to select counties and then run a clustering algorithm on the 
selected map overlay. This will produce several more map overlays representing
the clusters produced which can then further be clustered on via the same procedure.
This clustering-on-clusters aims to allow users to quickly perform high-level data
analysis.
