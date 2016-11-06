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

import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.enums.MAV_CMD;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.item.MissionItem;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.mission.item.Waypoint;
import com.google.android.gms.maps.model.LatLng;

/**
 * {@link Mission} class is a singleton pattern class which lives from the start of the application
 * till the end. Is responsible for handling the vehicle mission.
 */
public class Mission implements
        MissionListener {

    /** This {@link Mission} class instance. */
    private static Mission sMission;

    /** Handles the communication of mission to and from the vehicle. */
    private MissionManager mMissionMananger;

    /** Mission events manager. */
    private MissionEvents mMissionEvents;

    /** Stores information coming from vehicle navigation controller. */
    private NavigationController mNavigationController;

    /** Reference to instantiated Flight Director class. */
    private FlightDirector mFlightDirector;

    /** Reference to instantiated Waypoint Follower class. */
    private WaypointFollower mWaypointFollower;

    /** Object holding registered mission items. */
    private MissionItemList mMissionItemList;

    /** Indicates that no specific waypoint is to be used. */
    public static final int NO_WAYPOINT = -1;

    /** Waypoint towards which the vehicle currently moves. */
    private int mCurrentWp = NO_WAYPOINT;

    /**
     * Constructor of Mission class (private because it's a singleton).
     */
    private Mission() {
        mMissionEvents = new MissionEvents();
        mMissionMananger = new MissionManager();
        mNavigationController = new NavigationController(mMissionEvents);
        mFlightDirector = new FlightDirector(mMissionEvents);
        mWaypointFollower = new WaypointFollower(mMissionEvents);
        mMissionItemList = new MissionItemList();
        // The mission listener is added but not removed because this instance lives as long as the
        // app lives
        mMissionEvents.addMissionListener(this);
    }

    /**
     * Instantiates new {@link Mission} object only if it wasn't instantiated before, because it's
     * singleton so just one instance exists per running app.
     * 
     * @return instance of the {@link Mission} singleton
     */
    public static Mission init() {
        if (sMission == null)
            sMission = new Mission();
        return sMission;
    }

    /**
     * Returns the only instance of {@link Mission} singleton.
     * 
     * @return reference to {@link Mission} singleton instance
     */
    public static Mission get() {
        return sMission;
    }

    /**
     * Gets the index of mission item in the navigation mission items list.
     * 
     * @param item the navigation mission item
     * @return index of the mission item in a list
     */
    public int getNavMissionItemIndex(NavMissionItem item) {
        return mMissionItemList.getNavMissionItemIndex(item);
    }

    /**
     * Gets the navigation mission item on specific position of navigation mission items list.
     * 
     * @param index the index to the navigation mission items list
     * @return index of the navigation mission item in a list
     */
    public NavMissionItem getNavMissionItemOnIndex(int index) {
        return mMissionItemList.getNavMissionItemOnIndex(index);
    }

    /**
     * Gets the mission item on specific position of mission items list.
     * 
     * @param index the index to the mission items list
     * @return index of the mission item in a list
     */
    public MissionItem getMissionItemOnIndex(int index) {
        return mMissionItemList.getMissionItemOnIndex(index);
    }

    /**
     * Gets the index of mission item in the mission items list.
     * 
     * @param item the mission item
     * @return index of the mission item in a list
     */
    public int getMissionItemIndex(MissionItem item) {
        return mMissionItemList.getMissionItemIndex(item);
    }

    /**
     * Adds mission item at the end of the list of existing mission items.
     * 
     * @param missionItem mission item to add to mission
     */
    public void addMissionItem(MissionItem missionItem) {
        mMissionItemList.addMissionItem(missionItem);
        Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_UPDATE);
        if (missionItem instanceof NavMissionItem)
            Mission.get().getEvents().onMissionEvent(MissionEvent.WAYPOINT_ADDED);
    }

    /**
     * Injects mission items into a current list of mission items on specific index.
     * 
     * @param missionItems the mission items to be added
     * @param index index into the existing list of mission items
     */
    public void addMissionItemsOnIndex(List<MissionItem> missionItems, int index) {
        mMissionItemList.addMissionItemsOnIndex(missionItems, index);
        Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_UPDATE);
    }

    /**
     * Injects waypoints into a current list of mission items on specific index.
     * 
     * @param positions positions of the waypoints to be added
     * @param altitude altitude of the waypoints, the same for all
     * @param index index into the existing list of mission items
     */
    public void addWaypointsOnIndex(List<LatLng> positions, int altitude, int index) {
        mMissionItemList.addWaypointsOnIndex(positions, altitude, index);
        Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_UPDATE);
    }

    /**
     * Removes mission item from a mission.
     * 
     * @param missionItem mission item to remove from a mission
     */
    public void removeMissionItem(MissionItem missionItem) {
        mMissionItemList.removeMissionItem(missionItem);
        Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_UPDATE);
    }

    /**
     * Convert MavLink mission item messages to list of {@link MissionItem}s.
     * 
     * @param msgs list of MavLink mission item messages
     * @return the list of {@link MissionItem} objects
     */
    private List<MissionItem> processMavLinkMessages(List<msg_mission_item> msgs) {
        List<MissionItem> receivedMission = new ArrayList<MissionItem>();

        for (msg_mission_item msg : msgs) {
            switch (msg.command) {
                case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
                    Waypoint wp = new Waypoint(mMissionItemList);
                    wp.unpackMavMessage(msg);
                    receivedMission.add(wp);
                    break;
                default:
                    break;
            }
        }
        return receivedMission;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionEvents.MissionListener#onMissionEvent(com.bocekm.skycontrol
     * .mission.MissionEvents.MissionEvent)
     */
    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case MISSION_RECEIVED:
                List<msg_mission_item> msgs = mMissionMananger.getMissionItemMsgs();
                if (msgs == null)
                    break;
                // First waypoint from vehicle is always vehicle AHRS home, we don't need it
                msgs.remove(0);
                mMissionItemList.replaceMissionItemList(processMavLinkMessages(msgs));
                Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_UPDATE);
                break;
            default:
                break;
        }
    }

    /**
     * Sends the mission to the vehicle.
     * 
     * @param missionItems list of the mission items
     */
    public void sendMissionToVehicle(MissionItemList missionItems) {
        List<msg_mission_item> missionItemMsgs = new ArrayList<msg_mission_item>();
        // Dummy waypoint added as it gets discarded by APM (replaced by vehicle AHRS home)
        missionItemMsgs.add(new Waypoint(missionItems).setAltitude(0).setPosition(new LatLng(0, 0))
                .packMissionItem());
        for (MissionItem item : missionItems.getMissionItems()) {
            missionItemMsgs.add(item.packMissionItem());
        }
        mMissionMananger.sendMission(missionItemMsgs);
    }

    public void sendMissionToVehicle() {
        sendMissionToVehicle(mMissionItemList);
    }

    public void receiveMissionFromVehicle() {
        mMissionMananger.receiveMission();
    }

    public MissionEvents getEvents() {
        return mMissionEvents;
    }

    public MissionManager getMissionMananger() {
        return mMissionMananger;
    }

    public NavigationController getNavController() {
        return mNavigationController;
    }

    public List<MissionItem> getMissionItems() {
        return mMissionItemList.getMissionItems();
    }

    public List<NavMissionItem> getNavMissionItems() {
        return mMissionItemList.getNavMissionItems();
    }

    public FlightDirector getFlightDirector() {
        return mFlightDirector;
    }

    public NavMissionItem getCurrentWp() {
        // APM sends current wp messages just for navigation commands not for do commands so we can
        // typecast the mission item to NavMissionItem
        return (NavMissionItem) mMissionItemList.getMissionItemOnIndex(mCurrentWp - 1);
    }

    /**
     * Index of the current waypoint in mission from the autopilot standpoint, for which the first
     * mission item has index 1.
     * 
     * @return 1-based index of the current wp
     */
    public int getCurrentWpIndexInAutopilot() {
        return mCurrentWp;
    }

    /**
     * Index of the current waypoint in mission as it is stored in the application, where first
     * mission item has index 0.
     * 
     * @return 0-based index of the current wp
     */
    public int getCurrentWpIndexInApp() {
        return mCurrentWp - 1;
    }

    public void setCurrentWpIndex(int currentWp) {
        if (mCurrentWp != currentWp) {
            mCurrentWp = currentWp;
            Mission.get().getEvents().onMissionEvent(MissionEvent.CURRENT_MISSION_ITEM);
        }
    }

    public MissionItemList getMissionItemList() {
        return mMissionItemList;
    }

    public WaypointFollower getWaypointFollower() {
        return mWaypointFollower;
    }
}
