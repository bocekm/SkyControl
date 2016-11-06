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

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the tabs.
 * {@link FragmentPagerAdapter} keeps every loaded fragment in memory.
 */
public class TabsPagerAdapter extends FragmentStatePagerAdapter {

    /** Array of fragments registered for each tab. */
    private SparseArray<Fragment> mRegisteredFragments = new SparseArray<Fragment>();

    /** Array with tab title strings. */
    private ArrayList<String> mTabTitles;

    /**
     * Whether Google Play services are available. Different fragments are used based on this value.
     */
    private boolean mGooglePlayServicesAvailable;

    /**
     * Action bar tab position index for Map tab. Shall correspond to the tab titles specified in
     * tab_titles resource array.
     */
    public final static int MAP_TAB = 0;
    /**
     * Action bar tab position index for flight director tab. Shall correspond to the tab titles
     * specified in tab_titles resource array.
     */
    public final static int FD_TAB = 1;
    /**
     * Action bar tab position index for Log tab. Shall correspond to the tab titles specified in
     * tab_titles resource array.
     */
    public final static int LOG_TAB = 2;

    /**
     * Tab titles are accessible by indexing the returned {@link ArrayList} by position of the tab
     * on action bar.
     * 
     * @return array with tab title strings
     */
    public ArrayList<String> getTabTitles() {
        return mTabTitles;
    }

    /**
     * Instantiates a new {@link TabsPagerAdapter}.
     * 
     * @param fm instance of {@link FragmentManager}
     * @param c the context
     */
    public TabsPagerAdapter(FragmentManager fm, Context c) {
        super(fm);
        // Load tab titles from XML resource array and put them into ArrayList
        String[] tabTitlesArray = c.getResources().getStringArray(R.array.tab_titles);
        mTabTitles = new ArrayList<String>(Arrays.asList(tabTitlesArray));
        // Determine whether Google Play services are available on the device
        mGooglePlayServicesAvailable = LocationFragment.isLocationServiceAvailable(c);
    }

    /*
     * (non-Javadoc) GetItem is called to instantiate/get the fragment for the given tab.
     * 
     * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
     */
    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        // position represents the position of the tab on action bar, numbered from 0
        switch (position) {
            case MAP_TAB:
                // If Google Play services are not available, the map fragment cannot be shown.
                // Return placeholder fragment instead.
                fragment =
                        mGooglePlayServicesAvailable ? new LocationFragment() : PlaceholderFragment
                                .newInstance(R.string.gservices_missing);
                break;
            case FD_TAB:
                fragment = new FlightDirectorFragment();
                break;
            case LOG_TAB:
                fragment = new LogFragment();
                break;
            default:
                throw new ClassCastException(this.toString()
                        + " is trying to initialize unwanted tab No." + position);
        }
        return fragment;
    }

    /*
     * (non-Javadoc) Save currently instantiated fragment reference to the array. This array is
     * indexed by position of the tab on the action bar, numbered from 0.
     * 
     * @see android.support.v4.app.FragmentPagerAdapter#instantiateItem(android.view.ViewGroup, int)
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mRegisteredFragments.put(position, fragment);
        return fragment;
    }

    /**
     * Returns reference to the fragment on specific position.
     * 
     * @param position position of the tab (holding requested fragment) on the action bar, numbered
     *        from 0
     * @return the fragment residing on a action bar tab denoted by position parameter
     */
    public Fragment getRegisteredFragment(int position) {
        return mRegisteredFragments.get(position);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentPagerAdapter#destroyItem(android.view.ViewGroup, int,
     * java.lang.Object)
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Once the fragment gets destroyed, remove it from the array holding references to the
        // instantiated fragments
        mRegisteredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    /*
     * (non-Javadoc) Returns total number of the tabs
     * 
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        return mTabTitles.size();
    }
}
