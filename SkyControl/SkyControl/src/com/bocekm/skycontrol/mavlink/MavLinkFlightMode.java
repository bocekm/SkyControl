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

import com.MAVLink.Messages.ApmModes;
import com.MAVLink.Messages.ardupilotmega.msg_set_mode;
import com.MAVLink.Messages.enums.MAV_MODE_FLAG;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * Contains method for changing the vehicle autopilot flight mode.
 */
public class MavLinkFlightMode {

	/**
	 * Change flight mode of the autopilot.
	 *
	 * @param mode one of the {@link ApmModes}
	 */
	public static void changeFlightMode(ApmModes mode) {
		msg_set_mode msg = new msg_set_mode();
		msg.target_system = (byte) Vehicle.get().getHeartbeat().getVehicleSysId();
		msg.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED;
		msg.custom_mode = mode.getNumber();
		Connection.get().getMavLinkClient().sendMavPacket(msg.pack());
	}
}
