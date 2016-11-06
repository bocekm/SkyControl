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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.enums.MAV_CMD;
import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.mission.MissionItemList;
import com.google.android.gms.maps.model.LatLng;

/**
 * Stores information about mission waypoint, like position and altitude.
 */
public class Waypoint extends NavMissionItem {

    /**
     * Instantiates a new {@link Waypoint}.
     * 
     * @param list mission item list this waypoint will be registered to
     */
    public Waypoint(MissionItemList list) {
        super(list);
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.WAYPOINT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionItem#unpackMavMessage(com.MAVLink.Messages.ardupilotmega
     * .msg_mission_item)
     */
    @Override
    public void unpackMavMessage(msg_mission_item mavMsg) {
        mPosition = new LatLng(mavMsg.x, mavMsg.y);
        mAltitude = (int) mavMsg.z;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.mission.MissionItem#packMissionItem()
     */
    @Override
    public msg_mission_item packMissionItem() {
        msg_mission_item mavMsg = super.packMissionItem();
        mavMsg.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
        mavMsg.x = (float) mPosition.latitude;
        mavMsg.y = (float) mPosition.longitude;
        mavMsg.z = mAltitude;
        return mavMsg;
    }

    public void setDefaultAltitude() {
        // Get default waypoint altitude from user preferences
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        mAltitude = userPref.getInt(PreferencesFragment.DEFAULT_WAYPOINT_ALT_PREF_KEY, 0);
        boolean agl = userPref.getBoolean(PreferencesFragment.DEFAULT_ALT_ABOVE_GND_PREF_KEY, true);
        if (CollisionAvoidance.get().isCasEnabled() && mPosition != null && agl) {
            int elev = CollisionAvoidance.get().getElevationModel().getElevation(mPosition);
            if (elev > -1)
                mAltitude += elev;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.mission.MissionItem#getTitle()
     */
    @Override
    public String getTitle() {
        return new String("Waypoint " + (getIndexInNavMission() + 1));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.mission.MissionItem#getSubTitle()
     */
    @Override
    public String getSubTitle() {
        return new String("Altitude: " + String.valueOf(mAltitude) + " m");
    }
}
