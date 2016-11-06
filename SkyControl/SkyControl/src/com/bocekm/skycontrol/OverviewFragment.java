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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;

/**
 * {@link OverviewFragment} displays basic flight data of connected vehicle, like speed,
 * heading, etc.
 */
public class OverviewFragment extends FlightDataFragment implements
        VehicleListener {

    // References to the UI elements
    private TextView mAirspeedView;
    private TextView mAltitudeView;
    private TextView mVertSpeedView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        // Add VehicleListener to receive vehicle location updates
        Vehicle.get().getEvents().addVehicleListener(this);

        // Get references to the UI elements
        mAirspeedView = (TextView) rootView.findViewById(R.id.airspeed_value);
        mAltitudeView = (TextView) rootView.findViewById(R.id.altitude_value);
        mAltitudeView.setText(String.valueOf(((Float) Vehicle.get().getAltitude().getAltitude())
                .intValue()));
        mVertSpeedView = (TextView) rootView.findViewById(R.id.vs_or_dist_value);
        return rootView;
    }

    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case ATTITUDE:
                this.onEvent(Vehicle.get().getAttitude().getPitchInDegrees(), Vehicle.get()
                        .getAttitude().getRollInDegrees(), Vehicle.get().getAttitude()
                        .getYawInDegrees());
                break;
            case SPEED_ALTITUDE:
                Float airspeed = Vehicle.get().getSpeed().getAirspeed();
                Float altitude = Vehicle.get().getAltitude().getAltitude();
                Float vertSpeed = Vehicle.get().getSpeed().getVerticalSpeed();

                mAirspeedView.setText(String.valueOf(airspeed.intValue()));
                mAltitudeView.setText(String.valueOf(altitude.intValue()));
                mVertSpeedView.setText(String.valueOf(vertSpeed.intValue()));
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        Vehicle.get().getEvents().removeVehicleListener(this);
        super.onDestroy();
    }
}
