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

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_attitude;
import com.MAVLink.Messages.ardupilotmega.msg_global_position_int;
import com.MAVLink.Messages.ardupilotmega.msg_gps_raw_int;
import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.ardupilotmega.msg_nav_controller_output;
import com.MAVLink.Messages.ardupilotmega.msg_param_value;
import com.MAVLink.Messages.ardupilotmega.msg_radio;
import com.MAVLink.Messages.ardupilotmega.msg_statustext;
import com.MAVLink.Messages.ardupilotmega.msg_vfr_hud;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * {@link MavLinkMsgHandler} manages the incoming MavLink messages.
 */
public class MavLinkMsgHandler {

    /**
     * Redistribute the incoming MavLink message to appropriate listeners.
     * 
     * @param msg received MavLink message
     */
    public void handleMessage(MAVLinkMessage msg) {
        // Check whether the message is mission related and handle it appropriately
        if (Mission.get().getMissionMananger().handleMessage(msg))
            return;

        switch (msg.msgid) {
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                Vehicle.get().getAttitude().onAttitudeReceived((msg_attitude) msg);
                break;
            case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                Vehicle.get().getState().onSpeedReceived((msg_vfr_hud) msg);
                Vehicle.get().getAltitude().onAltitudeReceived((msg_vfr_hud) msg);
                Vehicle.get().getSpeed().onSpeedReceived((msg_vfr_hud) msg);
                break;
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                Vehicle.get().getHeartbeat().onHeartbeatReceived((msg_heartbeat) msg);
                Vehicle.get().getType().onVehicleTypeReceived((msg_heartbeat) msg);
                Vehicle.get().getState().onStateReceived((msg_heartbeat) msg);
                break;
            case msg_radio.MAVLINK_MSG_ID_RADIO:
                Connection.get().getTelemetry().onTelemetryReceived((msg_radio) msg);
                break;
            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                Vehicle.get().getPosition().onPositionReceived((msg_global_position_int) msg);
                break;
            case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
                Vehicle.get().getPosition().onGpsStateReceived((msg_gps_raw_int) msg);
                break;
            case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
                Connection.get().setStatusText(new String(((msg_statustext) msg).text));
                Connection.get().getEvents().onConnectionEvent(ConnectionEvent.STATUS_RECEIVED);
                break;
            case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
                Mission.get().getNavController()
                        .onNavControlReceived((msg_nav_controller_output) msg);
                break;
            case msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE:
                SkyControlUtils.log("Param " + String.valueOf(((msg_param_value) msg).param_id)
                        + ": " + ((msg_param_value) msg).param_value + " set\n", true);
                break;
            default:
                // SkyControlUtils.log("Unknown message: " + msg.msgid + "\n", true);
                break;
        }
    }
}
