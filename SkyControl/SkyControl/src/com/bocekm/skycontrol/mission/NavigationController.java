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
package com.bocekm.skycontrol.mission;

import com.MAVLink.Messages.ardupilotmega.msg_nav_controller_output;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;

/**
 * Vehicle (its navigation controller) sends planned navigation action when navigating (e.g. in AUTO
 * flight mode). Such information is mainly for testing purpose, to check whether the autopilot is
 * behaving as expected.
 */
public class NavigationController {

    /** Difference between current altitude and next waypoint altitude in meters. */
    private float mCurrToTargAltDiff = 0.0f;

    /** Difference between current and desired airspeed in m/s. */
    private float mCurrToTargAsdpDiff = 0.0f;

    /** Distance to next waypoint in meters. */
    private short mDistToWaypoint = 0;

    /** Autopilot planned roll in degrees. */
    private float mNavRoll = 0.0f;

    /** Autopilot planned pitch in degrees. */
    private float mNavPitch = 0.0f;

    /** Autopilot planned heading in degrees. */
    private short mNavHeading = 0;

    /** Bearing to the next waypoint in degrees. */
    private short mTargetBearing = 0;

    /** Reference to a mission events handler. */
    private MissionEvents mEvents;

    /**
     * Instantiates a new {@link NavigationController} object.
     * 
     * @param events instance of the {@link NavigationController}
     */
    public NavigationController(MissionEvents events) {
        mEvents = events;
    }

    /**
     * Reads data from the incoming MavLink message.
     * 
     * @param msg the navigation controller MavLink message
     */
    public void onNavControlReceived(msg_nav_controller_output msg) {
        mCurrToTargAltDiff = msg.alt_error;
        mCurrToTargAsdpDiff = msg.aspd_error;
        mDistToWaypoint = msg.wp_dist;
        mNavRoll = msg.nav_roll;
        mNavPitch = msg.nav_pitch;
        mNavHeading = msg.nav_bearing;
        mTargetBearing = msg.target_bearing;
        mEvents.onMissionEvent(MissionEvent.NAV_CONTROLLER);
    }

    public float getCurrToTargAltDiff() {
        return mCurrToTargAltDiff;
    }

    public float getCurrToTargAsdpDiff() {
        return mCurrToTargAsdpDiff;
    }

    public short getDistToWaypoint() {
        return mDistToWaypoint;
    }

    public float getNavRoll() {
        return mNavRoll;
    }

    public float getNavPitch() {
        return mNavPitch;
    }

    public short getNavHeading() {
        return mNavHeading;
    }

    public short getTargetBearing() {
        return mTargetBearing;
    }
}
