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
import com.google.android.gms.maps.model.LatLng;

/**
 * Packs the mission command for changing the airspeed of vehicle.
 */
public class DoSetHome extends MissionItem {
    
    private LatLng mPosition; 
    private float mAltitude; 

    public DoSetHome setAltitude(float altitude) {
        mAltitude = altitude;
        return this;
    }

    public DoSetHome setPosition(LatLng position) {
        mPosition = position;
        return this;
    }

    /**
     * {@link DoSetHome} constructor.
     * 
     * @param list mission item list this {@link DoSetHome} command instance would belong to
     */
    public DoSetHome(MissionItemList list) {
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
        msg.command = MAV_CMD.MAV_CMD_DO_SET_HOME;
        msg.frame = MAV_FRAME.MAV_FRAME_MISSION;
        msg.param1 = 0.0f; // Use current (1=use current location, 0=use specified location)
        msg.x = (float)mPosition.latitude;
        msg.y = (float)mPosition.longitude;
        msg.z = mAltitude;
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
        
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.DO_SET_HOME;
    }
}
