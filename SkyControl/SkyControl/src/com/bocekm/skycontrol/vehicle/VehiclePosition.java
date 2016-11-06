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

import com.MAVLink.Messages.ardupilotmega.msg_global_position_int;
import com.MAVLink.Messages.ardupilotmega.msg_gps_raw_int;
import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.mission.FlightDirector.FdState;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionItemList;
import com.bocekm.skycontrol.mission.WaypointFollower.WpfState;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.google.android.gms.maps.model.LatLng;

/**
 * Keeps the information about the global position of the connected vehicle and GPS parameters.
 */
public class VehiclePosition extends VehicleProperty implements
        OnSharedPreferenceChangeListener {
    /**
     * Types of GPS fix.
     */
    public enum FixType {
        /** No fix. */
        NO_FIX,
        /** 2D fix. */
        TWO_DIM_FIX,
        /** 3D fix. */
        THREE_DIM_FIX;
    }

    /** Number of visible satellites. */
    private int mSatCount = -1;

    /** One of the values: no fix/2D fix/3D fix. */
    private FixType mFixType = FixType.NO_FIX;

    /** The global position of the vehicle. */
    private LatLng mPosition = null;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /** Whether to return fake position when asked for vehicle GPS position. */
    private boolean mUseFakeGps;

    /** Fake GPS position of vehicle based on last received device location. */
    private LatLng mFakePosition = null;

    /**
     * Radius for which proximity to mission waypoints will be checked when fake position is
     * enabled.
     */
    private static final int FAKE_WP_RADIUS = 30;

    /**
     * Instantiates a new {@link VehiclePosition} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehiclePosition(VehicleEvents events) {
        mEvents = events;
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mUseFakeGps = userPref.getBoolean(PreferencesFragment.USE_FAKE_POS_PREF_KEY, false);
    }

    /**
     * Checks validity of global position of the vehicle.
     * 
     * @return true, if is the global position valid
     */
    private boolean isPositionValid() {
        return (mPosition != null);
    }

    /**
     * Sets the usage of fake vehicle position for debugging purposes.
     * 
     * @param useFakeGps true if fake position shall be used
     */
    public void useFakeGps(boolean useFakeGps) {
        mUseFakeGps = useFakeGps;
    }


    /**
     * Checks whether fake vehicle position is used instead of position received from vehicle GPS.
     * 
     * @return true, if usage of fake vehicle position is enabled
     */
    public boolean isFakeGpsEnabled() {
        return mUseFakeGps;
    }

    /**
     * Returns the global position of the vehicle.
     * 
     * @return the global position. If no valid position is available, (0,0) position is returned.
     */
    public LatLng getPosition() {
        if (mUseFakeGps)
            return mFakePosition;
        if (isPositionValid()) {
            return mPosition;
        } else {
            return new LatLng(0, 0);
        }
    }

    /**
     * Saves new global position. Listeners to {@link VehicleEvent} are then notified that new
     * position has been received.
     * 
     * @param msg global position MavLink message
     */
    public void onPositionReceived(msg_global_position_int msg) {
        setPosition(new LatLng(msg.lat / 1E7, msg.lon / 1E7));
    }

    /**
     * On new GPS state received. Save the GPS state parameters to local variables. Listeners to
     * {@link VehicleEvent} are then notified if these changed.
     * 
     * @param msg GPS MavLink message
     */
    public void onGpsStateReceived(msg_gps_raw_int msg) {
        setGpsState(msg.fix_type, msg.satellites_visible);
    }

    /**
     * Gets number of visible satellites.
     * 
     * @return number of visible satellites. Returns -1 if no state of GPS received from vehicle
     *         yet.
     */
    public int getSatCount() {
        return mSatCount;
    }

    /**
     * Gets GPS fix type.
     * 
     * @return GPS fix type, one of the {@link FixType} enum items
     */
    public FixType getFixType() {
        return mFixType;
    }

    /**
     * Sets the new state of GPS. Notify listeners when number of visible satellites or GPS fixed
     * changed from previous.
     * 
     * @param mavLinkFixType type of the GPS fix, value got from MavLink message
     * @param satellitesVisible number of visible satellites
     */
    public void setGpsState(int mavLinkFixType, int satellitesVisible) {
        if (mSatCount != satellitesVisible) {
            mSatCount = satellitesVisible;
            mEvents.onVehicleEvent(VehicleEvent.GPS_COUNT);
        }
        FixType newFixType;
        switch (mavLinkFixType) {
            case 2:
                newFixType = FixType.TWO_DIM_FIX;
                break;
            case 3:
                newFixType = FixType.THREE_DIM_FIX;
                break;
            default:
                newFixType = FixType.NO_FIX;
                break;
        }
        if (mFixType != newFixType) {
            mFixType = newFixType;
            mEvents.onVehicleEvent(VehicleEvent.GPS_FIX);
        }
    }

    private void setPosition(LatLng position) {
        if (mUseFakeGps) {
            if (mFakePosition == null)
                // Wait until the fake position gets set to the device location
                return;
            // Base new fake position on groundspeed and heading. The distance to be
            // incremented is based on the frequency of sending position by vehicle.
            float heading = Vehicle.get().getAttitude().getYawInDegrees();
            float speed = Vehicle.get().getSpeed().getGroundspeed();
            float distance = speed / Vehicle.get().getStreamRates().getStreamRate();
            mFakePosition = SkyControlUtils.getDestination(mFakePosition, heading, distance);
            checkFdMissionRadius();
            checkWpfMissionRadius();
            mEvents.onVehicleEvent(VehicleEvent.POSITION);
        } else if (this.mPosition != position) {
            this.mPosition = position;
            mEvents.onVehicleEvent(VehicleEvent.POSITION);
        }
    }

    /**
     * Checks whether the fake vehicle position is in close proximity (within the specified radius)
     * of any of the waypoint follower mission items. If it is, event about new current wp is sent.
     */
    private void checkWpfMissionRadius() {
        if (Mission.get().getWaypointFollower().getWpfState() != WpfState.WPF_ENGAGED)
            return;
        for (NavMissionItem waypoint : Mission.get().getNavMissionItems()) {
            if (SkyControlUtils.getDistance(mFakePosition, waypoint.getPosition()) < FAKE_WP_RADIUS) {
                // The waypoint got reached so the fake current WP should be set to the following
                // waypoint
                Mission.get().setCurrentWpIndex(waypoint.getIndexInAutopilotMission() + 1);
                return;
            }
        }
    }

    /**
     * Checks whether the fake vehicle position is in close proximity (within the specified radius)
     * of any of the flight director mission items. If it is, event about new current wp is sent.
     */
    private void checkFdMissionRadius() {
        if (Mission.get().getFlightDirector().getFdState() != FdState.FD_ENGAGED)
            return;
        MissionItemList fdMission = Mission.get().getFlightDirector().getFdMission();
        for (NavMissionItem waypoint : fdMission.getNavMissionItems()) {
            if (SkyControlUtils.getDistance(mFakePosition, waypoint.getPosition()) < FAKE_WP_RADIUS) {
                // The waypoint got reached so the fake current WP should be set to the following
                // waypoint
                Mission.get().setCurrentWpIndex(waypoint.getIndexInAutopilotMission() + 1);
                return;
            }
        }
    }

    @Override
    protected void setDefaultValues() {
        mSatCount = -1;
        mFixType = FixType.NO_FIX;
        mPosition = null;
        mFakePosition = new LatLng(49.206692, 16.600446);
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
            case PreferencesFragment.USE_FAKE_POS_PREF_KEY:
                mUseFakeGps =
                        sharedPreferences.getBoolean(PreferencesFragment.USE_FAKE_POS_PREF_KEY,
                                false);
                break;
            default:
                break;
        }
    }

    public LatLng getFakePosition() {
        return mFakePosition;
    }

    public void setFakePosition(LatLng fakePosition) {
        mFakePosition = fakePosition;
    }
}
