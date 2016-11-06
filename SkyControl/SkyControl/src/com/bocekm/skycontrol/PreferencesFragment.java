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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

// TODO: Auto-generated Javadoc
/**
 * The purpose of the {@link PreferencesFragment} class is to display application preferences to the
 * user.
 */
public class PreferencesFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    // Consult any change to these constant with preferences.xml resource file
    /** Ability to swipe between the action bar tabs. */
    public static final String SWIPE_ENABLED_PREF_KEY = "com.bocekm.skycontrol.swipe_enabled_pref";
    /** Keep screen on while the app is running. */
    public static final String SCREEN_ON_PREF_KEY = "com.bocekm.skycontrol.screen_on_pref";
    /** User preference enabling logging to file. */
    public static final String LOG_TO_FILE_PREF_KEY = "com.bocekm.skycontrol.log_to_file_pref";
    /** Default altitude to be assigned to waypoint when creating a new one. */
    public static final String DEFAULT_WAYPOINT_ALT_PREF_KEY =
            "com.bocekm.skycontrol.default_waypoint_altitude_pref";
    /** Fake vehicle position is provided to app when vehicle gets connected. */
    public static final String USE_FAKE_POS_PREF_KEY = "com.bocekm.skycontrol.use_fake_pos_pref";
    /** Enables overriding actual vehicle groundspeed with fake value. */
    public static final String USE_FAKE_GNDSPEED_PREF_KEY =
            "com.bocekm.skycontrol.use_fake_gndspeed_pref";
    /** Sets groundspeed to be substituted for the original vehicle altitude, in meters. */
    public static final String FAKE_GNDSPEED_PREF_KEY =
            "com.bocekm.skycontrol.fake_groundspeed_pref";
    /** Enables overriding actual vehicle altitude with fake value. */
    public static final String USE_FAKE_ALTITUDE_PREF_KEY =
            "com.bocekm.skycontrol.use_fake_altitude_pref";
    /** Sets altitude to be substituted for the original vehicle altitude, in meters. */
    public static final String FAKE_ALTITUDE_PREF_KEY = "com.bocekm.skycontrol.fake_altitude_pref";
    /** User defined directory to be used for saving files accessible to user. */
    public static final String FILE_DIRECTORY_PREF_KEY =
            "com.bocekm.skycontrol.file_directory_pref";
    /** Enables downloading current mission of the autopilot on its first connection. */
    public static final String DWNLD_MISSION_ON_CONN_PREF_KEY =
            "com.bocekm.skycontrol.dwnld_mission_on_conn_pref";
    /** Enables the collision avoidance system. */
    public static final String CAS_ENABLED_PREF_KEY = "com.bocekm.skycontrol.cas_enabled_pref";
    /** Sets the distance from vehicle on which collision is being checked, in seconds. */
    public static final String CAS_DISTANCE_PREF_KEY =
            "com.bocekm.skycontrol.collision_distance_pref";
    /** Rate of receiving the data like position, speed, etc. from vehicle in Hz. */
    public static final String DATA_STREAM_RATE_PREF_KEY =
            "com.bocekm.skycontrol.data_stream_rate_pref";
    /** Make the default waypoint altitude above ground instead of AMSL. */
    public static final String DEFAULT_ALT_ABOVE_GND_PREF_KEY =
            "com.bocekm.skycontrol.default_alt_above_ground_pref";

    /** Array of preferences which need update of their summary on startup. */
    private static final List<String> sPrefsWithSummary = new ArrayList<String>();

    /*
     * (non-Javadoc)
     * 
     * @see android.preference.PreferenceFragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * FYI default sharedPreferences file location:
         * /data/data/com.your.package/shared_prefs/com.your.package_preferences.xml
         */

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Get user shared preferences
        SharedPreferences userPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Enable or disable keeping the screen on while the app is running
        keepScreenOn(userPref, getActivity());

        // Set summary for preferences which need that
        sPrefsWithSummary.add(FILE_DIRECTORY_PREF_KEY);
        sPrefsWithSummary.add(DEFAULT_WAYPOINT_ALT_PREF_KEY);
        sPrefsWithSummary.add(CAS_DISTANCE_PREF_KEY);
        sPrefsWithSummary.add(FAKE_ALTITUDE_PREF_KEY);
        sPrefsWithSummary.add(FAKE_GNDSPEED_PREF_KEY);
        sPrefsWithSummary.add(DATA_STREAM_RATE_PREF_KEY);
        for (String key : sPrefsWithSummary)
            updateSummary(userPref, key);
    }

    /**
     * Updates summary text of the preference.
     * 
     * @param userPref user preferences
     * @param key key of the preference
     */
    private void updateSummary(SharedPreferences userPref, String key) {
        if (!sPrefsWithSummary.contains(key))
            return;
        String summary;
        switch (key) {
            case FAKE_GNDSPEED_PREF_KEY:
                summary = userPref.getInt(key, 0) + " m/s";
                break;
            case DATA_STREAM_RATE_PREF_KEY:
                summary = userPref.getInt(key, 0) + " Hz";
                break;
            case CAS_DISTANCE_PREF_KEY:
                summary = userPref.getInt(key, 0) + " s";
                break;
            case FAKE_ALTITUDE_PREF_KEY:
                summary = userPref.getInt(key, 0) + " m";
                break;
            case DEFAULT_WAYPOINT_ALT_PREF_KEY:
                summary = userPref.getInt(key, 0) + " m";
                if (userPref.getBoolean(CAS_ENABLED_PREF_KEY, true)
                        && userPref.getBoolean(DEFAULT_ALT_ABOVE_GND_PREF_KEY, true))
                    summary += " AGL";
                else
                    summary += " AMSL";
                break;
            default:
                summary = userPref.getString(key, "");
                break;
        }
        Preference pref = findPreference(key);
        // Set summary to be the user-description for the selected value
        pref.setSummary(summary);
    }

    /*
     * (non-Javadoc) Handles any application settings change.
     * 
     * @see
     * android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged
     * (android.content.SharedPreferences, java.lang.String)
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(SkyControlConst.DEBUG_TAG, "Preference changed: " + key);
        updateSummary(sharedPreferences, key);
        switch (key) {
            case CAS_ENABLED_PREF_KEY:
            case DEFAULT_ALT_ABOVE_GND_PREF_KEY:
                updateSummary(sharedPreferences, DEFAULT_WAYPOINT_ALT_PREF_KEY);
                break;
            case SCREEN_ON_PREF_KEY:
                // Enable or disable keeping the screen on while the app is running
                keepScreenOn(sharedPreferences, getActivity());
                break;
            default:
                break;
        }
    }

    /**
     * Enables or disables keeping the device's screen on while the app is running.
     * 
     * @param userPref user preferences
     * @param activity the activity context
     */
    public static void keepScreenOn(SharedPreferences userPref, Activity activity) {
        if (userPref.getBoolean(PreferencesFragment.SCREEN_ON_PREF_KEY, false)) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        // Register application settings onChange listener
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();
        // Remove application settings onChange listener
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }
}
