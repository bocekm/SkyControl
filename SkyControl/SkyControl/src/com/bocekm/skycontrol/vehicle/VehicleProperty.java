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

import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mavlink.MavLinkService;

/**
 * Vehicle property superclass which calls {@link VehicleProperty#setDefaultValues()} when
 * {@link MavLinkService} is unbound so the classes representing properties of connected vehicle can
 * set themselves to state when no vehicle is connected.
 */
public abstract class VehicleProperty implements
        ConnectionListener {

    /**
     * {@link VehicleProperty} constructor. Registers {@link ConnectionListener} to be able to
     * receive {@link ConnectionEvent#SERVICE_UNBOUND}.
     */
    protected VehicleProperty() {
        Connection.get().getEvents().addConnectionListener(this);
    }

    /**
     * Sets values of the vehicle properties to default state as no vehicle is connected.
     */
    protected abstract void setDefaultValues();

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener#onConnectionEvent(com
     * .bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent)
     */
    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                setDefaultValues();
                break;
            default:
                break;
        }
    }

}
