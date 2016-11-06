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
package com.bocekm.skycontrol.connection;

import java.util.ArrayList;
import java.util.List;

import com.bocekm.skycontrol.mavlink.MavLinkService;

/**
 * Registers listeners to the connection events and handles sending notification to the registered
 * listeners.
 */
public class ConnectionEvents {

    /**
     * Events related to the communication with vehicle.
     */
    public enum ConnectionEvent {

        /** Telemetry information received. */
        TELEMETRY,
        /** {@link MavLinkService} unbound. */
        SERVICE_UNBOUND,
        /** {@link MavLinkService} bound. */
        SERVICE_BOUND,
        /** Status text received. */
        STATUS_RECEIVED
    }

    /**
     * The listener interface for receiving connection events. The class that is interested in
     * processing a connection event implements this interface, and the object created with that
     * class is registered with a component using the component's
     * <code>addConnectionListener<code> method. When the connection event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see ConnectionEvent
     */
    public interface ConnectionListener {

        /**
         * Implement this method to capture any {@link ConnectionEvent}.
         * 
         * @param event one of the {@link ConnectionEvent} enum items
         */
        public void onConnectionEvent(ConnectionEvent event);
    }

    /** Registered listeners to the {@link ConnectionEvent}s. */
    private List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();

    /**
     * Adds listener to the {@link ConnectionEvent}s.
     * 
     * @param listener object implementing the {@link ConnectionListener}
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null & !connectionListeners.contains(listener))
            connectionListeners.add(listener);
    }

    /**
     * Removes the registered listener.
     * 
     * @param listener object implementing the {@link ConnectionListener} to be removed from the
     *        list of listeners
     */
    public void removeConnectionListener(ConnectionListener listener) {
        if (listener != null && connectionListeners.contains(listener))
            connectionListeners.remove(listener);
    }

    /**
     * Calling this method means dispatching an event to all the registered listeners.
     * 
     * @param event the event to be dispatched
     */
    public void onConnectionEvent(ConnectionEvent event) {
        if (connectionListeners.size() > 0) {
            // Loop through all registered listeners
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnectionEvent(event);
            }
        }
    }
}
