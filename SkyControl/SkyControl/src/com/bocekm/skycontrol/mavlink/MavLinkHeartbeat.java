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

import com.MAVLink.Messages.MAVLinkPacket;
import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.enums.MAV_AUTOPILOT;
import com.MAVLink.Messages.enums.MAV_MODE_FLAG;
import com.MAVLink.Messages.enums.MAV_STATE;
import com.MAVLink.Messages.enums.MAV_TYPE;
import com.bocekm.skycontrol.connection.Connection;

/**
 * {@link MavLinkHeartbeat} sends heartbeat messages to a vehicle with specific period.
 */
public class MavLinkHeartbeat {

    /**
     * Heartbeat message to be packed to form a MavLink packet.
     */
    private static final msg_heartbeat sMsg = new msg_heartbeat();
    static {
        sMsg.type = MAV_TYPE.MAV_TYPE_GCS;
        sMsg.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
        sMsg.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED;
        sMsg.custom_mode = 0;
        sMsg.system_status = MAV_STATE.MAV_STATE_ACTIVE;
    }

    /**
     * Heartbeat MavLink packet. After sending into airspace, it's purpose is to check that
     * autopilot equipped vehicle is present and is responding.
     */
    private static final MAVLinkPacket sMsgPacket = sMsg.pack();

    /**
     * Sends the heartbeat to the air, expecting response if any vehicle is in range.
     */
    public static void sendHeartbeat() {
        Connection.get().getMavLinkClient().sendMavPacket(sMsgPacket);
    }

}
