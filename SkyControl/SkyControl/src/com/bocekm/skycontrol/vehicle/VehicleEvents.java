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

import java.util.ArrayList;
import java.util.List;

/**
 * Registers listeners to the vehicle events and handles sending notification to the registered
 * listeners.
 */
public class VehicleEvents {

    /**
     * Events related to changes to the connected vehicle parameters.
     */
    public enum VehicleEvent {

        /** New vehicle speed information received. */
        SPEED_ALTITUDE,
        /** New vehicle attitude information received. */
        ATTITUDE,
        /** Vehicle entered armed mode. */
        ARMED,
        /** Failsafe mode activated. */
        FAILSAFE,
        /** Flight mode (guided, loiter, etc.) changed. */
        FLIGHT_MODE,
        /** Vehicle changed its state from flying to not flying and vice versa. */
        IS_FLYING,
        /** Connected vehicle type (copter,fixed-wing,etc.) received. */
        TYPE,
        /** New global position received. */
        POSITION,
        /** GPS got fixed. */
        GPS_FIX,
        /** Number of localized GPS satellites received. */
        GPS_COUNT,
        /** Heartbeat hasn't been received from vehicle for some time. */
        HEARTBEAT_TIMEOUT,
        /** Heartbeat received again after timeout. */
        HEARTBEAT_RESTORED,
        /** Periodic heartbeat received. */
        HEARTBEAT_PERIODIC,
        /** Heartbeat of a new vehicle received. */
        VEHICLE_CONNECTED
    }

    /**
     * The listener interface for receiving vehicle events. The class that is interested in
     * processing a vehicle event implements this interface, and the object created with that class
     * is registered with a component using the component's
     * <code>addVehicleListener<code> method. When
     * the vehicle event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see VehicleEvent
     */
    public interface VehicleListener {

        /**
         * Implement this method to capture any {@link VehicleEvent}.
         * 
         * @param event one of the {@link VehicleEvent} enum items
         */
        public void onVehicleEvent(VehicleEvent event);
    }

    /** Registered listeners to the {@link VehicleEvent}s. */
    private List<VehicleListener> vehicleListeners = new ArrayList<VehicleListener>();

    /**
     * Adds listener to the {@link VehicleEvent}s.
     * 
     * @param listener object implementing the VehicleListener
     */
    public void addVehicleListener(VehicleListener listener) {
        if (listener != null & !vehicleListeners.contains(listener))
            vehicleListeners.add(listener);
    }

    /**
     * Removes the registered listener.
     * 
     * @param listener object implementing the VehicleListener to be removed from the list of
     *        listeners
     */
    public void removeVehicleListener(VehicleListener listener) {
        if (listener != null && vehicleListeners.contains(listener))
            vehicleListeners.remove(listener);
    }

    /**
     * Calling this method results in dispatching an event to all the registered listeners.
     * 
     * @param event the event to be dispatched
     */
    public void onVehicleEvent(VehicleEvent event) {
        if (vehicleListeners.size() > 0) {
            for (VehicleListener listener : vehicleListeners) {
                listener.onVehicleEvent(event);
            }
        }
    }
}
