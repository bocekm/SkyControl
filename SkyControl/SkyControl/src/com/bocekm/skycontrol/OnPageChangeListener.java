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

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Spinner;

/**
 * This listener's method onPageSelected is called when tab on action bar is clicked.
 */
public class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

    /** Activity, in which this Listener was created. */
    private Activity mActivity;

    /**
     * Instantiates a new on page change listener.
     * 
     * @param a The mActivity
     */
    public OnPageChangeListener(Activity a) {
        mActivity = a;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.ViewPager.SimpleOnPageChangeListener#onPageSelected(int)
     */
    @Override
    public void onPageSelected(int position) {

        // Android API bug workaround.
        // Update the ActionBar Spinner listing the tabs - Android uses Spinner when the screen is
        // too small to fit the tabs next to each other. The bug lies in not updating the Spinner
        // according to swiping the tabs.
        mActivity.getActionBar().getTabAt(position).select();
        ViewParent root = mActivity.findViewById(android.R.id.content).getParent();
        findAndUpdateSpinner(root, position);
    }

    /**
     * Searches the view hierarchy excluding the content view for a possible Spinner in the
     * ActionBar.
     * 
     * @param root The parent of the content view
     * @param position The position that should be selected
     * @return if the spinner was found and adjusted
     */
    private boolean findAndUpdateSpinner(Object root, int position) {
        if (root instanceof android.widget.Spinner) {
            // Found the Spinner
            Spinner spinner = (Spinner) root;
            spinner.setSelection(position);
            return true;
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            if (group.getId() != android.R.id.content) {
                // Found a container that isn't the container holding our screen layout
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (findAndUpdateSpinner(group.getChildAt(i), position)) {
                        // Found and done searching the View tree
                        Log.i(SkyControlConst.DEBUG_TAG, "ActionBar spinner is being updated");
                        return true;
                    }
                }
            }
        }
        // Action Bar Spinner not found
        return false;
    }
}
