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

import java.util.ArrayList;
import java.util.List;

/**
 * Registers listeners to the mission events and handles sending notification to the registered
 * listeners.
 */
public class MissionEvents {

    /**
     * Events related to the vehicle mission.
     */
    public enum MissionEvent {

        /** The response from vehicle on mission related request timed out after retries performed. */
        RESPONSE_TIMEOUT,

        /** Vehicle moved to a new mission item in a list. */
        CURRENT_MISSION_ITEM,

        /** Requested mission received from vehicle. */
        MISSION_RECEIVED,

        /** Mission successfully sent to a vehicle. */
        MISSION_WRITTEN,
        
        /** Error occurred when sending mission to a vehicle. */
        MISSION_WRITE_FAILED,

        /** Current mission has been updated. */
        MISSION_UPDATE,
        
        /** Current mission has been updated. */
        WAYPOINT_ADDED,

        /** Vehicle (its navigation controller) sent an information about planned navigation action. */
        NAV_CONTROLLER,
        
        /** Flight director engaged. */
        FD_ENGAGED,
        
        /** Flight director disengaged. */
        FD_DISENGAGED,
        
        /** Waypoint following mode engaged. */
        WPF_ENGAGED,
        
        /** Waypoint following mode disengaged. */
        WPF_DISENGAGED
    }

    /**
     * The listener interface for receiving mission events. The class that is interested in
     * processing a mission event implements this interface, and the object created with that class
     * is registered with a component using the component's
     * <code>addMissionListener<code> method. When
     * the mission event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see MissionEvent
     */
    public interface MissionListener {

        /**
         * Called when mission related event occurs.
         * 
         * @param event one of {@link MissionEvent} enum items
         */
        public void onMissionEvent(MissionEvent event);
    }

    /** Registered listeners to the {@link MissionEvent}s. */
    private List<MissionListener> missionListeners = new ArrayList<MissionListener>();

    /**
     * Registers listener to the {@link MissionEvent}s.
     * 
     * @param listener object implementing the {@link MissionListener}
     */
    public void addMissionListener(MissionListener listener) {
        if (listener != null & !missionListeners.contains(listener))
            missionListeners.add(listener);
    }

    /**
     * Removes the registered listener.
     * 
     * @param listener object implementing the ConnectionListener to be removed from the list of
     *        listeners
     */
    public void removeMissionListener(MissionListener listener) {
        if (listener != null && missionListeners.contains(listener))
            missionListeners.remove(listener);
    }

    /**
     * Calling this method means dispatching an event to all the registered listeners.
     * 
     * @param event the {@link MissionEvent} to be dispatched
     */
    public void onMissionEvent(MissionEvent event) {
        if (missionListeners.size() > 0) {
            for (MissionListener listener : missionListeners) {
                listener.onMissionEvent(event);
            }
        }
    }
}
