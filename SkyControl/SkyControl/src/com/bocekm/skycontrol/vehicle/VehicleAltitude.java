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
package com.bocekm.skycontrol.vehicle;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.MAVLink.Messages.ardupilotmega.msg_vfr_hud;
import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;

/**
 * Keeps the information about the connected vehicle altitude in meters.
 */
public class VehicleAltitude extends VehicleProperty implements
        OnSharedPreferenceChangeListener {

    /** The Altitude. */
    private float mAltitude = 0.0f;

    /** Whether to return fake altitude. */
    private boolean mUseFakeAltitude;

    /** Fake altitude of vehicle set by user. */
    private float mFakeAltitude;

    public VehicleAltitude() {
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mUseFakeAltitude =
                userPref.getBoolean(PreferencesFragment.USE_FAKE_ALTITUDE_PREF_KEY, false);
        mFakeAltitude = userPref.getInt(PreferencesFragment.FAKE_ALTITUDE_PREF_KEY, 0);
    }

    public float getAltitude() {
        if (mUseFakeAltitude)
            return mFakeAltitude;
        return mAltitude;
    }

    /**
     * Saves the new altitude.
     * 
     * @param msg VFR_HUD MavLink message
     */
    public void onAltitudeReceived(msg_vfr_hud msg) {
        setAltitude(msg.alt);
    }

    private void setAltitude(float altitude) {
        this.mAltitude = altitude;
    }

    @Override
    protected void setDefaultValues() {
        mAltitude = 0.0f;
    }

    /*
     * (non-Javadoc) Handles changes made upon the user settings.
     * 
     * @see
     * android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged
     * (android.content.SharedPreferences, java.lang.String)
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesFragment.USE_FAKE_ALTITUDE_PREF_KEY:
                mUseFakeAltitude =
                        sharedPreferences.getBoolean(
                                PreferencesFragment.USE_FAKE_ALTITUDE_PREF_KEY, false);
                break;
            case PreferencesFragment.FAKE_ALTITUDE_PREF_KEY:
                mFakeAltitude =
                        sharedPreferences.getInt(PreferencesFragment.FAKE_ALTITUDE_PREF_KEY, 0);
                // Notify listeners about new altitude, though fake
                Vehicle.get().getEvents().onVehicleEvent(VehicleEvent.SPEED_ALTITUDE);
                break;
            default:
                break;
        }
    }
}
