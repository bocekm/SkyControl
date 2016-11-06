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

import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;

/**
 * {@link LogHelper} class logs text based on events from various application listeners.
 */
public class LogHelper implements
        VehicleListener,
        ConnectionListener,
        MissionListener,
        CollisionListener {

    /**
     * Class constructor. Registers listeners. Be sure to call {@link LogHelper#destroyLogHelper()}
     * on Activity onDestroy event.
     * 
     * @param activity parent {@link MainActivity}
     */
    public LogHelper(MainActivity activity) {
        registerListeners();
    }

    /**
     * Registers appropriate listeners.
     */
    public void registerListeners() {
        Vehicle.get().getEvents().addVehicleListener(this);
        Connection.get().getEvents().addConnectionListener(this);
        Mission.get().getEvents().addMissionListener(this);
    }

    /**
     * Removes registered listeners and removed reference to {@link MainActivity} (important for
     * proper garbage collection).
     */
    public void destroyLogHelper() {
        Vehicle.get().getEvents().removeVehicleListener(this);
        Connection.get().getEvents().removeConnectionListener(this);
        Mission.get().getEvents().removeMissionListener(this);
    }

    /*
     * (non-Javadoc) Log appropriate text based on Vehicle event type.
     * 
     * @see
     * com.bocekm.skycontrol.vehicle.VehicleInterfaces.OnVehicleListener#onVehicleEvent(com.bocekm
     * .skycontrol.vehicle.VehicleEvents.VehicleEventTypes)
     */
    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case VEHICLE_CONNECTED:
                SkyControlUtils.log("Vehicle with ID "
                        + Vehicle.get().getHeartbeat().getVehicleSysId() + " connected\n", true);
                break;
            case HEARTBEAT_PERIODIC:
                SkyControlUtils.log("Heartbeat acquired\n", false);
                break;
            case HEARTBEAT_TIMEOUT:
                SkyControlUtils.log("Heartbeat timeout\n", true);
                break;
            case HEARTBEAT_RESTORED:
                SkyControlUtils.log("Heartbeat restored\n", true);
                break;
            case ATTITUDE:
                SkyControlUtils.log("Attitude:\nRoll: "
                        + Vehicle.get().getAttitude().getRollInDegrees() + "\nPitch: "
                        + Vehicle.get().getAttitude().getPitchInDegrees() + "\nYaw: "
                        + Vehicle.get().getAttitude().getYawInDegrees() + "\n", false);
                break;
            case SPEED_ALTITUDE:
                SkyControlUtils.log("Speed/altitude:\nAlt: "
                        + Vehicle.get().getAltitude().getAltitude() + "\nAirspeed: "
                        + Vehicle.get().getSpeed().getAirspeed() + "\nGndspeed: "
                        + Vehicle.get().getSpeed().getGroundspeed() + "\nVertspeed: "
                        + Vehicle.get().getSpeed().getVerticalSpeed() + "\n", false);
                break;
            case GPS_COUNT:
                SkyControlUtils.log("GPS sats visible: "
                        + Vehicle.get().getPosition().getSatCount() + "\n", false);
                break;
            case GPS_FIX:
                SkyControlUtils.log("Vehicle position fixed: "
                        + Vehicle.get().getPosition().getFixType().name() + "\n", true);
                break;
            case POSITION:
                SkyControlUtils.log(
                        "GPS position: "
                                + SkyControlUtils.locationToString(Vehicle.get().getPosition()
                                        .getPosition()) + "\n", false);
                break;
            case ARMED:
                if (Vehicle.get().getState().isArmed())
                    SkyControlUtils.log("Autopilot armed\n", true);
                else
                    SkyControlUtils.log("Autopilot disarmed\n", true);
                break;
            case FAILSAFE:
                if (Vehicle.get().getState().isFailsafe())
                    SkyControlUtils.log("Failsafe mode activated\n", true);
                else
                    SkyControlUtils.log("Failsafe mode deactivated\n", true);
                break;
            case FLIGHT_MODE:
                SkyControlUtils.log("Flight mode: " + Vehicle.get().getState().getMode().getName()
                        + "\n", true);
                break;
            case TYPE:
                SkyControlUtils.log("Vehicle type: " + Vehicle.get().getType().getType() + "\n",
                        true);
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc) Log appropriate text based on Connection event type.
     * 
     * @see com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener#onConnectionEvent(
     * com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent)
     */
    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_BOUND:
                SkyControlUtils.log("Telemetry connected\n", true);
                break;
            case SERVICE_UNBOUND:
                SkyControlUtils.log("Telemetry disconnected\n", true);
                break;
            case TELEMETRY:
                // SkyControlUtils.log("Telemetry params received\n", false);
                break;
            case STATUS_RECEIVED:
                SkyControlUtils.log("Vehicle status received: " + Connection.get().getStatusText()
                        + "\n", true);
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc) Log appropriate text based on Mission event type.
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionEvents.MissionListener#onMissionEvent(com.bocekm.skycontrol
     * .mission.MissionEvents.MissionEvent)
     */
    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case RESPONSE_TIMEOUT:
                SkyControlUtils.log("Mission response timed out\n", true);
                break;
            case MISSION_RECEIVED:
                SkyControlUtils.log("Mission received from vehicle with "
                        + Mission.get().getMissionItems().size() + " items\n", true);
                break;
            case MISSION_WRITTEN:
                SkyControlUtils.log("Mission written to vehicle\n", true);
                break;
            case NAV_CONTROLLER:
                SkyControlUtils.log("Nav controller:\ndist to wpt: "
                        + Mission.get().getNavController().getDistToWaypoint() + "\nheading: "
                        + Mission.get().getNavController().getNavHeading() + "\nbearing: "
                        + Mission.get().getNavController().getTargetBearing() + "\nalt diff: "
                        + Mission.get().getNavController().getCurrToTargAltDiff()
                        + "\nspeed diff: "
                        + Mission.get().getNavController().getCurrToTargAsdpDiff() + "\n", false);
                break;
            case CURRENT_MISSION_ITEM:
                SkyControlUtils.log(
                        "Current waypoint: " + Mission.get().getCurrentWpIndexInAutopilot() + "\n", true);
                break;
            case FD_ENGAGED:
                SkyControlUtils.log("Flight Director engaged\n", false);
                break;
            case FD_DISENGAGED:
                SkyControlUtils.log("Flight Director disengaged\n", false);
                break;
            case WPF_ENGAGED:
                SkyControlUtils.log("Waypoint Follower engaged\n", false);
                break;
            case WPF_DISENGAGED:
                SkyControlUtils.log("Waypoint Follower disengaged\n", false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCollisionEvent(CollisionEvent event) {
        switch (event) {
            case CLEAR_OF_COLLISION:
                SkyControlUtils.log("Clear of collision\n", true);
                break;
            case DANGER_OF_COLLISION:
                SkyControlUtils.log("Danger of collision\n", true);
                break;
            case OBSTACLES_LOADED:
                SkyControlUtils.log("Obstacles loaded\n", true);
                break;
            default:
                break;
        }
        
    }
}
