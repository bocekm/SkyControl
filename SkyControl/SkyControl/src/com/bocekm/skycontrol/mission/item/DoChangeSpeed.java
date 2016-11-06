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
import com.MAVLink.Messages.enums.MAV_CMD;
import com.MAVLink.Messages.enums.MAV_FRAME;
import com.bocekm.skycontrol.mission.MissionItemList;

/**
 * Packs the mission command for changing the airspeed of vehicle.
 */
public class DoChangeSpeed extends MissionItem {

    /** Desired airspeed. */
    private float mSpeed = 0.0f;

    /**
     * {@link DoChangeSpeed} constructor.
     * 
     * @param list mission item list this {@link DoChangeSpeed} command instance would belong to
     */
    public DoChangeSpeed(MissionItemList list) {
        super(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.mission.MissionItem#packMissionItem()
     */
    @Override
    public msg_mission_item packMissionItem() {
        msg_mission_item msg = super.packMissionItem();
        msg.command = MAV_CMD.MAV_CMD_DO_CHANGE_SPEED;
        msg.frame = MAV_FRAME.MAV_FRAME_MISSION;
        msg.param1 = 0.0f; // Speed type (0=Airspeed, 1=Ground Speed)
        msg.param2 = mSpeed; // Speed (m/s, -1 indicates no change)
        msg.param3 = -1; // Throttle ( Percent, -1 indicates no change)
        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionItem#unpackMavMessage(com.MAVLink.Messages.ardupilotmega
     * .msg_mission_item)
     */
    @Override
    public void unpackMavMessage(msg_mission_item msg) {
        mSpeed = msg.param2;
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.DO_CHANGE_SPEED;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }
}
