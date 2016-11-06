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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPacket;
import com.bocekm.skycontrol.MainActivity;
import com.bocekm.skycontrol.R;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.connection.UsbConnection;
import com.bocekm.skycontrol.mavlink.MavLinkConnection.MavLinkConnectionListener;

/**
 * {@link MavLinkService} is a component running alongside other parts of application in the same
 * process. The service runs only as long as any client is bound to it. Once the service is unbound
 * from all clients, the system destroys it.
 * 
 * @see <a
 *      href="http://developer.android.com/guide/components/bound-services.html#Messenger">http://developer.android.com/guide/components/bound-services.html#Messenger</a>
 */

public class MavLinkService extends Service implements
        MavLinkConnectionListener {
    /**
     * Command to the service to register a client, receiving callbacks from the service. The
     * Message's replyTo field must be a Messenger of the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 0;
    /** Command to service to send a MavLink packet bundled within the message. */
    public static final int MSG_SEND_DATA = 1;
    /** Command to service to log the error message sent in a message bundle. */
    public static final int MSG_LOG_ERR = 2;

    /** ID of the notification on status bar. */
    private static final int STATUS_BAR_NOTIFICATION = 1;
    /** The Notification manager. */
    private NotificationManager mNotificationManager;

    /** The MavLink connection. */
    private MavLinkConnection mDeviceConnectionThread;
    /** Current registered client. */
    private Messenger mClientMessenger = null;
    /** Target we publish for client to send messages to IncomingHandler. */
    private Messenger mServiceMessenger;
    /** Handler of messages coming from {@link MavLinkClient}. */
    private Handler mIncomingHandler;

    /**
     * Handler of incoming requests from {@link MavLinkClient}.
     */
    private static class ServiceIncomingHandler extends Handler {

        /**
         * Weak reference to the parent {@link MavLinkService}. Reason for weak reference:
         * http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
         */
        private final WeakReference<MavLinkService> mServiceWeak;

        /**
         * Instantiates a new incoming handler.
         * 
         * @param service the a service
         */
        ServiceIncomingHandler(MavLinkService service) {
            mServiceWeak = new WeakReference<MavLinkService>(service);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            MavLinkService mService = mServiceWeak.get();
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mService.mClientMessenger = msg.replyTo;
                    break;
                case MSG_SEND_DATA:
                    Bundle bData = msg.getData();
                    MAVLinkPacket packet = (MAVLinkPacket) bData.getSerializable("msg");
                    if (mService.mDeviceConnectionThread != null) {
                        mService.mDeviceConnectionThread.sendPacket(packet);
                    }
                    break;
                case MSG_LOG_ERR:
                    Bundle bErr = msg.getData();
                    String errText = bErr.getString("errText");
                    SkyControlUtils.log(errText + "\n", false);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger for sending messages to
     * the service.
     * 
     * @param intent the intent
     * @return the i binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mServiceMessenger.getBinder();
    }

    /**
     * Notify new message.
     * 
     * @param m the
     */
    private void notifyNewMessage(MAVLinkMessage m) {
        try {
            if (mClientMessenger != null) {
                Message msg = Message.obtain(null, MavLinkClient.MSG_RECEIVED_PACKET);
                Bundle data = new Bundle();
                data.putSerializable("packet", m);
                msg.setData(data);
                mClientMessenger.send(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mavlink.MavLinkConnection.MavLinkConnectionListener#onReceiveMessage
     * (com.MAVLink.Messages.MAVLinkMessage)
     */
    @Override
    public void onReceiveMessage(MAVLinkMessage msg) {
        notifyNewMessage(msg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Instantiate a handler of incoming requests from MavLinkClient
        mIncomingHandler = new ServiceIncomingHandler(this);
        mServiceMessenger = new Messenger(mIncomingHandler);

        // Create a notification in status bar while this Service is running
        showNotification();

        // Create a new connection to a communication resource
        connectToDevice();
    }

    /**
     * Called after the exception raised in any of the MavLinkConnection classes. Log error and
     * initiate destruction of this Service.
     * 
     * @param errMsg the error text
     */
    public void onComError(String errMsg) {
        // Prepare error message which needs to be passed to UI thread (through IncomingHandler),
        // because this method runs in separate service thread which cannot interact with UI
        Message msg = Message.obtain(null, MSG_LOG_ERR);
        Bundle data = new Bundle();
        String errText = getResources().getString(R.string.mavlink_error) + " " + errMsg;
        data.putString("errText", errText);
        msg.setData(data);

        // Imitate message sent from MavLinkClient by making use of IncomingHandler just to log
        // the error message
        mIncomingHandler.sendMessage(msg);

        // Initiate releasing the resources (e.g. USB driver) and unbinding from the client
        disconnectService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        // Initiate releasing the resources (e.g. USB driver) and unbinding from the client
        disconnectService();
        super.onDestroy();
    }

    /**
     * Starts the {@link MavLinkConnection} Thread which connects to the communication interface.
     */
    private void connectToDevice() {
        mDeviceConnectionThread = new UsbConnection(this);
        // Start the Thread
        mDeviceConnectionThread.start();
    }

    /**
     * Disconnects from {@link MavLinkConnection} leading to release of the device drivers and
     * destroying this Service.
     */
    private void disconnectService() {
        // Tell the MavLinkClient to unbind itself from this Service. Doing that leads to destroying
        // this Service, because Service without any bound client gets destroyed by Android.
        try {
            if (mClientMessenger != null) {
                Message msg = Message.obtain(null, MavLinkClient.MSG_SELF_DESTROY_SERVICE);
                mClientMessenger.send(msg);
            }
        } catch (RemoteException e) {
            throw new Error("Lost reference to the MavLinkClient: " + e.getMessage());
        }

        // Close the MavLink connection, i.e. release the communication resources
        if (mDeviceConnectionThread != null) {
            mDeviceConnectionThread.onCloseConnection();
            mDeviceConnectionThread = null;
        }
        // Cancel the persistent notification.
        removeNotification();
    }

    /**
     * Shows a notification in the status bar while this service is running. Clicking on the
     * notification takes the user to the application.
     */
    private void showNotification() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String text = getResources().getString(R.string.telemetry_connected_notification);
        // Create the notification object
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_notify_service)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(text);

        // User is presented with MainActivity when clicked on notification
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        mBuilder.setContentIntent(contentIntent);

        // Show the notification
        mNotificationManager.notify(STATUS_BAR_NOTIFICATION, mBuilder.build());
    }

    /**
     * Removes notification about running service from the status bar.
     */
    private void removeNotification() {
        mNotificationManager.cancelAll();
    }
}
