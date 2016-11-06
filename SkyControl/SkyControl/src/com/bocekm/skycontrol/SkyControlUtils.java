/*
 * Copyright (c) 2014, Michal Bocek, All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bocekm.skycontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.bocekm.skycontrol.file.FileUtil;
import com.google.android.gms.maps.model.LatLng;

/**
 * Provides set of utility methods used across application.
 */
public class SkyControlUtils {

    /** Reference to showed Toast message to avoid piling up the Toasts */
    private static Toast sToast;

    /**
     * Truncate angle to <min, max> scale and multiply it by constant if needed.
     * 
     * @param angle the angle to be adjusted in degrees
     * @param minMaxDeg scale to which the angle is to be adjusted, <-minMaxDeg, minMaxDeg>
     * @param multiplier constant by which the angle will be multiplied
     * @return the adjusted angle
     */
    public static int adjustAngle(float angle, int minMaxDeg, int multiplier) {
        if (angle > minMaxDeg)
            angle = minMaxDeg;
        if (angle < -minMaxDeg)
            angle = -minMaxDeg;
        Float multiplied = angle * 10;
        return multiplied.intValue();
    }

    /**
     * Converts density-independent pixels to pixels.
     * 
     * @param dp density-independent pixels
     * @return pixels
     */
    public static int dpToPixel(int dp) {
        Resources r = SkyControlApp.getAppContext().getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                r.getDisplayMetrics());
    }

    /**
     * Toasts text. Avoids piling up of the toast messages - if there is any previous toast message
     * visible it is canceled. Logs the message also so user can read it when didn't catch the
     * toast.
     * 
     * @param text the text to be toasted
     * @param duration duration of the toast, use {@link Toast#LENGTH_LONG} or
     *        {@link Toast#LENGTH_SHORT}
     */
    public static void toast(String text, int duration) {
        // Cancel previous Toast which is still showing to avoid piling up the Toasts
        if (sToast != null)
            sToast.cancel();
        sToast = Toast.makeText(SkyControlApp.getAppContext(), text, duration);
        TextView v = (TextView) sToast.getView().findViewById(android.R.id.message);;
        if (v != null)
            // Make the message centered. Noticeable when the message is long enough to be wrapped
            // to new line.
            v.setGravity(Gravity.CENTER);
        sToast.show();

        // Log the toast message in case user didn't catch the toast
        if (text.charAt(text.length() - 1) != '\n')
            text += "\n";
        log(text, true);
    }

    /**
     * Logs text to log file and to {@link LogFragment} log tab window. It may happen that
     * {@link LogFragment} is not instantiated yet. In that case the string is saved to
     * {@link SharedPreferences} and it will be written to {@link LogFragment#mLogTextView} and log
     * file once the Fragment gets instantiated.
     * 
     * @param text text to be logged
     * @param toWindow indicate whether to log the text to log tab window
     */
    public static void log(String text, boolean toWindow) {
        MainActivity activity = SkyControlApp.getMainActivity();
        if (activity == null) {
            Log.e(SkyControlConst.ERROR_TAG, "\"" + text + "\" couldn't be logged");
            return;
        }
        Fragment fragment = activity.getPageFragmentOnPosition(TabsPagerAdapter.LOG_TAB);
        // If the Log fragment is already instantiated, append the text to log directly bypassing
        // the use of shared preferences
        String textToFile = "[" + FileUtil.getTimeStamp() + "] " + text;
        if (fragment != null) {
            if (((LogFragment) fragment).isLogToFileEnabled())
                ((LogFragment) fragment).appendToLogFile(textToFile);
            if (toWindow)
                ((LogFragment) fragment).appendToLogWindow(text);
        } else {
            // Open Shared Preferences
            SharedPreferences prefs =
                    activity.getSharedPreferences(SkyControlConst.APP_PREFERENCES,
                            Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            // Get any text that may be saved in shared preferences already and append the new text
            // to it.
            String previous =
                    prefs.getString(LogFragment.APPEND_TO_LOG_FILE_PREF_KEY,
                            SkyControlConst.EMPTY_STRING);
            StringBuilder s = new StringBuilder(previous);
            s.append(textToFile);
            editor.putString(LogFragment.APPEND_TO_LOG_FILE_PREF_KEY, s.toString());
            if (toWindow) {
                previous =
                        prefs.getString(LogFragment.APPEND_TO_LOG_WINDOW_PREF_KEY,
                                SkyControlConst.EMPTY_STRING);
                s = new StringBuilder(previous);
                s.append(text);
                editor.putString(LogFragment.APPEND_TO_LOG_WINDOW_PREF_KEY, s.toString());
            }
            editor.commit();
        }
    }

    /**
     * Converts degrees to radians.
     * 
     * @param angle angle in degrees
     * @return angle in radians
     */
    public static double degToRad(double angle) {
        return angle * Math.PI / 180;
    }

    /**
     * Converts radians to degrees.
     * 
     * @param radians angle in radians
     * @return angle in degrees
     */
    public static double radToDeg(double radians) {
        return radians * 180 / Math.PI;
    }

    /**
     * Calculates destination point given start point, bearing & distance, using Vincenty inverse
     * formula for ellipsoids.
     * 
     * <a href=
     * "http://stackoverflow.com/questions/2637023/how-to-calculate-the-latlng-of-a-point-a-certain-distance-away-from-another"
     * >Java implementation source</a>
     * 
     * Vincenty Direct Solution of Geodesics on the Ellipsoid (c) Chris Veness 2005-2012
     * 
     * from: Vincenty direct formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the
     * Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975 <a
     * href=
     * "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf">http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
     * </a>
     * 
     * @param initialPosition start point position
     * @param brng initial bearing in decimal degrees
     * @param dist distance to computed destination along bearing in meters
     * @return destination point coordinates with 5.10<sup>-4</sup> m precision
     */
    public static LatLng getDestination(LatLng initialPosition, double brng, double dist) {
        // WGS-84 ellipsoid
        double a = 6378137, b = 6356752.3142, f = 1 / 298.257223563;
        double s = dist;
        double alpha1 = SkyControlUtils.degToRad(brng);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);
        double lat1 = initialPosition.latitude;
        double lon1 = initialPosition.longitude;

        double tanU1 = (1 - f) * Math.tan(SkyControlUtils.degToRad(lat1));
        double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1)), sinU1 = tanU1 * cosU1;
        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double sinSigma = 0, cosSigma = 0, deltaSigma = 0, cos2SigmaM = 0;
        double sigma = s / (b * A), sigmaP = 2 * Math.PI;

        while (Math.abs(sigma - sigmaP) > 1e-12) {
            cos2SigmaM = Math.cos(2 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            deltaSigma =
                    B
                            * sinSigma
                            * (cos2SigmaM + B
                                    / 4
                                    * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6
                                            * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma)
                                            * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
            sigmaP = sigma;
            sigma = s / (b * A) + deltaSigma;
        }

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double lat2 =
                Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
                        (1 - f) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda =
                Math.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
        double L =
                lambda
                        - (1 - C)
                        * f
                        * sinAlpha
                        * (sigma + C * sinSigma
                                * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        // Normalize to -180...+180
        double lon2 = (SkyControlUtils.degToRad(lon1) + L + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        // final bearing, if required
        // double revAz = Math.atan2(sinAlpha, -tmp);

        LatLng newPosition =
                new LatLng(SkyControlUtils.radToDeg(lat2), SkyControlUtils.radToDeg(lon2));
        return newPosition;
    }

    /**
     * Calculates orthodromic distance between two points specified by latitude/longitude using
     * Vincenty inverse formula for ellipsoids.
     * 
     * @param firstPosition the first position
     * @param secondPosition the second position
     * @return distance in meters between points with 5.10<sup>-4</sup> m precision
     * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted
     *      here</a>
     */
    public static double getDistance(LatLng firstPosition, LatLng secondPosition) {
        double lat1 = firstPosition.latitude;
        double lon1 = firstPosition.longitude;
        double lat2 = secondPosition.latitude;
        double lon2 = secondPosition.longitude;
        return getDistanceOrAzimuth(lat1, lon1, lat2, lon2, false);
    }

    /**
     * Calculates orthodromic distance between two points specified by latitude/longitude using
     * Vincenty inverse formula for ellipsoids.
     * 
     * @param lat1 first position latitude
     * @param lon1 first position longitude
     * @param lat2 second position latitude
     * @param lon2 second position longitude
     * @return distance in meters between points with 5.10<sup>-4</sup> m precision
     * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted
     *      here</a>
     */
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return getDistanceOrAzimuth(lat1, lon1, lat2, lon2, false);
    }

    /**
     * Calculates azimuth from first position towards second position, both positions specified by
     * latitude/longitude, using Vincenty inverse formula for ellipsoids.
     * 
     * @param lat1 first position latitude
     * @param lon1 first position longitude
     * @param lat2 second position latitude
     * @param lon2 second position longitude
     * @return azimuth from first position towards second position in degrees
     * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted
     *      here</a>
     */
    public static double getAzimuth(double lat1, double lon1, double lat2, double lon2) {
        return getDistanceOrAzimuth(lat1, lon1, lat2, lon2, true);
    }

    /**
     * Calculates geodetic distance or bearing between two points specified by latitude/longitude
     * using Vincenty inverse formula for ellipsoids.
     * 
     * http://stackoverflow.com/questions/120283/how-can-i-measure-distance-and-create-a-bounding-
     * box-based-on-two-latitudelongi
     * 
     * @param lat1 first position latitude
     * @param lon1 first position longitude
     * @param lat2 second position latitude
     * @param lon2 second position longitude
     * @param getAzimuth true to return azimuth instead of distance, false to return distance
     * @return distance or azimuth depending on getAzimuth param; distance between points is in
     *         meters with 5.10<sup>-4</sup> m precision ; azimuth from first position towards
     *         second position is in degrees
     * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted
     *      here</a>
     */
    private static double getDistanceOrAzimuth(double lat1, double lon1, double lat2, double lon2,
            boolean getAzimuth) {
        double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
        double lambda = L, lambdaP, iterLimit = 100;
        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma =
                    Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
                            + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
                            * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0)
                return 0; // co-incident points
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            if (Double.isNaN(cos2SigmaM))
                cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda =
                    L
                            + (1 - C)
                            * f
                            * sinAlpha
                            * (sigma + C
                                    * sinSigma
                                    * (cos2SigmaM + C * cosSigma
                                            * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

        if (iterLimit == 0)
            return Double.NaN; // formula failed to converge

        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma =
                B
                        * sinSigma
                        * (cos2SigmaM + B
                                / 4
                                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6
                                        * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma)
                                        * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double dist = b * A * (sigma - deltaSigma);
        double fwdAz =
                SkyControlUtils.radToDeg(Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1
                        * cosU2 * cosLambda));
        if (fwdAz > 360.0)
            fwdAz -= 360.0;
        if (fwdAz < 0.0)
            fwdAz += 360.0;

        return getAzimuth ? fwdAz : dist;
    }

    /**
     * Converts LatLng object to String in format "Lat, Lng".
     * 
     * @param location a Location object containing the current location
     * @return string in format "latitude, longitude", or empty string if no location is available
     */
    public static String locationToString(LatLng location) {
        // If the location is valid
        if (location != null) {
            // Return the latitude and longitude as string
            return SkyControlApp.getAppContext().getString(R.string.latitude_longitude,
                    location.latitude, location.longitude);
        } else {
            // Otherwise, return the empty string
            return SkyControlConst.EMPTY_STRING;
        }
    }
}
