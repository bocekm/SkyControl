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

import com.MAVLink.Messages.ardupilotmega.msg_request_data_stream;
import com.MAVLink.Messages.enums.MAV_DATA_STREAM;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * This class requests from vehicle sending various data streams, like global position,
 * attitude, speed, etc.
 */
public class MavLinkStreamRates {

    /**
     * Setup data streams with rate (in Hz) of sending for each data stream.
     * 
     * @param extendedStatus requested rate of sending the extended status messages
     * @param extra1 requested rate of sending the extra 1 messages
     * @param extra2 requested rate of sending the extra 2 messages
     * @param extra3 requested rate of sending the extra 3 messages
     * @param position requested rate of sending the global position messages
     * @param rawSensors requested rate of sending the raw sensor messages
     */
    public static void setupStreamRates(int extendedStatus, int extra1,
            int extra2, int extra3, int position, int rawSensors) {
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_EXTENDED_STATUS,
                extendedStatus);
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_EXTRA1, extra1);
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_EXTRA2, extra2);
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_EXTRA3, extra3);
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_POSITION, position);
        requestMavlinkDataStream(MAV_DATA_STREAM.MAV_DATA_STREAM_RAW_SENSORS, rawSensors);
    }

    /**
     * Send MavLink packet with request for one of the MavLink data streams.
     * 
     * @param streamId identification of the requested data, one of MAV_DATA_STREAM items
     * @param rate frequency in Hz the requested data shall be sent by vehicle with
     */
    private static void requestMavlinkDataStream(int streamId, int rate) {
        msg_request_data_stream msg = new msg_request_data_stream();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();

        msg.req_message_rate = (short) rate;
        msg.req_stream_id = (byte) streamId;

        if (rate > 0) {
            msg.start_stop = 1;
        } else {
            msg.start_stop = 0;
        }
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }
}
