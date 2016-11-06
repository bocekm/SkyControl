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
package com.bocekm.skycontrol.connection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bocekm.skycontrol.mavlink.MavLinkHeartbeat;

/**
 * {@link AndroidHeartbeat} class is used to send periodic heartbeat messages to the vehicle.
 */
public class AndroidHeartbeat {

    /**
     * Heartbeat period in seconds.
     */
    private final int mPeriod;

    /**
     * Scheduler used to periodically send the heartbeat.
     */
    private ScheduledExecutorService mHeartbeatScheduler;

    /**
     * Task for {@link AndroidHeartbeat#mHeartbeatScheduler} for sending the heartbeat.
     */
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            MavLinkHeartbeat.sendHeartbeat();
        }
    };

    /**
     * {@link AndroidHeartbeat} class constructor. Sets the period of the heartbeat.
     * 
     * @param periodInSec period with which the heartbeat message will be periodically sent in
     *        seconds
     */
    public AndroidHeartbeat(int periodInSec) {
        mPeriod = periodInSec;
    }

    /**
     * Activate/deactivate sending periodic heartbeat messages.
     * 
     * @param active true to activate the heartbeat, false to deactivate it
     */
    public void setActive(boolean active) {
        if (active) {
            // Create new task scheduler which will be sending heartbeat messages with period passed
            // as a parameter
            mHeartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
            mHeartbeatScheduler.scheduleWithFixedDelay(heartbeatRunnable, 0, mPeriod,
                    TimeUnit.SECONDS);
        } else if (mHeartbeatScheduler != null) {
            // Destroy the scheduler
            mHeartbeatScheduler.shutdownNow();
            mHeartbeatScheduler = null;
        }
    }
}
