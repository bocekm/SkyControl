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
package com.bocekm.skycontrol.vehicle;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.MAVLink.Messages.enums.MAV_PARAM_TYPE;
import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.mavlink.MavLinkParameters;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;

/**
 * Class managing the vehicle autopilot parameters, currently just setting parameters for failsafe
 * behavior.
 */
public class VehicleParameters implements
        VehicleListener {

    /** Instance of class handling the changes happening to the vehicle. */
    private VehicleEvents mEvents;

    /** The Constant DISABLE_SHORT_FAILSAFE. */
    private static final int DISABLE_SHORT_FAILSAFE = 0;

    /** The Constant LONG_FAILSAFE_RTL. */
    private static final int LONG_FAILSAFE_RTL = 1;

    /**
     * Instantiates a new vehicle parameters.
     * 
     * @param events the events
     */
    public VehicleParameters(VehicleEvents events) {
        mEvents = events;
        mEvents.addVehicleListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener#onVehicleEvent(com.bocekm.skycontrol
     * .vehicle.VehicleEvents.VehicleEvent)
     */
    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case VEHICLE_CONNECTED:
                // On vehicle connection the failsafe actions are set as follows
                // Short failsafe action is disabled
                VehicleParameter parameter =
                        new VehicleParameter().setName("FS_SHORT_ACTN")
                                .setType(MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8)
                                .setValue(DISABLE_SHORT_FAILSAFE);
                MavLinkParameters.sendParameter(parameter);
                // Long failsafe action is set to return to launch
                parameter =
                        new VehicleParameter().setName("FS_LONG_ACTN")
                                .setType(MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8)
                                .setValue(LONG_FAILSAFE_RTL);
                MavLinkParameters.sendParameter(parameter);
                
                SharedPreferences userPref =
                        PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
                int distanceToCheckpointInS = userPref.getInt(PreferencesFragment.CAS_DISTANCE_PREF_KEY, 1);
                
                // Long failsafe timeout is set to half the CAS distance to checkpoint, set by user
                parameter =
                        new VehicleParameter().setName("FS_LONG_TIMEOUT")
                                .setType(MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT8)
                                .setValue(distanceToCheckpointInS/2);
                MavLinkParameters.sendParameter(parameter);
                break;
            default:
                break;
        }
    }
}
