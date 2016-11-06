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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bocekm.skycontrol.mission.FlightDirector;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * Sets the UI elements and onclick actions for the flight director controllers - number pickers.
 */
public class FlightDirectorControllersFragment extends Fragment {

    /** Maximum airspeed settable in number picker. */
    private static final int MAX_SPEED = 50;
    /** Maximum vertical speed settable in number picker. */
    private static final int MAX_VS = 15;
    /** Maximum heading settable in number picker. */
    private static final int HEADING_MAX_DEGREES = 360;

    /** Tags for identifying number picker response. */
    private static final int HEADING_REQUEST = 0;
    private static final int ALTITUDE_REQUEST = 1;
    private static final int SPEED_REQUEST = 2;
    private static final int VS_REQUEST = 3;

    /** References to UI elements. */
    private TextView mHeadingTextView;
    private TextView mAltitudeTextView;
    private TextView mAirspeedTextView;
    private TextView mVertSpeedTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.flight_data, container, false);

        // Obtain references to the UI elements
        mHeadingTextView = (TextView) view.findViewById(R.id.heading_value);
        mAltitudeTextView = (TextView) view.findViewById(R.id.altitude_value);
        mAirspeedTextView = (TextView) view.findViewById(R.id.airspeed_value);
        mVertSpeedTextView = (TextView) view.findViewById(R.id.vs_or_dist_value);
        mHeadingTextView.setText(String.valueOf(FlightDirector.FD_MIN_HEADING));
        mAltitudeTextView.setText(String.valueOf(FlightDirector.FD_MIN_ALTITUDE));
        mAirspeedTextView.setText(String.valueOf(FlightDirector.FD_MIN_AIRSPEED));
        mVertSpeedTextView.setText(String.valueOf(FlightDirector.FD_MIN_VERT_SPEED));

        // Set onclick listeners to make buttons from layouts
        LinearLayout layout = updateLayout(view, R.id.heading_bearing_layout);
        layout.setOnClickListener(new OnNumberButtonClickListener(getActivity(), this,
                FlightDirector.FD_MIN_HEADING, HEADING_MAX_DEGREES, R.id.heading_value,
                HEADING_REQUEST, "Set heading"));

        layout = updateLayout(view, R.id.altitude_layout);
        layout.setOnClickListener(new OnNumberButtonClickListener(getActivity(), this,
                FlightDirector.FD_MIN_ALTITUDE, getResources().getInteger(
                        R.integer.max_waypoint_altitude), R.id.altitude_value, ALTITUDE_REQUEST,
                "Set altitude (max " + getResources().getInteger(R.integer.max_waypoint_altitude)
                        + " m)"));

        layout = updateLayout(view, R.id.airspeed_layout);
        layout.setOnClickListener(new OnNumberButtonClickListener(getActivity(), this,
                FlightDirector.FD_MIN_AIRSPEED, MAX_SPEED, R.id.airspeed_value, SPEED_REQUEST,
                "Set airspeed (max " + MAX_SPEED + " m/s)"));

        layout = updateLayout(view, R.id.vs_layout);
        layout.setOnClickListener(new OnNumberButtonClickListener(getActivity(), this,
                FlightDirector.FD_MIN_VERT_SPEED, MAX_VS, R.id.vs_or_dist_value, VS_REQUEST,
                "Set vertical speed (max " + MAX_VS + " m/s)"));

        return view;
    }

    /**
     * Copy current vehicle parameters to flight director.
     */
    protected void overwriteFlightDirectorWithCurrent() {
        int current = (int) Vehicle.get().getAttitude().getYawInDegrees();
        if (current < FlightDirector.FD_MIN_HEADING)
            // Do not allow to copy value less then minimum specified for FD
            current = FlightDirector.FD_MIN_HEADING;
        Mission.get().getFlightDirector().setHeading(current);
        mHeadingTextView.setText(String.valueOf(current));

        current = (int) Vehicle.get().getAltitude().getAltitude();
        if (current < FlightDirector.FD_MIN_ALTITUDE)
            // Do not allow to copy value less then minimum specified for FD
            current = FlightDirector.FD_MIN_ALTITUDE;
        Mission.get().getFlightDirector().setAltitude(current);
        mAltitudeTextView.setText(String.valueOf(current));

        current = (int) Vehicle.get().getSpeed().getAirspeed();
        if (current < FlightDirector.FD_MIN_AIRSPEED)
            // Do not allow to copy value less then minimum specified for FD
            current = FlightDirector.FD_MIN_AIRSPEED;
        Mission.get().getFlightDirector().setAirspeed(current);
        mAirspeedTextView.setText(String.valueOf(current));

        current = (int) Math.abs(Vehicle.get().getSpeed().getVerticalSpeed());
        if (current < FlightDirector.FD_MIN_VERT_SPEED)
            // Do not allow to copy value less then minimum specified for FD
            current = FlightDirector.FD_MIN_VERT_SPEED;
        Mission.get().getFlightDirector().setVertSpeed(current);
        mVertSpeedTextView.setText(String.valueOf(current));
    }

    /**
     * Updates parameters of the UI elements so they have correct size and are clickable.
     * 
     * @param view view the UI element belongs to
     * @param layoutId resource id of the UI element
     * @return updated UI element
     */
    private LinearLayout updateLayout(View view, int layoutId) {
        LinearLayout layout = (LinearLayout) view.findViewById(layoutId);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        int marginInPx = SkyControlUtils.dpToPixel(4);
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        layout.setLayoutParams(params);
        layout.setClickable(true);
        layout.setBackgroundResource(R.drawable.view_clickable_background);

        return layout;
    }

    /*
     * (non-Javadoc) Receives result of the number picking by user.
     * 
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        int pickedNumber = data.getIntExtra(NumberPickerDialog.EXTRA_PICKED_NUMBER, 0);
        switch (requestCode) {
            case HEADING_REQUEST:
                mHeadingTextView.setText(String.valueOf(pickedNumber));
                Mission.get().getFlightDirector().setHeading(pickedNumber);
                break;
            case ALTITUDE_REQUEST:
                mAltitudeTextView.setText(String.valueOf(pickedNumber));
                Mission.get().getFlightDirector().setAltitude(pickedNumber);
                break;
            case SPEED_REQUEST:
                mAirspeedTextView.setText(String.valueOf(pickedNumber));
                Mission.get().getFlightDirector().setAirspeed(pickedNumber);
                break;
            case VS_REQUEST:
                mVertSpeedTextView.setText(String.valueOf(pickedNumber));
                Mission.get().getFlightDirector().setVertSpeed(pickedNumber);
                break;
            default:
                break;
        }
    }
}
