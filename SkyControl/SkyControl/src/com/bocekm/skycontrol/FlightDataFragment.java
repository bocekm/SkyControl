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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Encapsulates the common methods/fields for fragments displaying the flight parameters.
 */
public abstract class FlightDataFragment extends Fragment {
    /** References to the UI elements. */
    protected TextView mHeadingView;
    protected VerticalProgressBar mPitchBarUp;
    protected VerticalProgressBar mPitchBarDown;
    protected ProgressBar mRollBarLeft;
    protected ProgressBar mRollBarRight;

    /** Scale of the progress bar, <-PROGRESS_BAR_SCALE_DEG, PROGRESS_BAR_SCALE_DEG>. */
    protected final static int PROGRESS_BAR_SCALE_DEG = 45;
    /**
     * Number by which the angle in degrees is to be multiplied before it gets set in the progress
     * bar.
     */
    protected final static int PROGRESS_BAR_SCALE_DEG_MULT = 10;

    /** Angle adjusted for displaying in the progress bar. */
    protected int mAdjustedAngle;

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.flight_data_and_attitude, container, false);

        // Rotate progress bars over 180 degrees to depict pitch and yaw changes in correct sense
        mPitchBarUp = (VerticalProgressBar) rootView.findViewById(R.id.pitch_indicator_up);
        mPitchBarUp.setRotation(180);
        mRollBarLeft = (ProgressBar) rootView.findViewById(R.id.roll_indicator_left);
        mRollBarLeft.setRotation(180);

        // Get references to the UI elements
        mPitchBarDown = (VerticalProgressBar) rootView.findViewById(R.id.pitch_indicator_down);
        mRollBarRight = (ProgressBar) rootView.findViewById(R.id.roll_indicator_right);
        mHeadingView = (TextView) rootView.findViewById(R.id.heading_value);

        return rootView;
    }

    /**
     * On event.
     *
     * @param pitch the pitch
     * @param roll the roll
     * @param heading the heading
     */
    public void onEvent(float pitch, float roll, Float heading) {
        mHeadingView.setText(((Integer) (heading.intValue())).toString());

        // Get the pitch and yaw angles and adjust them to for the progress bar display
        mAdjustedAngle =
                SkyControlUtils.adjustAngle(
                        pitch,
                        PROGRESS_BAR_SCALE_DEG, PROGRESS_BAR_SCALE_DEG_MULT);
        mPitchBarUp.setProgress(mAdjustedAngle > 0 ? Math.abs(mAdjustedAngle) : 0);
        mPitchBarDown.setProgress(mAdjustedAngle > 0 ? 0 : Math.abs(mAdjustedAngle));

        mAdjustedAngle =
                SkyControlUtils.adjustAngle(roll,
                        PROGRESS_BAR_SCALE_DEG, PROGRESS_BAR_SCALE_DEG_MULT);
        mRollBarRight.setProgress(mAdjustedAngle > 0 ? Math.abs(mAdjustedAngle) : 0);
        mRollBarLeft.setProgress(mAdjustedAngle > 0 ? 0 : Math.abs(mAdjustedAngle));
    }
}
