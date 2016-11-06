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

import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;

import com.MAVLink.Messages.ApmModes;
import com.bocekm.skycontrol.FlightDirectorControllersFragment;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mavlink.MavLinkFlightMode;
import com.bocekm.skycontrol.mavlink.MavLinkMission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.item.DoChangeSpeed;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.mission.item.Waypoint;
import com.bocekm.skycontrol.rrt.RrtSearch;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;
import com.google.android.gms.maps.model.LatLng;

/**
 * Flight direction - imperfect emulation of the Mode Control Panel known from the commercial
 * aircraft. Reaching the values set is achieved by creating mission waypoints at appropriate
 * location and altitude. By reaching these waypoints new ones get created until the FD is switched
 * off. Limitation: no possibility to set each flight parameter to be maintained individually, just
 * all of them at once so mission for the autopilot can be calculated.
 */
public class FlightDirector implements
        MissionListener,
        VehicleListener,
        ConnectionListener,
        CollisionListener {
    /**
     * Minimum distance of the first waypoint which should lie in reasonable distance from the
     * initial position. Having no min distance, with altitude difference close to nothing the first
     * waypoint would lie essentially just few meters away and at the time of receiving the FD
     * mission the vehicle would be passed it already.
     */
    private static final int MIN_WP_DISTANCE_IN_S = 5;

    /** Distance of the next "altitude hold" waypoint in seconds. */
    private static final int ALT_HOLD_WP_DIST_IN_SEC = 15;

    public static final int FD_MIN_HEADING = 0;
    public static final int FD_MIN_ALTITUDE = 0;
    /**
     * Min airspeed and V/S are set to 1 as anything below would make no sense for flight direction.
     */
    public static final int FD_MIN_AIRSPEED = 1;
    public static final int FD_MIN_VERT_SPEED = 1;

    private int mDesiredHeading = FD_MIN_HEADING;
    private int mDesiredAltitude = FD_MIN_ALTITUDE;
    private int mDesiredAirspeed = FD_MIN_AIRSPEED;
    private int mDesiredVertSpeed = FD_MIN_VERT_SPEED;

    /**
     * Flight director finite state machine states.
     */
    public enum FdState {
        /** Flight director disengaged. */
        FD_IDLE,
        /** Flight director mode requested. */
        FD_ENGAGE_REQUESTED,
        /** Flight director engaged. */
        FD_ENGAGED
    }

    /** Current flight director state. */
    private static FdState sFdState = FdState.FD_IDLE;

    private DoChangeSpeed mSpeedCmd;

    private boolean mToastAboutDisengage = false;

    /**
     * Mission with the waypoints calculated from desired flight parameters set in
     * {@link FlightDirectorControllersFragment}.
     */
    private MissionItemList mFdMission = null;

    /**
     * Instantiates a new flight director.
     * 
     * @param events {@link MissionEvents}
     */
    public FlightDirector(MissionEvents events) {
        events.addMissionListener(this);
        Vehicle.get().getEvents().addVehicleListener(this);
        Connection.get().getEvents().addConnectionListener(this);
        CollisionAvoidance.get().getEvents().addCollisionListener(this);
        mFdMission = new MissionItemList();
    }

    /**
     * Disengage flight director.
     * 
     * @param toast true if message about this mode state change should be toasted
     */
    public void disengageFlightDirector(boolean toast) {
        mToastAboutDisengage = toast;
        MavLinkFlightMode.changeFlightMode(ApmModes.FIXED_WING_MANUAL);
    }

    /**
     * Sets the state of FD to engaged and notifies user/listeners.
     */
    private void setFdEngaged() {
        SkyControlUtils.toast("Flight Director engaged", Toast.LENGTH_SHORT);
        sFdState = FdState.FD_ENGAGED;
        Mission.get().getEvents().onMissionEvent(MissionEvent.FD_ENGAGED);
    }

    /**
     * Sets the state of FD to idle and notifies user/listeners.
     * 
     * @param toast true if message about this mode state change should be toasted
     */
    public void setFdDisengaged(boolean toast) {
        if (mToastAboutDisengage || toast) {
            SkyControlUtils.toast("Flight Director disengaged", Toast.LENGTH_SHORT);
            mToastAboutDisengage = false;
        }
        sFdState = FdState.FD_IDLE;
        mFdMission.clearMission();
        Mission.get().getEvents().onMissionEvent(MissionEvent.FD_DISENGAGED);
    }

    /**
     * Update mission generated by FD when any of the flight director controllers are changed.
     */
    private void updateFdMission() {
        // Update the FD waypoints if the flight director is engaged
        if (sFdState != FdState.FD_IDLE)
            engageFlightDirector();
    }

    public FdState getFdState() {
        return sFdState;
    }

    /**
     * Engage flight director.
     */
    public void engageFlightDirector() {
        mFdMission.clearMission();
        if (sFdState == FdState.FD_IDLE) {
            sFdState = FdState.FD_ENGAGE_REQUESTED;
            SkyControlUtils.log("FD requested\n", false);
        }

        // DO_CHANGE_SPEED command is one of numerous autopilot mission commands which is to be set
        // before the waypoints
        mSpeedCmd = new DoChangeSpeed(mFdMission);
        mSpeedCmd.setSpeed(mDesiredAirspeed);
        mFdMission.addMissionItem(mSpeedCmd);

        float currentAltitude = Vehicle.get().getAltitude().getAltitude();

        LatLng currentPosition = Vehicle.get().getPosition().getPosition();
        // Compute distance in which the vehicle will reach desired altitude with given speed and
        // climb rate
        // TODO: Use current ground speed for calculation instead of desired airspeed (airspeed
        // gives us no information about distance flown) and recalculate mission on each current
        // ground speed change
        double altitudeReachedDistance =
                (Math.abs(currentAltitude - mDesiredAltitude) / mDesiredVertSpeed)
                        * mDesiredAirspeed;

        // First waypoint is about getting the airplane to the set altitude, second waypoint is more
        // about keeping the specified heading and altitude. When the first waypoints is reached it
        // is discarded and another waypoint on the same flight path gets generated to keep the
        // airplane on desired path till the FD is disengaged.
        Waypoint altitudeReachedWp = createWp(currentPosition, altitudeReachedDistance);
        if (notCreated(altitudeReachedWp))
            return;
        Waypoint altitudeHoldWp = createAltHoldWp(altitudeReachedWp.getPosition());
        if (notCreated(altitudeHoldWp))
            return;
        // Find a collision free path if any obstacle is on the flight path
        List<LatLng> waypoints =
                getCollisionFreePath(altitudeReachedWp.getPosition(), altitudeHoldWp.getPosition());
        if (notCreated(waypoints))
            return;
        // Put the waypoints right behind the speed command which is on index 0
        mFdMission.addWaypointsOnIndex(waypoints, mDesiredAltitude, 1);
        Mission.get().sendMissionToVehicle(mFdMission);
    }

    /**
     * Find a collision free path if any obstacle is on the flight path between the two waypoints.
     * 
     * @param firstWp the first wp
     * @param secondWp the second wp
     * @return the collision free path
     */
    private List<LatLng> getCollisionFreePath(LatLng firstWp, LatLng secondWp) {
        ArrayList<LatLng> waypointList = new ArrayList<LatLng>();
        boolean doesCollide =
                CollisionAvoidance.checkForCollision(firstWp, secondWp, mDesiredAltitude);
        if (doesCollide) {
            RrtSearch rrtSearch =
                    new RrtSearch(firstWp, mDesiredHeading, mDesiredAltitude, secondWp);
            List<LatLng> rrtWaypoints = rrtSearch.runSearch(1000);
            if (rrtWaypoints == null)
                return null;
            waypointList.add(firstWp);
            waypointList.addAll(rrtWaypoints);
            waypointList.add(secondWp);
            return waypointList;
        } else {
            waypointList.add(firstWp);
            waypointList.add(secondWp);
            return waypointList;
        }
    }

    /**
     * Could not create new object (waypoint/list of waypoints). It means it lies on collision path
     * and no collision free path could be found.
     * 
     * @param <T> the generic type
     * @param object the object
     * @return true, if the object couldn't be created
     */
    private <T> boolean notCreated(T object) {
        if (object != null)
            return false;
        SkyControlUtils.toast("Cannot find collision-free path. Switching FD off...",
                Toast.LENGTH_LONG);
        setFdDisengaged(false);
        return true;
    }

    /**
     * Creates a waypoint with specific distance and bearing from the initial position.
     * 
     * @param initialPosition the initial position
     * @param targetDistance distance from the initial position
     * @return the waypoint
     */
    private Waypoint createWp(LatLng initialPosition, double targetDistance) {
        float currentGroundspeed = Vehicle.get().getSpeed().getGroundspeed();
        if (targetDistance < MIN_WP_DISTANCE_IN_S * currentGroundspeed)
            // Cannot make waypoint too close to the current vehicle position as it would be
            // reached instantly
            targetDistance = MIN_WP_DISTANCE_IN_S * currentGroundspeed;

        Waypoint waypoint = null;
        double originalDistance = targetDistance;
        boolean doesCollide = true;
        for (int i = 0; i < 10 && doesCollide; i++) {
            // Try to find non-colliding position
            LatLng targetPosition =
                    SkyControlUtils
                            .getDestination(initialPosition, mDesiredHeading, targetDistance);
            waypoint = new Waypoint(mFdMission);
            waypoint.setAltitude(mDesiredAltitude);
            waypoint.setPosition(targetPosition);
            doesCollide = waypoint.doesCollide();
            // Double the distance for the next iteration
            targetDistance += originalDistance;
        }
        if (doesCollide)
            // Failed to find a non-colliding position with current FD parameters set by user
            return null;
        return waypoint;
    }

    /**
     * Creates "altitude hold" waypoint with specific distance (based on speed) from the passed
     * position.
     * 
     * @param basePosition position which the new waypoint position calculation is based on
     * @return the waypoint
     */
    private Waypoint createAltHoldWp(LatLng basePosition) {
        // Create consecutive waypoint with distance set to reach in empirically chosen 15 seconds
        // from the base position
        double altitudeHoldDistance =
                ALT_HOLD_WP_DIST_IN_SEC * Vehicle.get().getSpeed().getGroundspeed();
        Waypoint altitudeHoldWp = createWp(basePosition, altitudeHoldDistance);
        return altitudeHoldWp;
    }

    /**
     * Once the vehicle starts moving towards last FD waypoint, new one needs to be created.
     */
    private void updateAltHold() {
        // Get index of waypoint the vehicle is currently moving towards
        int currentWp = Mission.get().getCurrentWpIndexInAutopilot();
        int fdMissionSize = mFdMission.getMissionItems().size();
        if (currentWp == fdMissionSize) {
            SkyControlUtils.log("Heading to the last FD waypoint. Generating new mission.\n", true);
            // Vehicle is heading to the last of the FD waypoints -> generate new one
            LatLng lastWpPosition =
                    ((NavMissionItem) mFdMission.getMissionItemOnIndex(Mission.get()
                            .getCurrentWpIndexInApp())).getPosition();
            Waypoint altitudeHoldWp = createAltHoldWp(lastWpPosition);
            if (notCreated(altitudeHoldWp))
                return;
            // Remove previous waypoints
            mFdMission.clearMission();
            mFdMission.addMissionItem(mSpeedCmd);
            List<LatLng> waypoints =
                    getCollisionFreePath(lastWpPosition, altitudeHoldWp.getPosition());
            if (notCreated(waypoints))
                return;
            // Put the waypoints right behind the speed command which is on index 0
            mFdMission.addWaypointsOnIndex(waypoints, mDesiredAltitude, 1);
            Mission.get().sendMissionToVehicle(mFdMission);
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
            case CURRENT_MISSION_ITEM:
                if (sFdState != FdState.FD_ENGAGED)
                    break;
                updateAltHold();
                break;
            case MISSION_WRITTEN:
                if (sFdState != FdState.FD_IDLE) {
                    // FD_ENGAGE_REQUESTED or FD_ENGADED
                    // Set the current mission item to first one in the mission (the do change speed
                    // command)
                    MavLinkMission.sendSetCurrentMissionItem(MavLinkMission.RESTART_MISSION);
                    if (Vehicle.get().getState().getMode() == ApmModes.FIXED_WING_AUTO) {
                        if (sFdState != FdState.FD_ENGAGED)
                            // When flight mode AUTO was already set then onchange notification
                            // would
                            // never come, so setting FD_ENGAGED on here directly
                            setFdEngaged();
                    } else
                        MavLinkFlightMode.changeFlightMode(ApmModes.FIXED_WING_AUTO);
                }
                break;
            case MISSION_WRITE_FAILED:
                if (sFdState == FdState.FD_ENGAGE_REQUESTED) {
                    setFdDisengaged(false);
                    SkyControlUtils.toast("Communication error. Could not engage FD",
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
                        if (sFdState == FdState.FD_ENGAGE_REQUESTED)
                            setFdEngaged();
                        break;
                    default:
                        if (sFdState != FdState.FD_IDLE)
                            setFdDisengaged(false);
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionEvent(
            com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                if (sFdState != FdState.FD_IDLE)
                    setFdDisengaged(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCollisionEvent(CollisionEvent event) {
        switch (event) {
            case DANGER_OF_COLLISION:
                if (sFdState != FdState.FD_IDLE)
                    disengageFlightDirector(true);
                break;
            default:
                break;
        }
    }

    public void setHeading(int heading) {
        mDesiredHeading = heading;
        updateFdMission();
    }

    public void setAltitude(int altitude) {
        mDesiredAltitude = altitude;
        updateFdMission();
    }

    public void setAirspeed(int airspeed) {
        mDesiredAirspeed = airspeed;
        updateFdMission();
    }

    public void setVertSpeed(int vertSpeed) {
        mDesiredVertSpeed = vertSpeed;
        updateFdMission();
    }

    public int getHeading() {
        return mDesiredHeading;
    }

    public int getAltitude() {
        return mDesiredAltitude;
    }

    public int getAirspeed() {
        return mDesiredAirspeed;
    }

    public int getVertSpeed() {
        return mDesiredVertSpeed;
    }

    public MissionItemList getFdMission() {
        return mFdMission;
    }
}
