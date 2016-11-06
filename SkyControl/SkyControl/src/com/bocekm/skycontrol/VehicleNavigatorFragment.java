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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;

/**
 * Displays values received from vehicle navigation controller - planned navigation flight data.
 */
public class VehicleNavigatorFragment extends FlightDataFragment implements
        MissionListener {

    // References to the UI elements
    /** Difference between current and desired speed. */
    private TextView mSpeedDiffView;
    /** Difference between current and desired altitude. */
    private TextView mAltDiffView;
    /** Distance to next waypoint in meters. */
    private TextView mDistToWpView;
    /** Bearing to next waypoint in degrees. */
    private TextView mBearingView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        // Add MissionListener to receive navigation controller updates
        Mission.get().getEvents().addMissionListener(this);

        // Update the layout element strings
        TextView text = (TextView) rootView.findViewById(R.id.vs_or_dist_title);
        text.setText("DIST TO WP");
        text = (TextView) rootView.findViewById(R.id.vs_or_dist_unit);
        text.setText("m");
        text.setPadding(text.getPaddingLeft(), text.getPaddingTop(), SkyControlUtils.dpToPixel(18),
                text.getPaddingBottom());
        text = (TextView) rootView.findViewById(R.id.airspeed_title);
        text.setText("AIRSPEED Δ");
        text = (TextView) rootView.findViewById(R.id.altitude_title);
        text.setText("ALTITUDE Δ");

        // Make the bearing layout elements visible
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.bearing_layout);
        layout.setVisibility(View.VISIBLE);
        View separator = (View) rootView.findViewById(R.id.bearing_separator);
        separator.setVisibility(View.VISIBLE);

        // Get references to the UI elements
        mSpeedDiffView = (TextView) rootView.findViewById(R.id.airspeed_value);
        mAltDiffView = (TextView) rootView.findViewById(R.id.altitude_value);
        mDistToWpView = (TextView) rootView.findViewById(R.id.vs_or_dist_value);
        mBearingView = (TextView) rootView.findViewById(R.id.bearing_value);
        
        return rootView;
    }

    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case NAV_CONTROLLER:
                this.onEvent(Mission.get().getNavController()
                        .getNavPitch(), Mission.get().getNavController()
                        .getNavRoll(), (float) Mission.get().getNavController().getNavHeading());

                Float speedDiff = Mission.get().getNavController().getCurrToTargAsdpDiff();
                Float altDiff = Mission.get().getNavController().getCurrToTargAltDiff();
                Short distToWp = Mission.get().getNavController().getDistToWaypoint();
                Short bearing = Mission.get().getNavController().getTargetBearing();

                mSpeedDiffView.setText(String.valueOf(speedDiff.intValue()));
                mAltDiffView.setText(String.valueOf(altDiff.intValue()));
                mDistToWpView.setText(String.valueOf(distToWp.intValue()));
                mBearingView.setText(String.valueOf(bearing.intValue()));
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        Mission.get().getEvents().removeMissionListener(this);
        super.onDestroy();
    }
}
