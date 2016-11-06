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

import com.MAVLink.Messages.ardupilotmega.msg_attitude;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;

/**
 * Keeps the information about the orientation of an vehicle's axes. All values are in radians.
 */
public class VehicleAttitude extends VehicleProperty {

    /** The roll in radians. */
    private float mRoll = 0.0f;

    /** The pitch in radians. */
    private float mPitch = 0.0f;

    /** The yaw in radians. */
    private float mYaw = 0.0f;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /**
     * Instantiates a new {@link VehicleAttitude} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehicleAttitude(VehicleEvents events) {
        mEvents = events;
    }

    /**
     * On attitude MavLink message received. Sets attitude parameters to the local variables.
     * 
     * @param msg attitude MavLink message
     */
    public void onAttitudeReceived(msg_attitude msg) {
        setRollPitchYaw(msg.roll, msg.pitch, msg.yaw);
    }

    /**
     * Sets the roll, the pitch, the yaw and notifies listeners about change to these values.
     * 
     * @param roll the roll in radians
     * @param pitch the pitch in radians
     * @param yaw the yaw in radians
     */
    private void setRollPitchYaw(float roll, float pitch, float yaw) {
        mRoll = roll;
        mPitch = pitch;
        mYaw = yaw;
        mEvents.onVehicleEvent(VehicleEvent.ATTITUDE);
    }

    public float getRoll() {
        return mRoll;
    }

    public float getPitch() {
        return mPitch;
    }

    public float getYaw() {
        return mYaw;
    }

    public float getRollInDegrees() {
        return (float) SkyControlUtils.radToDeg(mRoll);
    }

    public float getPitchInDegrees() {
        return (float) SkyControlUtils.radToDeg(mPitch);
    }

    /**
     * Gets the yaw in degrees.
     * 
     * @return the yaw in degrees, range <0, 360>
     */
    public float getYawInDegrees() {
        float yawInDeg = (float) SkyControlUtils.radToDeg(mYaw);
        // Range is now <-360,360> .. convert it to <0, 360>
        if (yawInDeg < 0.0)
            yawInDeg = 360 + yawInDeg;
        return yawInDeg;
    }

    @Override
    protected void setDefaultValues() {
        mRoll = 0.0f;
        mPitch = 0.0f;
        mYaw = 0.0f;
    }
}
