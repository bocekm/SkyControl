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

import java.lang.ref.WeakReference;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;
import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

/**
 * {@link MavLinkClient} serves as a Mavlink communication client in client-server interface, where
 * server is {@link MavLinkService}.
 */
public class MavLinkClient {

    /** New MavLink message received from vehicle through {@link MavLinkService}. */
    public static final int MSG_RECEIVED_PACKET = 0;
    /** Indication that the Service requests to unbind itself from {@link MavLinkClient}. */
    public static final int MSG_SELF_DESTROY_SERVICE = 1;

    /**
     * Context received from the caller which instantiates the {@link MavLinkClient}. It's used for
     * binding the {@link MavLinkService}, so it shall be context of the application component, like
     * an Activity.
     */
    private Context mParent;

    /**
     * Handle to {@link Messenger} of the {@link MavLinkService}. It is used for sending messages to
     * the service.
     */
    private Messenger mServiceMessenger = null;

    /**
     * Handle to {@link Messenger} of this {@link MavLinkClient} class. It is used to get notified
     * about new messages, mainly MavLink packets, sent by {@link MavLinkService}.
     */
    private final Messenger mClientMessenger = new Messenger(new ClientIncomingHandler(this));

    /** Whether the {@link MavLinkService} is bound to the {@link MavLinkClient#mParent}. */
    private boolean mIsServiceBound;

    /**
     * Instantiates a new {@link MavLinkClient} object.
     * 
     * @param context context of the parent
     */
    public MavLinkClient(Context context) {
        mParent = context;
    }

    /**
     * Establish a connection with the MavLink service which uses the telemetry connected via USB.
     */
    public void bindMavLinkService() {
        // Get USB manager to check whether any USB device is connected
        UsbManager manager = (UsbManager) mParent.getSystemService(Context.USB_SERVICE);
        // Find the first available driver. It's unlikely to have more than one USB device
        // connected.
        UsbSerialDriver sDriver = UsbSerialProber.findFirstDevice(manager);
        if (sDriver == null) {
            // When no USB device is connected there's no point in binding the MavLinkService
            SkyControlUtils.toast("No USB device found. Please (re)connect telemetry.",
                    Toast.LENGTH_SHORT);
            Log.d(SkyControlConst.DEBUG_TAG, "No USB device connected");
            return;
        }

        try {
            // Bind the MavLinkService to the parent context
            mIsServiceBound =
                    mParent.bindService(new Intent(mParent, MavLinkService.class),
                            mServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        } catch (SecurityException e) {
            throw new Error("Cannot bind service: " + e.getMessage());
        }
        if (!mIsServiceBound) {
            Log.e(SkyControlConst.ERROR_TAG, "MavLink service not bound");
        }
    }

    /**
     * Unbinds the MavLink service.
     */
    public void unbindMavLinkService() {
        if (isServiceBound()) {
            // Unbinding the service effectively destroys the service as the only connected client
            // was this class
            mParent.unbindService(mServiceConnection);
            // Notify listeners about the event of unbound service
            onServiceUnbound();
        }
    }

    /**
     * Handler of incoming messages from {@link MavLinkService}, mainly MavLink packets.
     */
    private static class ClientIncomingHandler extends Handler {

        /**
         * Weak reference to the parent {@link MavLinkClient}. Reason for weak reference:
         * http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
         */
        private final WeakReference<MavLinkClient> mClientWeak;

        /**
         * Instantiates a new handler of messages coming from {@link MavLinkService}.
         * 
         * @param client the parent {@link MavLinkClient}
         */
        ClientIncomingHandler(MavLinkClient client) {
            mClientWeak = new WeakReference<MavLinkClient>(client);
        }

        /*
         * (non-Javadoc) Process messages coming from the bound service.
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            MavLinkClient mClient = mClientWeak.get();
            if (!mClient.isServiceBound())
                // It may happen that request for service disconnect was placed by MavLinkClient but it not yet
                // happened as service destruction is asynchronous.
                return;
            switch (msg.what) {
                case MSG_RECEIVED_PACKET:
                    Bundle b = msg.getData();
                    MAVLinkMessage m = (MAVLinkMessage) b.getSerializable("packet");
                    // Pass the received packet to the packet handler, which notifies listeners
                    // about the new packet
                    Connection.get().getMavLinkMsgHandler().handleMessage(m);
                    break;
                case MSG_SELF_DESTROY_SERVICE:
                    // The running service requests to be destroyed by unbinding this client from it
                    mClient.unbindMavLinkService();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /** Callbacks for service binding, passed to bindService(). */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        /*
         * (non-Javadoc) Called when the connection with the service has been established, giving us
         * the service object we can use to interact with the service.
         * 
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
         * android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We are communicating with our service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mServiceMessenger = new Messenger(service);
            // Monitor the service for as long as we are connected to it.
            try {
                Message msg = Message.obtain(null, MavLinkService.MSG_REGISTER_CLIENT);
                msg.replyTo = mClientMessenger;
                mServiceMessenger.send(msg);
                onServiceBound();
            } catch (RemoteException e) {
            }
        }

        /*
         * (non-Javadoc) Called when the connection with the service has been unexpectedly
         * disconnected - that is, its process crashed.
         * 
         * @see
         * android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Call the same method which handles service unbinding to notify that the service is no
            // longer running
            onServiceUnbound();
        }
    };

    /**
     * Send MavLink packet to the vehicle.
     * 
     * @param packet the MavLink packet to be sent
     */
    public void sendMavPacket(MAVLinkPacket packet) {
        if (mServiceMessenger == null) {
            return;
        }

        // Create new empty message message type MSG_SEND_DATA specified
        Message msg = Message.obtain(null, MavLinkService.MSG_SEND_DATA);
        Bundle data = new Bundle();
        // Attach MavLink packet to the message
        data.putSerializable("msg", packet);
        msg.setData(data);
        try {
            // Send the message to MavLinkService which is going to pass it to the vehicle using the
            // telemetry
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Error sending MavLink packet with ID " + packet.msgid
                    + ", " + e.getMessage(), e);
            SkyControlUtils
                    .log("Error sending MavLink packet with ID " + packet.msgid + "\n", true);
            onServiceUnbound();
        }
    }

    /**
     * Notify the listeners that the {@link MavLinkService} has been bound.
     */
    private void onServiceBound() {
        Connection.get().getEvents().onConnectionEvent(ConnectionEvent.SERVICE_BOUND);
    }

    /**
     * Notify the listeners that the {@link MavLinkService} has been unbound.
     */
    private void onServiceUnbound() {
        mIsServiceBound = false;
        Connection.get().getEvents().onConnectionEvent(ConnectionEvent.SERVICE_UNBOUND);
    }

    /**
     * Check whether the {@link MavLinkService} has been bound.
     * 
     * @return true if the {@link MavLinkService} is running, false otherwise
     */
    public boolean isServiceBound() {
        return mIsServiceBound;
    }

    /**
     * Bind or unbind the {@link MavLinkService} based on the current state of the service. If it's
     * running it will be unbound and the other way around.
     */
    public void toggleConnectionState() {
        if (isServiceBound()) {
            unbindMavLinkService();
        } else {
            bindMavLinkService();
        }
    }

}
