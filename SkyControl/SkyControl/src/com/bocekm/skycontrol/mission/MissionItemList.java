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
package com.bocekm.skycontrol.mission;

import java.util.ArrayList;
import java.util.List;

import com.bocekm.skycontrol.mission.item.MissionItem;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.mission.item.Waypoint;
import com.google.android.gms.maps.model.LatLng;

/**
 * Represents a list of mission items.
 */
public class MissionItemList {

    /** Stores the set of mission items belonging to this mission. */
    private List<MissionItem> mMissionItems = new ArrayList<MissionItem>();

    /**
     * Stores the set of navigation mission items (subset of all mission items) belonging to this
     * mission.
     */
    private List<NavMissionItem> mNavMissionItems = new ArrayList<NavMissionItem>();

    /**
     * Gets list of mission items of a specific type.
     *
     * @param type type of the mission items to get from the list
     * @param list list of mission items to filter
     * @return the mission items of type
     */
    private <T extends MissionItem> List<T> getMissionItemsOfType(Class<T> type,
            List<MissionItem> list) {
        List<T> result = new ArrayList<T>();

        for (MissionItem item : list) {
            if (type.isAssignableFrom(item.getClass())) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    /**
     * Update list of navigation mission items based on list of mission items.
     */
    public void updateNavMission() {
        mNavMissionItems.clear();
        mNavMissionItems.addAll(getMissionItemsOfType(NavMissionItem.class, mMissionItems));
    }

    /**
     * Remove all mission items.
     */
    public void clearMission() {
        mMissionItems.clear();
        mNavMissionItems.clear();
    }

    /**
     * Gets the index of mission item in the navigation mission items list.
     * 
     * @param item the navigation mission item
     * @return index of the mission item in a list
     */
    public int getNavMissionItemIndex(NavMissionItem item) {
        return mNavMissionItems.indexOf(item);
    }

    /**
     * Gets the navigation mission item on specific position of navigation mission items list.
     * 
     * @param index the index to the navigation mission items list
     * @return index of the navigation mission item in a list
     */
    public NavMissionItem getNavMissionItemOnIndex(int index) {
        if (index >= 0 && index < mNavMissionItems.size())
            return mNavMissionItems.get(index);
        else
            return null;
    }

    /**
     * Gets the mission item on specific position of mission items list.
     * 
     * @param index the index to the mission items list
     * @return index of the mission item in a list
     */
    public MissionItem getMissionItemOnIndex(int index) {
        if (index >= 0 && index < mMissionItems.size())
            return mMissionItems.get(index);
        else
            return null;
    }

    /**
     * Gets the index of mission item in the mission items list.
     * 
     * @param item the mission item
     * @return index of the mission item in a list
     */
    public int getMissionItemIndex(MissionItem item) {
        return mMissionItems.indexOf(item);
    }

    /**
     * Removes mission item from a mission.
     * 
     * @param missionItem mission item to remove from a mission
     */
    public void removeMissionItem(MissionItem missionItem) {
        mMissionItems.remove(missionItem);
        updateNavMission();
    }

    /**
     * Removes mission item from a mission.
     * 
     * @param index index of the mission item to remove
     */
    public void removeMissionItemOnIndex(int index) {
        if (index >= 0 && index < mMissionItems.size()) {
            mMissionItems.remove(index);
            updateNavMission();
        }
    }

    /**
     * Adds mission item to a mission.
     * 
     * @param missionItem mission item to add to mission
     */
    public void addMissionItem(MissionItem missionItem) {
        mMissionItems.add(missionItem);
        updateNavMission();
    }

    /**
     * Injects mission items into a current list of mission items on specific index.
     * 
     * @param missionItems the mission items to be added
     * @param index index into the existing list of mission items
     */
    public void addMissionItemsOnIndex(List<MissionItem> missionItems, int index) {
        if (index < 0 || index > mMissionItems.size() || missionItems == null)
            return;
        for (MissionItem missionItem : missionItems) {
            mMissionItems.add(index, missionItem);
            index++;
        }
        mNavMissionItems.clear();
        mNavMissionItems.addAll(getMissionItemsOfType(NavMissionItem.class, mMissionItems));
    }

    /**
     * Injects waypoints into a current list of mission items on specific index.
     * 
     * @param positions positions of the waypoints to be added
     * @param altitude altitude of the waypoints, the same for all
     * @param index index into the existing list of mission items
     */
    public void addWaypointsOnIndex(List<LatLng> positions, int altitude, int index) {
        List<MissionItem> waypoints = createWaypoints(positions, altitude);
        addMissionItemsOnIndex(waypoints, index);
    }

    /**
     * Creates waypoints from list of positions with given altitude.
     *
     * @param positions the positions
     * @param altitude the altitude
     * @return list of waypoints
     */
    private List<MissionItem> createWaypoints(List<LatLng> positions, int altitude) {
        if (positions == null)
            return null;
        List<MissionItem> waypoints = new ArrayList<MissionItem>();
        for (LatLng position : positions) {
            NavMissionItem wp = new Waypoint(this).setAltitude(altitude).setPosition(position);
            waypoints.add(wp);
        }
        return waypoints;
    }

    /**
     * Removes current mission items and replaces them with a passed list of mission items.
     *
     * @param missionItems list of mission items
     */
    public void replaceMissionItemList(List<MissionItem> missionItems) {
        mMissionItems.clear();
        mMissionItems.addAll(missionItems);
        updateNavMission();
    }

    public List<MissionItem> getMissionItems() {
        return mMissionItems;
    }

    public List<NavMissionItem> getNavMissionItems() {
        return mNavMissionItems;
    }

}
