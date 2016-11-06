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
package com.bocekm.skycontrol.mission.item;

import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.enums.MAV_COMPONENT;
import com.bocekm.skycontrol.mission.MissionItemList;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * Collects methods and variables common to all mission items, be that navigation or command mission
 * item.
 */
public abstract class MissionItem {

    /**
     * Supported mission item types.
     */
    public enum MissionItemType {

        /** Navigation waypoint. */
        WAYPOINT,

        /** Speed change request command. */
        DO_CHANGE_SPEED,

        /** Set home command. */
        DO_SET_HOME
    }

    /** Mission item list this mission item is registered in. */
    protected MissionItemList mMissionItemList;

    /**
     * {@link MissionItem} constructor.
     * 
     * @param list the mission item list this mission item will belong to
     */
    public MissionItem(MissionItemList list) {
        mMissionItemList = list;
    }

    /**
     * Returns a new MavLink mission item message representing this {@link MissionItem} object.
     * 
     * @return new mission item MavLink message
     */
    public msg_mission_item packMissionItem() {
        msg_mission_item msg = new msg_mission_item();
        msg.target_component = (byte) MAV_COMPONENT.MAV_COMP_ID_ALL;
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();
        return msg;
    };

    /**
     * Gets data from MavLink mission item message.
     * 
     * @param mavMsg the MavLink message
     */
    public abstract void unpackMavMessage(msg_mission_item mavMsg);

    /**
     * Gets the type of mission item.
     * 
     * @return the one of {@link MissionItemType} enum items
     */
    public abstract MissionItemType getType();

    public MissionItemList getMissionItemList() {
        return mMissionItemList;
    }

    /**
     * Index of the mission item in mission from the application standpoint, where first mission
     * item has index 0.
     * 
     * @return 0-based index of the mission item
     */
    public int getIndexInAppMission() {
        return mMissionItemList.getMissionItemIndex(this);
    };

    /**
     * Index of the mission item in mission from the autopilot standpoint, for which the first
     * mission item has index 1.
     * 
     * @return 1-based index of the mission item
     */
    public int getIndexInAutopilotMission() {
        return mMissionItemList.getMissionItemIndex(this) + 1;
    };
}
