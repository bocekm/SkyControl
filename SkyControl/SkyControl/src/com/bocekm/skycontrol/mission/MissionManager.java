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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_mission_ack;
import com.MAVLink.Messages.ardupilotmega.msg_mission_count;
import com.MAVLink.Messages.ardupilotmega.msg_mission_current;
import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.ardupilotmega.msg_mission_request;
import com.MAVLink.Messages.enums.MAV_CMD;
import com.MAVLink.Messages.enums.MAV_MISSION_RESULT;
import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.mavlink.MavLinkMission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;

/**
 * Class to manage receiving or sending mission items to and from the vehicle.
 */
public class MissionManager implements
        VehicleListener {

    /**
     * States of the {@link MissionManager} finite state machine.
     */
    enum MissionManagerStates {

        /** No request to read or write mission in the moment. */
        IDLE,
        /** Current mission of the vehicle is requested. */
        READ_REQUEST,
        /** Reading of requested mission items. */
        READING_MI,
        /** Write of the mission to vehicle is requested. */
        WRITE_REQUEST,
        /** Writing of the mission items to vehicle. */
        WRITING_MI,
        /**
         * Mission items sent. Waiting for the acknowledge from the vehicle that it received them
         * all.
         */
        WAITING_WRITE_ACK
    }

    /** Number of mission items to be received. */
    private short mMissionItemCount;

    /** Sequence number of the mission item being currently written. */
    private int mWriteMissionItemSeq;

    /** Maximum number of retrying to send request for which the response was not received. */
    private static final short sMaxRetries = 5;

    /** Default timeout for receiving result of the {@link MissionManager} request from vehicle. */
    private static final short sDefaultTimoutInMs = 2000;

    /** Current state of the {@link MissionManager} finite state machine. */
    private MissionManagerStates mFsmState = MissionManagerStates.IDLE;

    /** Response time timer. */
    private Timer mTimeoutTimer;
    /** Number of request retries when response is not received in time. */
    private int mNumberOfRetries;
    /** Timeout the response should be received within, in ms. */
    private long mTimeout;

    /** List of mission items used when writing or receiving a mission. */
    private List<msg_mission_item> mMissionItemMsgs = new ArrayList<msg_mission_item>();

    /** Handler of messages coming from timeout Timer. */
    private final Handler mHandler = new TimeoutHandler(this);


    /**
     * {@link MissionManager} constructor. Registers {@link VehicleListener}.
     */
    public MissionManager() {
        Vehicle.get().getEvents().addVehicleListener(this);
    }

    /**
     * Requests mission from the vehicle.
     */
    public void receiveMission() {
        // Ensure that MissionManager is not doing anything else
        if (mFsmState != MissionManagerStates.IDLE)
            return;
        startTimer(sDefaultTimoutInMs);
        mFsmState = MissionManagerStates.READ_REQUEST;
        MavLinkMission.requestMissionItemList();
    }

    /**
     * Sends mission to the vehicle.
     * 
     * @param missionItemMsgs mission items to be sent in format of MavLink messages
     */
    public void sendMission(List<msg_mission_item> missionItemMsgs) {
        if (mMissionItemMsgs == null)
            return;

        // Ensure that MissionManager is not doing anything else
        // if (mFsmState != MissionManagerStates.IDLE)
        // return;

        // Stop any previous activity before sending mission
        setManagerIdle();

        updateMissionItemSequenceNumber(missionItemMsgs);
        mMissionItemMsgs.clear();
        mMissionItemMsgs.addAll(missionItemMsgs);
        startTimer(sDefaultTimoutInMs);
        mFsmState = MissionManagerStates.WRITE_REQUEST;
        MavLinkMission.sendMissionItemCount(mMissionItemMsgs.size());
        SkyControlUtils.log("Mission write initiated, size: " + mMissionItemMsgs.size() + "\n",
                false);
    }

    /**
     * Updates a sequence number of the mission items in the list.
     * 
     * @param missionItems list with the mission items
     */
    private void updateMissionItemSequenceNumber(List<msg_mission_item> missionItems) {
        short seq = 0;
        for (msg_mission_item msg : missionItems) {
            msg.seq = seq++;
        }
    }

    /**
     * Tries to process a Mavlink message if it is a mission related one.
     * 
     * @param msg Mavlink message to process
     * @return true, if message was processed by this method, false otherwise
     */
    public boolean handleMessage(MAVLinkMessage msg) {
        boolean messageHandled = true;
        switch (msg.msgid) {
            case msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT:
                // Number of the mission items to be read received
                if (mFsmState == MissionManagerStates.READ_REQUEST) {
                    resetTimer();
                    mMissionItemCount = ((msg_mission_count) msg).count;
                    mMissionItemMsgs.clear();
                    MavLinkMission.requestMissionItem(mMissionItemMsgs.size());
                    mFsmState = MissionManagerStates.READING_MI;
                } else {
                    logError("Unexpected MAVLINK_MSG_ID_MISSION_COUNT received.");
                }
                break;
            case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:
                // Another mission item received
                if (mFsmState == MissionManagerStates.READING_MI) {
                    resetTimer();
                    processReceivedMissionItem((msg_mission_item) msg);
                    if (mMissionItemMsgs.size() < mMissionItemCount) {
                        // Request another mission item
                        MavLinkMission.requestMissionItem(mMissionItemMsgs.size());
                    } else {
                        // All mission items received, send acknowledge of that to vehicle
                        setManagerIdle();
                        MavLinkMission.sendAck();
                        Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_RECEIVED);
                    }
                } else {
                    logError("Unexpected MAVLINK_MSG_ID_MISSION_ITEM received.");
                }
                break;
            case msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST:
                // Request for another mission item received
                if (mFsmState == MissionManagerStates.WRITE_REQUEST) {
                    // Mission write request has been accepted so vehicle is requesting first
                    // mission item
                    mFsmState = MissionManagerStates.WRITING_MI;
                }
                if (mFsmState == MissionManagerStates.WRITING_MI
                        || mFsmState == MissionManagerStates.WAITING_WRITE_ACK) {
                    // This block get executed also on WAITING_WRITE_ACK state because vehicle may
                    // not have received the last mission item so instead of sending ACK it requests
                    // the mission item again
                    resetTimer();
                    mWriteMissionItemSeq = ((msg_mission_request) msg).seq;
                    sendMissionItem(mWriteMissionItemSeq);
                } else {
                    logError("Unexpected MAVLINK_MSG_ID_MISSION_REQUEST received.");
                }
                break;
            case msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK:
                // Vehicle received all mission items correctly
                if (mFsmState == MissionManagerStates.WAITING_WRITE_ACK
                        && ((msg_mission_ack) msg).type == MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED) {
                    Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_WRITTEN);
                    logMissionContent();
                } else {
                    // Error received from vehicle
                    Mission.get().getEvents().onMissionEvent(MissionEvent.MISSION_WRITE_FAILED);
                    logError("Unexpected MAVLINK_MSG_ID_MISSION_ACK with type "
                            + ((msg_mission_ack) msg).type + " received");
                }
                setManagerIdle();
                break;
            case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
                // Change in the current mission item the vehicle is heading to
                if(!Vehicle.get().getPosition().isFakeGpsEnabled())
                    Mission.get().setCurrentWpIndex(((msg_mission_current) msg).seq);
                break;
            default:
                messageHandled = false;
                break;
        }
        return messageHandled;
    }

    /**
     * Log written mission items to log file.
     */
    private void logMissionContent() {
        int i = 0;
        StringBuilder sBldr = new StringBuilder("Items of written mission:\n");
        for (msg_mission_item item : mMissionItemMsgs) {
            sBldr.append("item " + i++ + ": ");
            switch (item.command) {
                case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
                    sBldr.append("WP (lat " + item.x + ", lng " + item.y + ", alt " + item.z
                            + ")\n");
                    break;
                case MAV_CMD.MAV_CMD_DO_CHANGE_SPEED:
                    sBldr.append("SPD (" + item.param2 + ")\n");
                    break;
                case MAV_CMD.MAV_CMD_DO_SET_HOME:
                    sBldr.append("HOME (lat " + item.x + ", lng " + item.y + ", alt " + item.z
                            + ")\n");
                    break;
                default:
                    break;
            }
        }
        SkyControlUtils.log(sBldr.toString(), false);
    }

    /**
     * Logs error and sets the {@link MissionManager} to {@link MissionManagerStates#IDLE} state.
     * 
     * @param errText the error text to log
     */
    private void logError(String errText) {
        setManagerIdle();
        Log.e(SkyControlConst.ERROR_TAG, errText);
        SkyControlUtils.log(errText + "\n", false);
    }

    /**
     * Sets {@link MissionManager} to {@link MissionManagerStates#IDLE} state and stops the response
     * timer if started.
     */
    private void setManagerIdle() {
        mFsmState = MissionManagerStates.IDLE;
        stopTimer();
    }

    /**
     * Result of the request hasn't been received in time. Do appropriate action.
     */
    public void handleTimeout() {
        if (mFsmState == MissionManagerStates.IDLE)
            return;
        // If max retry is reached, set state to IDLE. No retry again.
        if (mNumberOfRetries >= sMaxRetries) {
            mFsmState = MissionManagerStates.IDLE;
            // Notify listeners that the response from vehicle has reached specified timeout
            Mission.get().getEvents().onMissionEvent(MissionEvent.RESPONSE_TIMEOUT);
            return;
        } else
            SkyControlUtils.log("Mission item send retry " + mNumberOfRetries + "\n", false);

        // Restart the timer but keep the number of retries
        resetTimerOnRetry();

        switch (mFsmState) {
            case READ_REQUEST:
                // Send request for mission again
                MavLinkMission.requestMissionItemList();
                break;
            case READING_MI:
                // Request last mission item again
                if (mMissionItemMsgs.size() < mMissionItemCount) {
                    MavLinkMission.requestMissionItem(mMissionItemMsgs.size());
                }
                break;
            case WRITE_REQUEST:
                // Send mission write again
                MavLinkMission.sendMissionItemCount(mMissionItemMsgs.size());
                break;
            case WRITING_MI:
            case WAITING_WRITE_ACK:
                // Send last sent mission item again
                sendMissionItem(mWriteMissionItemSeq);
                break;
            default:
                // Timeout happened in unexpected state, stop timer to not get the timeout again
                logError("Unexpected MissionManager state (" + mFsmState + ") on timeout");
                break;
        }
    }

    /**
     * Processes mission item to be sent.
     * 
     * @param writeMissionItemSeq sequence number of the mission item to be sent
     */
    private void sendMissionItem(int writeMissionItemSeq) {
        try {
            Connection.get().getMavLinkClient()
                    .sendMavPacket(mMissionItemMsgs.get(writeMissionItemSeq).pack());
        } catch (IndexOutOfBoundsException e) {
            logError("Requested mission item is out of bounds");
            return;
        }

        // Last mission item is being sent, ACK of whole mission is expected now from vehicle
        // Vehicle requests the mission items with seq number ranging <0, mMissionItems.size()-1>
        if (writeMissionItemSeq >= mMissionItemMsgs.size() - 1) {
            mFsmState = MissionManagerStates.WAITING_WRITE_ACK;
        }
    }

    /**
     * Processes received mission item.
     * 
     * @param msg the MavLink mission item message
     */
    private void processReceivedMissionItem(msg_mission_item msg) {
        // In case we receive the same WP again after retry
        // Vehicle sends the mission items with seq number starting with 0
        if (msg.seq <= mMissionItemMsgs.size() - 1)
            return;

        mMissionItemMsgs.add(msg);
    }

    /**
     * Stops the timer.
     */
    public synchronized void stopTimer() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
            mTimeoutTimer.purge();
            mTimeoutTimer = null;
        }
    }

    /**
     * Starts the timer.
     * 
     * @param timeoutInMs the timeout in ms
     */
    public void startTimer(long timeoutInMs) {
        mNumberOfRetries = 0;
        mTimeout = timeoutInMs;
        setTimer(mTimeout);
    }

    /**
     * Resets the timer and the number of retries that already took place.
     */
    public void resetTimer() {
        mNumberOfRetries = 0;
        setTimer(this.mTimeout);
    }

    /**
     * Resets the timer while keeping the number of retries that already took place.
     */
    public void resetTimerOnRetry() {
        setTimer(this.mTimeout);
    }

    /**
     * Sets the new timer.
     * 
     * @param timeoutInMs the timeout in ms
     */
    public synchronized void setTimer(long timeoutInMs) {
        stopTimer();
        if (mTimeoutTimer == null) {
            mTimeoutTimer = new Timer();
            mTimeoutTimer.schedule(new TimerTask() {
                public void run() {
                    if (mTimeoutTimer != null) {
                        stopTimer();
                        mNumberOfRetries++;
                        // This TimerTask runs in another thread, so we cannot call methods that
                        // handle UI. Notify through Handler instead.
                        mHandler.obtainMessage().sendToTarget();
                    }
                }
            }, timeoutInMs); // delay in milliseconds
        }
    }

    /**
     * Handler of timeout notification from {@link Timer}.
     */
    private static class TimeoutHandler extends Handler {

        /**
         * Weak reference to the parent {@link MissionManager}. Reason for weak reference:
         * http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
         */
        private final WeakReference<MissionManager> mMissionManagerWeak;

        /**
         * Instantiates a new timeout handler.
         * 
         * @param missionManager reference to a {@link MissionManager}
         */
        TimeoutHandler(MissionManager missionManager) {
            mMissionManagerWeak = new WeakReference<MissionManager>(missionManager);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            MissionManager mMissionManager = mMissionManagerWeak.get();
            mMissionManager.handleTimeout();
        }
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
            case ARMED:
                // Get user shared preferences
                SharedPreferences userPref =
                        PreferenceManager
                                .getDefaultSharedPreferences(SkyControlApp.getAppContext());
                if (userPref.getBoolean(PreferencesFragment.DWNLD_MISSION_ON_CONN_PREF_KEY, false)
                        && Vehicle.get().getState().isArmed())
                    // Request mission from vehicle when it gets armed and user set so in prefs
                    receiveMission();
                break;
            default:
                break;
        }
    }

    /**
     * Gets the mission item MavLink messages.
     * 
     * @return list of MavLink mission item messages
     */
    public List<msg_mission_item> getMissionItemMsgs() {
        return mMissionItemMsgs;
    }
}
