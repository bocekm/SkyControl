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

import android.widget.Toast;

import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.mission.MissionItemList;
import com.google.android.gms.maps.model.LatLng;

/**
 * Specialization of mission item, related to navigation mission items (currently just waypoints)
 * only.
 */
public abstract class NavMissionItem extends MissionItem {

    /** The horizontal position. */
    protected LatLng mPosition = null;

    /** Says whether the position of the nav item collides with terrain/obstacle. */
    protected boolean mCollides = false;

    /** The vertical position. For navigation purposes int is enough. */
    protected int mAltitude = -1;

    public NavMissionItem(MissionItemList list) {
        super(list);
    }

    /**
     * Called by list adapter to acquire title to display in mission item list.
     * 
     * @return the title
     */
    public abstract String getTitle();

    /**
     * Called by list adapter to acquire subtitle to display in mission item list.
     * 
     * @return the subtitle
     */
    public abstract String getSubTitle();

    public LatLng getPosition() {
        return mPosition;
    }

    public NavMissionItem setPosition(LatLng position) {
        mPosition = position;
        checkForCollision();
        return this;
    }

    public int getAltitude() {
        return mAltitude;
    }

    public NavMissionItem setAltitude(int altitude) {
        mAltitude = altitude;
        checkForCollision();
        return this;
    }

    /**
     * Gets index of the mission item in the list of navigation mission items.
     * 
     * @return index to the navigation mission list
     */
    public int getIndexInNavMission() {
        return mMissionItemList.getNavMissionItemIndex(this);
    };

    public boolean doesCollide() {
        return mCollides;
    }

    public void collides(boolean doesCollide) {
        mCollides = doesCollide;
    }

    /**
     * Checks whether the waypoint does not collide with terrain/obstacle
     */
    private void checkForCollision() {
        if (CollisionAvoidance.get().isCasEnabled() && mPosition != null && mAltitude >= 0) {
            mCollides = CollisionAvoidance.checkForCollision(mPosition, mAltitude);
        }
    }

    /**
     * Toast message to user on waypoint collision.
     */
    public void toastOnCollision() {
        if (mCollides && CollisionAvoidance.get().isCasEnabled()) {
            SkyControlUtils.toast("Waypoint collides with terrain/obstacle", Toast.LENGTH_SHORT);
            SkyControlUtils.log("Waypoint collides with terrain/obstacle\n", true);
        }
    }
}
