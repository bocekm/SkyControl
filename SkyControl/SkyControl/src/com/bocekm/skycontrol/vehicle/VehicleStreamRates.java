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

import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.mavlink.MavLinkStreamRates;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;

/**
 * {@link VehicleStreamRates} class requests data streams to be sent by vehicle when first heartbeat
 * is received.
 */
public class VehicleStreamRates implements
        VehicleListener,
        OnSharedPreferenceChangeListener {

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    private int mStreamRate;

    /**
     * Instantiates a new {@link VehicleStreamRates} object.
     * 
     * @param events the {@link VehicleEvents} instance
     */
    public VehicleStreamRates(VehicleEvents events) {
        mEvents = events;
        mEvents.addVehicleListener(this);
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mStreamRate = userPref.getInt(PreferencesFragment.DATA_STREAM_RATE_PREF_KEY, 1);
    }

    /*
     * (non-Javadoc) Request data streams on first received heartbeat.
     * 
     * @see
     * com.bocekm.skycontrol.vehicle.VehicleInterfaces.OnVehicleListener#onVehicleEvent(com.bocekm
     * .skycontrol.vehicle.VehicleEvents.VehicleEvent)
     */
    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case VEHICLE_CONNECTED:
            case HEARTBEAT_RESTORED:
                setupStreamRates();
                break;
            default:
                break;
        }
    }

    /**
     * Request data streams to be sent by vehicle back to the app, with specific frequency of each
     * data stream.
     */
    public void setupStreamRates() {
        // RAW_SENS:
        // - RAW_IMU
        // - SCALED_IMU2
        // - SCALED_PRESSURE
        // - SENSOR_OFFSETS
        int rawSensors = mStreamRate;
        // EXT_STAT:
        // - SYS_STATUS
        // - MEMINFO
        // - MISSION_CURRENT
        // - GPS_RAW_INT
        // - NAV_CONTROLLER_OUTPUT
        // - LIMITS_STATUS
        int extendedStatus = mStreamRate;
        // POSITION:
        // - GLOBAL_POSITION_INT
        int position = mStreamRate;
        // EXTRA1:
        // - ATTITUDE
        // - SIMSTATE (SITL)
        int extra1 = mStreamRate;
        // EXTRA2:
        // - SPEED
        int extra2 = mStreamRate;
        // EXTRA3:
        // - AHRS
        // - HWSTATUS
        // - SYSTEM_TIME
        int extra3 = mStreamRate;

        MavLinkStreamRates.setupStreamRates(extendedStatus, extra1, extra2, extra3, position,
                rawSensors);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesFragment.DATA_STREAM_RATE_PREF_KEY:
                mStreamRate =
                        sharedPreferences.getInt(PreferencesFragment.DATA_STREAM_RATE_PREF_KEY, 1);
                setupStreamRates();
                break;
            default:
                break;
        }
    }
    
    public int getStreamRate() {
        return mStreamRate;
    }
}
