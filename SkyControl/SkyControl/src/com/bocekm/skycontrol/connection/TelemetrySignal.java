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

import com.MAVLink.Messages.ardupilotmega.msg_radio;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;

/**
 * The {@link TelemetrySignal} class encompasses available information on quality of the telemetry
 * signal.
 */
public class TelemetrySignal {

    /** Local signal strength. */
    private double mRssi = -1;

    /** Remote signal strength. */
    private double mRemoteRssi = -1;

    /** Local background mNoise level. */
    private double mNoise = -1;

    /** Remote background mNoise level. */
    private double mRemoteNoise = -1;

    /** {@link ConnectionEvents} class for ability to dispatch a {@link ConnectionEvent}. */
    private ConnectionEvents mEvents;

    /**
     * Instantiates a new {@link TelemetrySignal} object.
     * 
     * @param events {@link ConnectionEvents} class for ability to dispatch a
     *        {@link ConnectionEvent}
     */
    public TelemetrySignal(ConnectionEvents events) {
        mEvents = events;
    }

    /**
     * More important then signal strength (RSSI) is the signal to noise ratio (SNR) giving us
     * information on the quality of the signal.
     * 
     * @return the signal to noise ratio of the local transceiver
     */
    public double getSnr() {
        return mRssi - mNoise;
    }

    /**
     * More important then signal strength (RSSI) is the signal to noise ratio (SNR) giving us
     * information on the quality of the signal.
     * 
     * @return the signal to noise ratio of the remote transceiver
     */
    public double getRemoteSnr() {
        return mRemoteRssi - mRemoteNoise;
    }

    /**
     * Telemetry connection quality status received. When using 3DR telemetry, the radio status
     * MavLink message is injected by its firmware while the hardware detects heartbeat message
     * coming through serial communication.
     * 
     * @param msg MavLink packet of {@link msg_radio} type
     */
    public void onTelemetryReceived(msg_radio msg) {
        setTelemetryState(msg.rssi, msg.remrssi, msg.noise, msg.remnoise);
    }

    /**
     * Sets the telemetry state.
     *
     * @param rssi RSSI
     * @param remrssi remote RSSI
     * @param noise noise of the RSSI
     * @param remnoise noise of the remote RSSI
     */
    public void setTelemetryState(byte rssi, byte remrssi, byte noise, byte remnoise) {

        this.mRssi = rssi & 0xFF;
        this.mRemoteRssi = remrssi & 0xFF;
        this.mNoise = noise & 0xFF;
        this.mRemoteNoise = remnoise & 0xFF;

        mEvents.onConnectionEvent(ConnectionEvent.TELEMETRY);
    }
    
    public double getRssi() {
        return mRssi;
    }

    public double getRemoteRssi() {
        return mRemoteRssi;
    }

    public double getNoise() {
        return mNoise;
    }

    public double getRemoteNoise() {
        return mRemoteNoise;
    }
}
