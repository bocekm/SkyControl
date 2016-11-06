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
package com.bocekm.skycontrol.cas;

import java.util.ArrayList;
import java.util.List;

public class CollisionEvents {
    
    public enum CollisionEvent {
        DANGER_OF_COLLISION,
        CLEAR_OF_COLLISION,
        CHECKPOINT_POSITION_UPDATED,
        OBSTACLES_LOADED,
        OBSTACLES_DESTROYED
    }

    public interface CollisionListener {

        /**
         * Implement this method to capture any {@link CollisionEvent}.
         * 
         * @param event one of the {@link CollisionEvent} enum items
         */
        public void onCollisionEvent(CollisionEvent event);
    }

    /** Registered listeners to the {@link CollisionEvent}s. */
    private List<CollisionListener> collisionListeners = new ArrayList<CollisionListener>();

    /**
     * Adds listener to the {@link CollisionEvent}s.
     * 
     * @param listener object implementing the {@link CollisionListener}
     */
    public void addCollisionListener(CollisionListener listener) {
        if (listener != null & !collisionListeners.contains(listener))
            collisionListeners.add(listener);
    }

    /**
     * Removes the registered listener.
     * 
     * @param listener object implementing the {@link CollisionListener} to be removed from the
     *        list of listeners
     */
    public void removeCollisionListener(CollisionListener listener) {
        if (listener != null && collisionListeners.contains(listener))
            collisionListeners.remove(listener);
    }

    /**
     * Calling this method means dispatching an event to all the registered listeners.
     * 
     * @param event the event to be dispatched
     */
    public void onCollisionEvent(CollisionEvent event) {
        if (collisionListeners.size() > 0) {
            // Loop through all registered listeners
            for (CollisionListener listener : collisionListeners) {
                listener.onCollisionEvent(event);
            }
        }
    }
}
