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
package com.bocekm.skycontrol.mission;

import java.util.List;

import android.widget.Toast;

import com.MAVLink.Messages.ApmModes;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mavlink.MavLinkFlightMode;
import com.bocekm.skycontrol.mavlink.MavLinkMission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.rrt.RrtSearch;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;
import com.google.android.gms.maps.model.LatLng;

/**
 * On engage sends current mission to vehicle and starts its execution. When obstacle is detected
 * while executing mission an collision free path gets generated and sent to vehicle to follow. On
 * disengage moves the vehicle back to MANUAL autopilot mode.
 */
public class WaypointFollower implements
        ConnectionListener,
        CollisionListener,
        MissionListener,
        VehicleListener {

    /** State of the waypoint follower mode. */
    private static WpfState sWpfState = WpfState.WPF_IDLE;

    /**
     * Saved current mission index in case of injecting the mission with collision avoidance
     * waypoints.
     */
    private int mWaypointIndexToRecover = Mission.NO_WAYPOINT;

    private boolean mToastAboutDisengage = false;

    /**
     * States of the waypoint follower mode.
     */
    public enum WpfState {
        /** Waypoint follower mode is disabled. */
        WPF_IDLE,
        /** Waypoint follower mode is requested by user. */
        WPF_ENGAGE_REQUESTED,
        /** Waypoint follower mode is engaged. */
        WPF_ENGAGED
    }

    /**
     * Instantiates a new waypoint follower object.
     * 
     * @param events {@link MissionEvents}
     */
    public WaypointFollower(MissionEvents events) {
        events.addMissionListener(this);
        Vehicle.get().getEvents().addVehicleListener(this);
        Connection.get().getEvents().addConnectionListener(this);
        CollisionAvoidance.get().getEvents().addCollisionListener(this);
    }

    /**
     * Disengage waypoint follower mode.
     * 
     * @param toast true if message about this mode state change should be toasted
     */
    public void disengageWaypointFollower(boolean toast) {
        mToastAboutDisengage = toast;
        MavLinkFlightMode.changeFlightMode(ApmModes.FIXED_WING_MANUAL);
    }

    /**
     * Engage waypoint follower mode.
     */
    public void engageWaypoingFollower() {
        // Send the current waypoints to the vehicle
        sWpfState = WpfState.WPF_ENGAGE_REQUESTED;
        SkyControlUtils.log("WPF requested\n", false);
        Mission.get().sendMissionToVehicle();
    }

    /**
     * Sets WP state to ENGAGED.
     */
    public void setWpfEngaged() {
        sWpfState = WpfState.WPF_ENGAGED;
        if (CollisionAvoidance.get().inDangerOfCollision())
            SkyControlUtils.toast("Collision avoidance initiated", Toast.LENGTH_SHORT);
        else
            SkyControlUtils.toast("Mission initiated", Toast.LENGTH_SHORT);
        Mission.get().getEvents().onMissionEvent(MissionEvent.WPF_ENGAGED);
    }

    /**
     * Sets WP state to IDLE.
     * 
     * @param toast true if message about this mode state change should be toasted
     */
    public void setWpfDisengaged(boolean toast) {
        if (mToastAboutDisengage || toast) {
            SkyControlUtils.toast("Mission aborted", Toast.LENGTH_SHORT);
            mToastAboutDisengage = false;
        }
        sWpfState = WpfState.WPF_IDLE;
        Mission.get().getEvents().onMissionEvent(MissionEvent.WPF_DISENGAGED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener#onConnectionEvent(com
     * .bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent)
     */
    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                if (sWpfState != WpfState.WPF_IDLE)
                    setWpfDisengaged(false);
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener#onCollisionEvent(com.bocekm.
     * skycontrol.cas.CollisionEvents.CollisionEvent)
     */
    @Override
    public void onCollisionEvent(CollisionEvent event) {
        switch (event) {
            case DANGER_OF_COLLISION:
                if (sWpfState == WpfState.WPF_ENGAGED) {
                    NavMissionItem currentWp = Mission.get().getCurrentWp();
                    RrtSearch rrtSearch =
                            new RrtSearch(Vehicle.get().getPosition().getPosition(), Vehicle.get()
                                    .getAttitude().getYawInDegrees(), Vehicle.get().getAltitude()
                                    .getAltitude(), currentWp.getPosition());
                    List<LatLng> rrtWaypoints = rrtSearch.runSearch(1000);
                    Mission.get().addWaypointsOnIndex(rrtWaypoints,
                            (int) Vehicle.get().getAltitude().getAltitude(),
                            Mission.get().getCurrentWpIndexInApp());
                    mWaypointIndexToRecover = Mission.get().getCurrentWpIndexInAutopilot();
                    Mission.get().sendMissionToVehicle();
                }
                break;
            default:
                break;
        }
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
            case MISSION_WRITTEN:
                if (sWpfState != WpfState.WPF_IDLE) {
                    if (mWaypointIndexToRecover != Mission.NO_WAYPOINT) {
                        // Recover the current waypoint index present before collision detection
                        MavLinkMission.sendSetCurrentMissionItem(mWaypointIndexToRecover);
                        mWaypointIndexToRecover = Mission.NO_WAYPOINT;
                    } else
                        MavLinkMission.sendSetCurrentMissionItem(MavLinkMission.RESTART_MISSION);
                    if (Vehicle.get().getState().getMode() == ApmModes.FIXED_WING_AUTO) {
                        if (sWpfState != WpfState.WPF_ENGAGED)
                            // When flight mode AUTO was already set then onchange notification
                            // would
                            // never come, so setting WP on here directly
                            setWpfEngaged();
                    } else
                        MavLinkFlightMode.changeFlightMode(ApmModes.FIXED_WING_AUTO);
                }
                break;
            case MISSION_WRITE_FAILED:
                if (sWpfState == WpfState.WPF_ENGAGE_REQUESTED) {
                    setWpfDisengaged(false);
                    SkyControlUtils.toast("Communication error. Could not engage WPF",
                            Toast.LENGTH_SHORT);
                }
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener#onVehicleEvent(com.bocekm.skycontrol
     * .vehicle.VehicleEvents.VehicleEvent)
     */
    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case FLIGHT_MODE:
                switch (Vehicle.get().getState().getMode()) {
                    case FIXED_WING_AUTO:
                        if (sWpfState == WpfState.WPF_ENGAGE_REQUESTED)
                            setWpfEngaged();
                        break;
                    default:
                        if (sWpfState != WpfState.WPF_IDLE)
                            setWpfDisengaged(true);
                        break;
                }
                break;
            default:
                break;
        }
    }

    public WpfState getWpfState() {
        return sWpfState;
    }
}
