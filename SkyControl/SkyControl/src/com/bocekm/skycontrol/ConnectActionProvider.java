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
package com.bocekm.skycontrol;

import android.content.Context;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;

/**
 * Responsible for showing the connection button on action bar and responding to clicks on it.
 */
public class ConnectActionProvider extends ActionProvider implements
        ConnectionListener {
    private final Context mContext;
    private static final short NO_SIGNAL = -1;
    /** Signal strength. */
    private static double sSignal = NO_SIGNAL;
    /** Whether the app is connected to the USB telemetry. */
    private static boolean sConnected = false;

    /** References to UI elements. */
    private View mView;
    private TextView mConnButtonTitle;
    private TextView mSignalTextView;

    public ConnectActionProvider(Context context) {
        super(context);
        mContext = context;
        Connection.get().getEvents().addConnectionListener(this);
    }

    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        mView = layoutInflater.inflate(R.layout.connect_action_provider, null);
        mConnButtonTitle = (TextView) mView.findViewById(R.id.connect_menu_title);
        if (sConnected)
            mConnButtonTitle.setText(R.string.menu_disconnect);
        else
            mConnButtonTitle.setText(R.string.menu_connect);
        mSignalTextView = (TextView) mView.findViewById(R.id.rssi_menu_value);
        if (sSignal != NO_SIGNAL)
            updateSignalTextView();
        LinearLayout wpButton = (LinearLayout) mView.findViewById(R.id.connect_menu_layout);
        wpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Connection.get().getMavLinkClient().toggleConnectionState();
            }
        });
        return mView;
    }

    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                sSignal = NO_SIGNAL;
                sConnected = false;
                mConnButtonTitle.setText(R.string.menu_connect);
                // Hide the signal strength text
                mSignalTextView.setVisibility(View.GONE);
                break;
            case SERVICE_BOUND:
                sConnected = true;
                mConnButtonTitle.setText(R.string.menu_disconnect);
                break;
            case TELEMETRY:
                double local = Connection.get().getTelemetry().getSnr();
                double remote = Connection.get().getTelemetry().getRemoteSnr();
                // Show the lower from local and remote signal strength values. If it's negative,
                // show zero instead.
                sSignal = Math.min(local, remote) > 0 ? Math.min(local, remote) : 0;
                updateSignalTextView();
                break;
            default:
                break;
        }
    }

    /**
     * Update text showing the signal strength. Show it if it's for the first time.
     */
    private void updateSignalTextView() {
        mSignalTextView.setVisibility(View.VISIBLE);
        mSignalTextView.setText("signal: " + String.valueOf((int) sSignal));
    }

    public void removeListener() {
        Connection.get().getEvents().removeConnectionListener(this);
    }

}
