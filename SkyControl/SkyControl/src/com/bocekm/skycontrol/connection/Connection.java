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

import android.app.Application;
import android.content.Context;

import com.bocekm.skycontrol.mavlink.MavLinkClient;
import com.bocekm.skycontrol.mavlink.MavLinkMsgHandler;

/**
 * {@link Connection} class is a singleton pattern class which lives from the start of the application
 * till the end. Keeps information about communication connection between the app and vehicle.
 */
public class Connection {

    /** This {@link Connection} class instance. */
    private static Connection sConnection;

    /** Handler of the MavLink messages transmitted by the vehicle. */
    private MavLinkMsgHandler mMavLinkMsgHandler;

    /** {@link MavLinkClient} being used to send MavLink messages to the vehicle. */
    private MavLinkClient mMavLinkClient;

    /** Quality of the telemetry signal. */
    private TelemetrySignal mTelemetrySignal;
    
    /** Class handling the changes happening to the communication connection. */
    private ConnectionEvents mConnectionEvents;
    
    /** Status text sent by vehicle. */
    private String mStatusText;

    /**
     * Constructor of Connection class (private because it's a singleton).
     * 
     * @param appContext {@link Context} of the {@link Application} class
     */
    private Connection(Context appContext) {
        mConnectionEvents = new ConnectionEvents();
        mMavLinkMsgHandler = new MavLinkMsgHandler();
        mMavLinkClient = new MavLinkClient(appContext);
        mTelemetrySignal = new TelemetrySignal(mConnectionEvents);
    }

    /**
     * Instantiates new {@link Connection} object only if it wasn't instantiated before, because it's
     * singleton so just one instance exists per running app.
     * 
     * @param c any {@link Context}
     * @return instance of the Connection singleton
     */
    public static Connection init(Context c) {
        if (sConnection == null) {
            // getApplicationContext makes sure we pass application context to constructor,
            // whatever context we get in parameter
            sConnection = new Connection(c.getApplicationContext());
        }
        return sConnection;
    }

    /**
     * Returns the only instance of {@link Connection} singleton.
     * 
     * @return instance of {@link Connection}
     */
    public static Connection get() {
        return sConnection;
    }

    public TelemetrySignal getTelemetry() {
        return mTelemetrySignal;
    }

    public MavLinkClient getMavLinkClient() {
        return mMavLinkClient;
    }

    public MavLinkMsgHandler getMavLinkMsgHandler() {
        return mMavLinkMsgHandler;
    }
    
    public ConnectionEvents getEvents() {
        return mConnectionEvents;
    }
    
    public String getStatusText() {
        return mStatusText;
    }

    public void setStatusText(String statusText) {
        mStatusText = statusText;
    }
}
