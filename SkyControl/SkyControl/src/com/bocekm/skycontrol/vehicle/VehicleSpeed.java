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
 * Keeps the information about speed of the connected vehicle. All values in meters per second.
 */
public class VehicleSpeed extends VehicleProperty implements
        OnSharedPreferenceChangeListener {

    /** The vertical speed. */
    private float mVerticalSpeed = 0;

    /** The groundspeed. */
    private float mGroundspeed = 0;

    /** The airspeed. */
    private float mAirspeed = 0;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /** Whether to return fake speed when asked. */
    private boolean mUseFakeGroundspeed;

    /** Fake groundspeed of the vehicle. */
    private float mFakeGroundspeed = 15;

    /**
     * Instantiates a new {@link VehicleSpeed} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehicleSpeed(VehicleEvents events) {
        mEvents = events;
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mUseFakeGroundspeed =
                userPref.getBoolean(PreferencesFragment.USE_FAKE_GNDSPEED_PREF_KEY, false);
        mFakeGroundspeed = userPref.getInt(PreferencesFragment.FAKE_GNDSPEED_PREF_KEY, 0);
    }

    public float getVerticalSpeed() {
        return mVerticalSpeed;
    }

    public float getGroundspeed() {
        if (mUseFakeGroundspeed) {
            return mFakeGroundspeed;
        }
        return mGroundspeed;
    }

    public float getAirspeed() {
        return mAirspeed;
    }

    /**
     * On new vehicle speed/climb rate received. Notifies {@link VehicleEvent} listeners.
     * 
     * @param msg VFR_HUD MavLink message
     */
    public void onSpeedReceived(msg_vfr_hud msg) {
        setGroundAndAirSpeeds(msg.groundspeed, msg.airspeed, msg.climb);
        mEvents.onVehicleEvent(VehicleEvent.SPEED_ALTITUDE);
    }

    /**
     * Sets the ground/air/vertical speeds.
     * 
     * @param groundspeed the ground speed
     * @param airspeed the air speed
     * @param climb the climb
     */
    public void setGroundAndAirSpeeds(float groundspeed, float airspeed, float climb) {
        this.mGroundspeed = groundspeed;
        this.mAirspeed = airspeed;
        this.mVerticalSpeed = climb;
    }

    @Override
    protected void setDefaultValues() {
        mVerticalSpeed = 0;
        mGroundspeed = 0;
        mAirspeed = 0;
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
            case PreferencesFragment.USE_FAKE_GNDSPEED_PREF_KEY:
                mUseFakeGroundspeed =
                        sharedPreferences.getBoolean(
                                PreferencesFragment.USE_FAKE_GNDSPEED_PREF_KEY, false);
                break;
            case PreferencesFragment.FAKE_GNDSPEED_PREF_KEY:
                mFakeGroundspeed =
                        sharedPreferences.getInt(PreferencesFragment.FAKE_GNDSPEED_PREF_KEY, 0);
                break;
            default:
                break;
        }
    }
}
