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
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener;
import com.bocekm.skycontrol.mission.FlightDirector;
import com.bocekm.skycontrol.mission.FlightDirector.FdState;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.WaypointFollower;
import com.bocekm.skycontrol.mission.WaypointFollower.WpfState;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleHeartbeat.HeartbeatState;

/**
 * Responsible for showing the waypoint follower and flight director modes toggle buttons on
 * action bar. The flight director mode actions are delegated to {@link FlightDirector}. The
 * waypoint follower mode actions are delegated to {@link WaypointFollower}.
 */
public class MissionActionProvider extends ActionProvider implements
        MissionListener,
        CollisionListener {

    /** Context of the views. */
    private final Context mContext;

    /** Text view displaying state of the waypoint follower mode to user. */
    private TextView mWpfStateTextView;

    /** Text view displaying state of the flight director mode to user. */
    private TextView mFdStateTextView;
    private LinearLayout mCollisionLayout;

    /**
     * Instantiates a new mission action provider.
     * 
     * @param context the context
     */
    public MissionActionProvider(Context context) {
        super(context);
        mContext = context;
        Mission.get().getEvents().addMissionListener(this);
        CollisionAvoidance.get().getEvents().addCollisionListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.ActionProvider#onCreateActionView()
     */
    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.mission_action_provider, null);
        // Display actual state of both modes to user
        mWpfStateTextView = (TextView) view.findViewById(R.id.wpf_menu_state);
        mWpfStateTextView
                .setText(Mission.get().getWaypointFollower().getWpfState() == WpfState.WPF_ENGAGED ? "ON"
                        : "OFF");
        mFdStateTextView = (TextView) view.findViewById(R.id.fd_menu_state);
        mFdStateTextView
                .setText(Mission.get().getFlightDirector().getFdState() == FdState.FD_ENGAGED ? "ON"
                        : "OFF");
        mCollisionLayout = (LinearLayout) view.findViewById(R.id.collision_layout);
        if (CollisionAvoidance.get().inDangerOfCollision())
            mCollisionLayout.setVisibility(View.VISIBLE);
        else
            mCollisionLayout.setVisibility(View.INVISIBLE);
        mCollisionLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Mission.get().getWaypointFollower().getWpfState() != WpfState.WPF_IDLE)
                    // User wishes to disengage the mode - switch to manual flight mode
                    Mission.get().getWaypointFollower().disengageWaypointFollower(true);
                // Flight Director disengages automatically
            }
        });

        // Toggle button for waypoint follower mode
        LinearLayout wpfButton = (LinearLayout) view.findViewById(R.id.wpf_menu_layout);
        wpfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Vehicle.get().getHeartbeat().getHeartbeatState() == HeartbeatState.PERIODIC_HEARTBEAT) {
                    if (Mission.get().getWaypointFollower().getWpfState() != WpfState.WPF_IDLE)
                        // User wishes to disengage the mode - switch to manual flight mode
                        Mission.get().getWaypointFollower().disengageWaypointFollower(true);
                    else if (Mission.get().getWaypointFollower().getWpfState() == WpfState.WPF_ENGAGE_REQUESTED) {
                        Mission.get().getWaypointFollower().setWpfDisengaged(false);
                        Mission.get().getWaypointFollower().engageWaypoingFollower();
                    } else {
                        if (Mission.get().getFlightDirector().getFdState() != FdState.FD_IDLE)
                            // If FD is engaged while WP is requested then set FD as disengaged
                            // before
                            Mission.get().getFlightDirector().setFdDisengaged(false);
                        Mission.get().getWaypointFollower().engageWaypoingFollower();
                    }
                } else
                    SkyControlUtils.toast("Vehicle is not connected", Toast.LENGTH_SHORT);

            }
        });

        // Toggle button for flight director
        LinearLayout fdButton = (LinearLayout) view.findViewById(R.id.fd_menu_layout);
        fdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Vehicle.get().getHeartbeat().getHeartbeatState() == HeartbeatState.PERIODIC_HEARTBEAT) {
                    if (Mission.get().getFlightDirector().getFdState() == FdState.FD_ENGAGED)
                        // User wishes to disengage the mode - switch to manual flight mode
                        Mission.get().getFlightDirector().disengageFlightDirector(true);
                    else if (Mission.get().getFlightDirector().getFdState() == FdState.FD_ENGAGE_REQUESTED) {
                        Mission.get().getFlightDirector().setFdDisengaged(false);
                        Mission.get().getFlightDirector().engageFlightDirector();
                    } else {
                        if (Mission.get().getWaypointFollower().getWpfState() != WpfState.WPF_IDLE)
                            // If WP is engaged while FD is requested then set WP as disengaged
                            // before
                            Mission.get().getWaypointFollower().setWpfDisengaged(false);
                        Mission.get().getFlightDirector().engageFlightDirector();
                    }
                } else
                    SkyControlUtils.toast("Vehicle is not connected", Toast.LENGTH_SHORT);
            }
        });
        return view;
    }



    /**
     * Removes the listeners. Shall be called by the Activity which created this object.
     */
    public void removeListeners() {
        Mission.get().getEvents().removeMissionListener(this);
        CollisionAvoidance.get().getEvents().removeCollisionListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionEvents.MissionListener#onMissionEvent(com.bocekm.skycontrol
     * .mission.MissionEvents.MissionEvent)
     */
    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case FD_DISENGAGED:
                mFdStateTextView.setText("OFF");
                break;
            case FD_ENGAGED:
                mFdStateTextView.setText("ON");
                break;
            case WPF_DISENGAGED:
                mWpfStateTextView.setText("OFF");
                break;
            case WPF_ENGAGED:
                mWpfStateTextView.setText("ON");
                break;
            default:
                break;
        }
    }

    @Override
    public void onCollisionEvent(CollisionEvent event) {
        switch (event) {
            case DANGER_OF_COLLISION:
                mCollisionLayout.setVisibility(View.VISIBLE);
                break;
            case CLEAR_OF_COLLISION:
                mCollisionLayout.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }


}
