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

import com.MAVLink.Messages.ardupilotmega.msg_param_set;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleParameter;

/**
 * Sets autopilot parameters.
 */
public class MavLinkParameters {
    
    /**
     * Send parameter to autopilot to be set.
     *
     * @param parameter the parameter to be set
     */
    public static void sendParameter(VehicleParameter parameter) {
        msg_param_set msg = new msg_param_set();
        msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();
        msg.setParam_Id(parameter.getName());
        msg.param_type = (byte) parameter.getType();
        msg.param_value = (float) parameter.getValue();
        Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
    }

}
