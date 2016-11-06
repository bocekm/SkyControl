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
package com.bocekm.skycontrol.mavlink;

import java.io.IOException;
import java.net.UnknownHostException;

import android.content.Context;

import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;

/**
 * The {@link MavLinkConnection} class handles received/to be sent MavLink data. It is to be
 * extended by class dedicated to cope with the interface, like USB device, TCP protocol, etc.
 */
public abstract class MavLinkConnection extends Thread {


    /**
     * Initialize the communication resources, e.g. USB device driver.
     * 
     * @throws UnknownHostException the unknown host exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected abstract void openConnection() throws UnknownHostException, IOException;

    /**
     * Read data block from the interface (e.g. USB device). Use the
     * {@link MavLinkConnection#mReadData} buffer to store read data.
     * 
     * @return number of bytes read.
     * @throws IOException Signals that an I/O exception while reading data has occurred.
     */
    protected abstract int readDataBlock() throws IOException;

    /**
     * Send data to the communication interface (USB device, TCP protocol, etc).
     * 
     * @param buffer buffer with the data to be sent
     * @throws IOException Signals that an I/O exception has occurred while sending data.
     */
    protected abstract void sendBuffer(byte[] buffer) throws IOException;

    /**
     * Close the connection with the communication resource, i.e. release USB driver, close TCP
     * connection, etc.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected abstract void closeConnection() throws IOException;

    /**
     * The listener interface for receiving mavLinkConnection events. The class that is interested
     * in processing a MavLinkConnection events implements this interface, and the object created
     * with that class is registered with a component by passing the object's Context to the
     * {@link MavLinkConnection#MavLinkConnection(Context)} constructor. When the MavLinkConnection
     * event occurs, that object's appropriate method is invoked.
     */
    public interface MavLinkConnectionListener {

        /**
         * Notifies on incoming new {@link MAVLinkMessage}.
         * 
         * @param msg the received {@link MAVLinkMessage}
         */
        public void onReceiveMessage(MAVLinkMessage msg);

        /**
         * Unrecoverable error occurred on reading data from or sending data to communication
         * interface (USB device, TCP connection, etc). The listener shall destroy itself as this
         * class is going to die after sending this notification.
         * 
         * @param errMsg message with details about the error
         */
        public void onComError(String errMsg);

    }

    /** Registered listener to the {@link MavLinkConnection} events. */
    private MavLinkConnectionListener mListener;

    /** Received MavLink packet. */
    private MAVLinkPacket mReceivedPacket;

    /** Parser to parse incoming MavLink packets. */
    private static final Parser sMavLinkParser = new Parser();

    /** Maximum possible size of the incoming MavLink packet. */
    private static final int MAX_MAV_PACKET_SIZE_BYTES = 256;

    /** The data read by {@link MavLinkConnection#readDataBlock()}. */
    protected byte[] mReadData = new byte[MAX_MAV_PACKET_SIZE_BYTES];

    /** Number of bytes read by {@link MavLinkConnection#readDataBlock()}. */
    private int mBytesRead;

    /** Indication whether we are connected to the communication interface. */
    protected boolean mServiceConnected = true;

    /**
     * Instantiates a new {@link MavLinkConnection} and registers a listener to the
     * {@link MavLinkConnectionListener} events.
     * 
     * @param parentContext parent context, implementing the {@link MavLinkConnectionListener}
     */
    public MavLinkConnection(Context parentContext) {
        try {
            mListener = (MavLinkConnectionListener) parentContext;
        } catch (ClassCastException e) {
            // The object doesn't implement the interface, throw exception
            throw new ClassCastException(parentContext.toString()
                    + " must implement MavLinkConnectionListener");
        }
    }

    /*
     * (non-Javadoc) This method is called when the Thread is started by the Service which
     * instantiated this class.
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        super.run();
        // Optional step of reseting the Parser statistics of MavLink communication
        sMavLinkParser.stats.mavlinkResetStats();
        try {
            // Initialize the communication resources
            openConnection();

            // Read data while the Service instantiating this class is running
            while (mServiceConnected) {
                mBytesRead = readDataBlock();
                parseReadData();
            }

        } catch (IOException e) {
            // Send notification to the listener (shall be Service) that fatal IO error has occurred
            // so it can destroy itself.
            mListener.onComError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses incoming MavLink packet in order to get MavLink message. Notifies registered
     * {@link MavLinkConnectionListener} listener about successfully parsed MavLink message.
     */
    private void parseReadData() {
        if (mBytesRead < 1)
            return;
        for (int i = 0; i < mBytesRead; i++) {
            // Once the parser finishes going through all received bytes then it returns the parsed
            // MavLink message. Until then it returns null.
            mReceivedPacket = sMavLinkParser.mavlink_parse_char(mReadData[i] & 0xff);
            if (mReceivedPacket != null) {
                MAVLinkMessage msg = mReceivedPacket.unpack();
                mListener.onReceiveMessage(msg);
            }
        }
    }

    /**
     * Encode and send a Mavlink packet via the established MavLink connection.
     * 
     * @param packet {@link MAVLinkPacket} to be transmitted
     */
    public void sendPacket(MAVLinkPacket packet) {
        byte[] buffer = packet.encodePacket();
        try {
            sendBuffer(buffer);
        } catch (IOException e) {
            mListener.onComError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Releases the established MavLink connection. Needs to be called by the Service, which
     * instantiates this class, in order to call {@link MavLinkConnection#closeConnection()}.
     */
    public void onCloseConnection() {
        mServiceConnected = false;
    }
}
