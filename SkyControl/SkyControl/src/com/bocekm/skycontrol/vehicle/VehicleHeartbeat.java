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

import android.os.Handler;

import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.enums.MAV_TYPE;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;

/**
 * The {@link VehicleHeartbeat} handles the heartbeat messages coming from vehicle.
 */
public class VehicleHeartbeat extends VehicleProperty {

    /**
     * The Enum HeartbeatState.
     */
    public enum HeartbeatState {
        /** No heartbeat received yet. */
        NO_HEARTBEAT_YET,
        /** The lost heartbeat. */
        LOST_HEARTBEAT,
        /** The normal heartbeat. */
        PERIODIC_HEARTBEAT
    }
    
    /** The periodic heartbeat timeout in ms. */
    private static final long PERIODIC_HEARTBEAT_TIMEOUT = 15000;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /** State of the heartbeat messaging. */
    private HeartbeatState mHeartbeatState = HeartbeatState.NO_HEARTBEAT_YET;

    /** System id of the vehicle which sent the heartbeat message. */
    private int mVehicleSysId = -1;

    /** Watchdog timer is implemented as a delayed Handler. */
    private final Handler mWatchdog = new Handler();

    /** Task to be run when watchdog timer times out. */
    private final Runnable mWatchdogCallback = new Runnable() {
        @Override
        public void run() {
            onHeartbeatTimeout();
        }
    };

    /**
     * Instantiates a new {@link VehicleHeartbeat} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehicleHeartbeat(VehicleEvents events) {
        mEvents = events;
    }

    /**
     * Send correct heartbeat event to the listeners and update watchdog timer.
     * 
     * @param msg heartbeat MavLink message
     */
    public void onHeartbeatReceived(msg_heartbeat msg) {
        // We don't care about the heartbeat from a GCS
        if (msg.type == MAV_TYPE.MAV_TYPE_GCS)
            return;

        if (mVehicleSysId != msg.sysid) {
            // Notify listeners when new vehicle gets connected.
            // Right now the app supports just one connected vehicle so when another vehicle
            // interferes a lot of things can get messed up.
            mVehicleSysId = msg.sysid;
            mEvents.onVehicleEvent(VehicleEvent.VEHICLE_CONNECTED);
        }
        
        // TODO Check MAV_STATE msg.system_status here
        switch (mHeartbeatState) {
            case LOST_HEARTBEAT:
                // Heartbeat was lost and is now received again, notify listener that it has been
                // restored
                mEvents.onVehicleEvent(VehicleEvent.HEARTBEAT_RESTORED);
                break;
            case PERIODIC_HEARTBEAT:
                // Notify listeners about receiving periodic heartbeat
                mEvents.onVehicleEvent(VehicleEvent.HEARTBEAT_PERIODIC);
                break;
            default:
                break;
        }

        mHeartbeatState = HeartbeatState.PERIODIC_HEARTBEAT;
        restartWatchdog(PERIODIC_HEARTBEAT_TIMEOUT);
    }

    /**
     * Callback run by watchdog when heartbeat hasn't been received for PERIODIC_HEARTBEAT_TIMEOUT.
     * Send notification to the listeners that heartbeat has timed out.
     */
    private void onHeartbeatTimeout() {
        mHeartbeatState = HeartbeatState.LOST_HEARTBEAT;
        mWatchdog.removeCallbacks(mWatchdogCallback);
        mEvents.onVehicleEvent(VehicleEvent.HEARTBEAT_TIMEOUT);
    }

    /**
     * Restart the watchdog timer - count again for time specified by timeout parameter. May be used
     * to start watchdog for first time.
     * 
     * @param timeout timeout in milliseconds
     */
    private void restartWatchdog(long timeout) {
        // re-start watchdog
        mWatchdog.removeCallbacks(mWatchdogCallback);
        mWatchdog.postDelayed(mWatchdogCallback, timeout);
    }

    public int getVehicleSysId() {
        return mVehicleSysId;
    }
    
    public HeartbeatState getHeartbeatState() {
        return mHeartbeatState;
    }

    @Override
    protected void setDefaultValues() {
        mWatchdog.removeCallbacks(mWatchdogCallback);
        mVehicleSysId = -1;
        mHeartbeatState = HeartbeatState.NO_HEARTBEAT_YET;
    }
}
