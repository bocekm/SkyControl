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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;

/**
 * {@link NonSwipeableViewPager} allows to disable swiping functionality of the action bar tabs.
 * Swiping means to switch between tabs by using swipe gestures (left to right or right to left).
 */
public class NonSwipeableViewPager extends ViewPager {

    /** Is swiping enabled. */
    private boolean mSwipeEnabled;

    /**
     * Instantiates a new view pager. Whether the swiping functionality is enabled or not is based
     * on application settings. By default it is enabled.
     * 
     * @param context Any context
     */
    public NonSwipeableViewPager(Context context) {
        super(context);
        // Load application settings, set by user or the default ones
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mSwipeEnabled = sharedPref.getBoolean(PreferencesFragment.SWIPE_ENABLED_PREF_KEY, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.ViewPager#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // As the swipe touch is detected, decide whether to change the tab or not
        if (this.mSwipeEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.ViewPager#onInterceptTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // As the swipe touch is detected, decide whether to change the tab or not
        if (this.mSwipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
    }
}
