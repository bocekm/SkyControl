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

import com.MAVLink.Messages.ardupilotmega.msg_mission_ack;
import com.MAVLink.Messages.ardupilotmega.msg_mission_count;
import com.MAVLink.Messages.ardupilotmega.msg_mission_request;
import com.MAVLink.Messages.ardupilotmega.msg_mission_request_list;
import com.MAVLink.Messages.ardupilotmega.msg_mission_set_current;
import com.MAVLink.Messages.enums.MAV_MISSION_RESULT;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * Allows sending MavLink messages related to mission of the vehicle.
 */
public class MavLinkMission {

    /**
     * Using this index in {@link MavLinkMission#sendSetCurrentMissionItem(int)} means restarting
     * the mission from the beginning.
     */
    public static final int RESTART_MISSION = 0;

    /**
     * Send acknowledge that all mission items have been received.
     */
    public static void sendAck() {
        msg_mission_ack msg = new msg_mission_ack();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();;
        msg.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

    /**
     * Request one mission item from the requested mission item list.
     * 
     * @param seq sequence number of the requested mission item
     */
    public static void requestMissionItem(int seq) {
        msg_mission_request msg = new msg_mission_request();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();;
        msg.seq = (short) seq;
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

    /**
     * Request a list of all vehicle mission items.
     */
    public static void requestMissionItemList() {
        msg_mission_request_list msg = new msg_mission_request_list();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();;
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

    /**
     * Send number of mission items to vehicle. That initiates exchange of mission items with the
     * vehicle.
     * 
     * @param count the count
     */
    public static void sendMissionItemCount(int count) {
        msg_mission_count msg = new msg_mission_count();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();;
        msg.count = (short) count;
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

    /**
     * Send set current mission item.
     * 
     * @param i index of the mission item in the mission; use {@link MavLinkMission#RESTART_MISSION}
     *        to start the mission from beginning
     */
    public static void sendSetCurrentMissionItem(int i) {
        if(Vehicle.get().getPosition().isFakeGpsEnabled())
            Mission.get().setCurrentWpIndex(i > 0 ? i : 1);
        msg_mission_set_current msg = new msg_mission_set_current();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();;
        msg.seq = (short) i;
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

}
