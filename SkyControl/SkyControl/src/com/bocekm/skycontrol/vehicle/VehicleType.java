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

import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.enums.MAV_TYPE;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;

/**
 * Keeps the information about the type of the connected vehicle.
 */
public class VehicleType extends VehicleProperty {

    /** The type of vehicle. Default is MAV_TYPE_GENERIC. */
    private int mType = MAV_TYPE.MAV_TYPE_GENERIC;

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /**
     * Types of vehicle onboard software.
     */
    public enum VehicleTypeEnum {
        /** ArduPlane. */
        ARDUPLANE,
        /** ArduCopter. */
        ARDUCOPTER,
        /** ArduRover. */
        ARDUROVER;
    }

    /**
     * Instantiates a new {@link VehicleType} object.
     * 
     * @param events instance of the {@link VehicleEvents}
     */
    public VehicleType(VehicleEvents events) {
        mEvents = events;
    }

    public int getType() {
        return mType;
    }

    /**
     * Returns type of the connected vehicle in form of one of the possible vehicle software -
     * Ardu[Plane/Copter/Rover].
     * 
     * @return one of the {@link VehicleTypeEnum} enum items. Returns VehicleTypeEnum.ARDUPLANE when
     *         no vehicle is connected or vehicle connected is unrecognized.
     */
    public VehicleTypeEnum getSimplifiedType() {
        VehicleTypeEnum simpleType;
        if (Connection.get().getMavLinkClient().isServiceBound()) {
            switch (getType()) {
                case MAV_TYPE.MAV_TYPE_FIXED_WING:
                    simpleType = VehicleTypeEnum.ARDUPLANE;
                    break;
                case MAV_TYPE.MAV_TYPE_GENERIC:
                case MAV_TYPE.MAV_TYPE_QUADROTOR:
                case MAV_TYPE.MAV_TYPE_COAXIAL:
                case MAV_TYPE.MAV_TYPE_HELICOPTER:
                case MAV_TYPE.MAV_TYPE_HEXAROTOR:
                case MAV_TYPE.MAV_TYPE_OCTOROTOR:
                case MAV_TYPE.MAV_TYPE_TRICOPTER:
                    simpleType = VehicleTypeEnum.ARDUCOPTER;
                    break;
                case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                case MAV_TYPE.MAV_TYPE_SURFACE_BOAT:
                    simpleType = VehicleTypeEnum.ARDUROVER;
                    break;
                default:
                    // unrecognized - base default behavior on FIXED_WING
                    simpleType = VehicleTypeEnum.ARDUPLANE;
                    break;
            }
        } else {
            // offline - base default behavior on FIXED_WING
            simpleType = VehicleTypeEnum.ARDUPLANE;
        }
        return simpleType;
    }

    /**
     * Saves type of the connected vehicle.
     * 
     * @param msg heartbeat MavLink message
     */
    public void onVehicleTypeReceived(msg_heartbeat msg) {
        setType(msg.type);
    }

    /**
     * Save the type passed as an argument and sends notification to listeners if it differs from
     * the previous.
     * 
     * @param type the new type of vehicle, one of the {@link MAV_TYPE} enum items
     */
    private void setType(int type) {
        if (mType != type) {
            mType = type;
            mEvents.onVehicleEvent(VehicleEvent.TYPE);
        }
    }

    @Override
    protected void setDefaultValues() {
        mType = MAV_TYPE.MAV_TYPE_GENERIC;        
    }
}
