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

import java.lang.reflect.Field;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Responsible for inflating the correct views on the flight director tab. Manages clicking on the
 * COPY button.
 */
public class FlightDirectorFragment extends Fragment {

    /** Fragment showing the basic flight parameter of the connected vehicle. */
    private final OverviewFragment mOverviewFragment = new OverviewFragment();

    /** Fragment managing controllers for the flight director. */
    private final FlightDirectorControllersFragment mFlightDirectorControllersFragment =
            new FlightDirectorControllersFragment();

    /** Fragment displaying values coming from vehicle navigation controller. */
    private final VehicleNavigatorFragment mVehicleNavigatorFragment =
            new VehicleNavigatorFragment();

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.flight_director_fragment, container, false);
        TextView copyButton = (TextView) rootView.findViewById(R.id.copy_values);
        copyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mFlightDirectorControllersFragment.overwriteFlightDirectorWithCurrent();
            }
        });
        return rootView;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        FragmentManager fm = getChildFragmentManager();
        fm.popBackStackImmediate();
        FragmentTransaction ft = fm.beginTransaction();
        if (fm.findFragmentByTag("overview") == null) {
            ft.add(R.id.fd_current_fragment, mOverviewFragment, "overview");
        }
        if (fm.findFragmentByTag("controller") == null) {
            ft.add(R.id.fd_set_fragment, mFlightDirectorControllersFragment, "controller");
        }
        if (fm.findFragmentByTag("navigator") == null) {
            ft.add(R.id.fd_planned_fragment, mVehicleNavigatorFragment, "navigator");
        }
        ft.addToBackStack(null);
        ft.commit();
    }

    /*
     * (non-Javadoc) Android nested fragment bug workaround:
     * http://stackoverflow.com/questions/15207305
     * /getting-the-error-java-lang-illegalstateexception-
     * activity-has-been-destroyed/15656428#15656428
     * 
     * @see android.support.v4.app.Fragment#onDetach()
     */
    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (Exception e) {
            Log.e(SkyControlConst.ERROR_TAG, "Error [s/g]etting mChildFragmentManager field");
            throw new RuntimeException(e);
        }
    }
}
