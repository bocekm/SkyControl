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


/**
 * {@link Vehicle} class is a singleton pattern class which lives from the start of the application
 * till the end. Keeps information about vehicle connected to the app.
 */
public class Vehicle {

    /** This {@link Vehicle} class instance. */
    private static Vehicle sVehicle;

    /** Class handling the changes happening to the vehicle. */
    private VehicleEvents mVehicleEvents;

    /** The Vehicle type. */
    private VehicleType mVehicleType;

    /** The Vehicle global position. */
    private VehiclePosition mVehiclePosition;

    /** The Vehicle speed. */
    private VehicleSpeed mVehicleSpeed;

    /** The Vehicle state and flight mode. */
    private VehicleState mVehicleState;

    /** The Vehicle heartbeat. */
    private VehicleHeartbeat mVehicleHeartbeat;

    /** The Vehicle altitude. */
    private VehicleAltitude mVehicleAltitude;

    /** The Vehicle attitude. */
    private VehicleAttitude mVehicleAttitude;

    /** The Vehicle stream rates. */
    private VehicleStreamRates mVehicleStreamRates;
    
    /** The Vehicle parameters. */
    private VehicleParameters mVehicleParameters;

    /**
     * Constructor of Vehicle class (private because it's a singleton).
     */
    private Vehicle() {
        mVehicleEvents = new VehicleEvents();
        mVehicleType = new VehicleType(mVehicleEvents);
        mVehicleStreamRates = new VehicleStreamRates(mVehicleEvents);
        mVehicleHeartbeat = new VehicleHeartbeat(mVehicleEvents);
        mVehicleState = new VehicleState(mVehicleEvents);
        mVehiclePosition = new VehiclePosition(mVehicleEvents);
        mVehicleAttitude = new VehicleAttitude(mVehicleEvents);
        mVehicleSpeed = new VehicleSpeed(mVehicleEvents);
        mVehicleAltitude = new VehicleAltitude();
        mVehicleParameters = new VehicleParameters(mVehicleEvents);
    }

    /**
     * Instantiates new {@link Vehicle} object only if it wasn't instantiated before, because it's
     * singleton so just one instance exists per running app.
     * 
     * @return instance of the Vehicle singleton
     */
    public static Vehicle init() {
        if (sVehicle == null)
            sVehicle = new Vehicle();
        return sVehicle;
    }

    /**
     * Returns the only instance of {@link Vehicle} singleton.
     * 
     * @return instance of {@link Vehicle}
     */
    public static Vehicle get() {
        return sVehicle;
    }

    public VehicleType getType() {
        return mVehicleType;
    }

    public VehicleState getState() {
        return mVehicleState;
    }

    public VehicleStreamRates getStreamRates() {
        return mVehicleStreamRates;
    }

    public VehicleHeartbeat getHeartbeat() {
        return mVehicleHeartbeat;
    }

    public VehicleEvents getEvents() {
        return mVehicleEvents;
    }

    public VehiclePosition getPosition() {
        return mVehiclePosition;
    }

    public VehicleAttitude getAttitude() {
        return mVehicleAttitude;
    }

    public VehicleAltitude getAltitude() {
        return mVehicleAltitude;
    }

    public VehicleSpeed getSpeed() {
        return mVehicleSpeed;
    }
    
    public VehicleParameters getVehicleParameters() {
        return mVehicleParameters;
    }
}
