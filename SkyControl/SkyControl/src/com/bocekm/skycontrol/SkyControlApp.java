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
package com.bocekm.skycontrol;

import android.app.Application;
import android.content.Context;

import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.vehicle.Vehicle;

/**
 * The Class {@link SkyControlApp}, as an {@link Application} class extension, is used to initialize
 * essential application data which persist throughout whole application lifetime.
 */
public class SkyControlApp extends Application {

    /** Reference to the application Context. */
    private static Context sAppContext = null;

    /** Reference to the entry point Activity. */
    private static MainActivity sMainActivity = null;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Keep application context, mainly to allow non-Activity classes to reach
        // user preferences
        sAppContext = getApplicationContext();

        // Get (init) instance of the singletons - persistent across application lifetime.
        // Singletons shall be initialized in application context because when the app is destroyed
        // let's say because of low memory and user wants to access it back again then just the last
        // activity on the stack is recreated after this application class onCreate is called.
        Connection.init(sAppContext);
        Vehicle.init();
        CollisionAvoidance.init();
        Mission.init();
    }

    public static Context getAppContext() {
        return sAppContext;
    }

    public static MainActivity getMainActivity() {
        return sMainActivity;
    }

    public static void setMainActivity(MainActivity mainActivity) {
        sMainActivity = mainActivity;
    }
}
