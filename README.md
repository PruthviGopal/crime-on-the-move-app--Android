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



Licensing:

The MIT License (MIT)
Copyright (c) 2016 Ethan Gaebel
 
Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial 
portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING 
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

