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

import android.os.SystemClock;
import android.util.Log;

import com.MAVLink.Messages.ApmModes;
import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.ardupilotmega.msg_vfr_hud;
import com.MAVLink.Messages.enums.MAV_MODE_FLAG;
import com.MAVLink.Messages.enums.MAV_STATE;
import com.MAVLink.Messages.enums.MAV_TYPE;
import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;

/**
 * {@link VehicleState} class manages vehicle state information like whether the vehicle is ready to
 * fly (armed)/is in motion/which flight mode it has set/etc.
 */
public class VehicleState extends VehicleProperty {

    /** Indication whether the vehicle is in failsafe mode. */
    private boolean mIsFailsafe = false;

    /** Indication whether the vehicle is in armed mode. */
    private boolean mIsArmed = false;

    /** Indication whether the vehicle is currently flying. Based on speed > 1 m/s and armed state */
    private boolean mIsFlying = false;

    /** The autopilot flight mode. */
    private ApmModes mFlightMode = ApmModes.UNKNOWN;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /** The flight timer start time in ms. */
    private long mStartTime = 0;

    /** Elapsed flight time, since the vehicle started flying in ms. */
    private long mElapsedFlightTime = 0;

    /**
     * Instantiates a new {@link VehicleState} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehicleState(VehicleEvents events) {
        mEvents = events;
        resetFlightTimer();
    }

    /**
     * Notifies listeners if the vehicle started or stopped flying. And start/stop flight timer
     * accordingly.
     * 
     * @param isFlying indicate whether the vehicle is flying
     */
    private void setIsFlying(boolean isFlying) {
        if (isFlying != mIsFlying) {
            mIsFlying = isFlying;
            mEvents.onVehicleEvent(VehicleEvent.IS_FLYING);
            if (mIsFlying) {
                startTimer();
            } else {
                stopTimer();
            }
        }
    }

    /**
     * Updates the indication whether the vehicle is flying based on current groundspeed and
     * autopilot armed state.
     * 
     * @param msg {@link msg_vfr_hud} message containing information about groundspeed
     */
    public void onSpeedReceived(msg_vfr_hud msg) {
        setIsFlying(msg.groundspeed > 1.0 & mIsArmed);
    }

    /**
     * Notifies listeners if the new failsafe state differs from previous.
     * 
     * @param newFailsafe the new mIsFailsafe
     */
    private void setFailsafe(boolean newFailsafe) {
        if (this.mIsFailsafe != newFailsafe) {
            this.mIsFailsafe = newFailsafe;
            mEvents.onVehicleEvent(VehicleEvent.FAILSAFE);
        }
    }

    /**
     * Notifies listeners if the new armed state differs from previous.
     * 
     * @param newState the new mIsArmed
     */
    private void setArmed(boolean newState) {
        if (this.mIsArmed != newState) {
            this.mIsArmed = newState;
            mEvents.onVehicleEvent(VehicleEvent.ARMED);
        }
    }

    /**
     * Notifies listeners if the new flight mode differs from previous.
     * 
     * @param mode the new mode
     */
    private void setMode(ApmModes mode) {
        if (mFlightMode != mode) {
            mFlightMode = mode;
            mEvents.onVehicleEvent(VehicleEvent.FLIGHT_MODE);
        }
    }

    /**
     * Retrieves current state and flight mode of connected vehicle from heartbeat message.
     * 
     * @param msg heartbeat MavLink message
     */
    public void onStateReceived(msg_heartbeat msg) {
        // We don't care about the heartbeat from a GCS
        if (msg.type == MAV_TYPE.MAV_TYPE_GCS)
            return;
        
        // Retrieve flight mode saved in message custom_mode parameter
        if ((msg.base_mode & (byte) MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED) != 0) {
            setMode(ApmModes.getMode(msg.custom_mode, msg.type));
        } else {
            Log.d(SkyControlConst.DEBUG_TAG, "Custom mode is not enabled in Heartbeat message");
        }

        setArmed((msg.base_mode & (byte) MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) != 0);
        setFailsafe(msg.system_status == (byte) MAV_STATE.MAV_STATE_CRITICAL);
    }

    /**
     * Resets flight timer.
     */
    private void resetFlightTimer() {
        mElapsedFlightTime = 0;
        mStartTime = SystemClock.elapsedRealtime();
    }

    /**
     * Starts flight timer.
     */
    private void startTimer() {
        mStartTime = SystemClock.elapsedRealtime();
    }

    /**
     * Stop timer.
     */
    private void stopTimer() {
        // Calculate elapsed time of the last flight
        mElapsedFlightTime += SystemClock.elapsedRealtime() - mStartTime;
        mStartTime = SystemClock.elapsedRealtime();
    }

    /**
     * Returns the flight time in seconds.
     * 
     * @return the flight time
     */
    public long getFlightTime() {
        if (mIsFlying) {
            // Calculate delta time since last checked
            mElapsedFlightTime += SystemClock.elapsedRealtime() - mStartTime;
            mStartTime = SystemClock.elapsedRealtime();
        }
        return mElapsedFlightTime / 1000;
    }

    public boolean isFailsafe() {
        return mIsFailsafe;
    }

    public boolean isArmed() {
        return mIsArmed;
    }

    public boolean isFlying() {
        return mIsFlying;
    }

    public ApmModes getMode() {
        return mFlightMode;
    }

    @Override
    protected void setDefaultValues() {
        mIsFailsafe = false;
        mIsArmed = false;
        mIsFlying = false;
        mFlightMode = ApmModes.UNKNOWN;
        mStartTime = 0;
        mElapsedFlightTime = 0;
    }
}
