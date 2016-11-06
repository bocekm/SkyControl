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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Opens number picker dialog when the button for which this listener is registered is clicked.
 */
public class OnNumberButtonClickListener implements
        OnClickListener {

    /** Reference to fragment hosting the button. */
    private Fragment mFragment;
    /** Reference to the parent activity hosting the fragment. */
    private FragmentActivity mActivity;
    /** Number currently chosen in number picker. */
    private int mCurrentNumber;
    /** Minimum number to show in number picker. */
    private int mMinNumber;
    /** Maximum number to show in number picker. */
    private int mMaxNumber;
    /** Resource id of the button view. */
    private int mViewId;
    /** Tag used to identify the response of the number picker. */
    private int mRequestTag;
    /** Title text of the number picker. */
    private String mTitle;

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        FragmentManager fm = mActivity.getSupportFragmentManager();
        // Get the value to select in number picker from the button view text
        mCurrentNumber =
                Integer.valueOf(String.valueOf(((TextView) mFragment.getView()
                        .findViewById(mViewId)).getText()));
        // Show the number picker
        NumberPickerDialog dialog =
                NumberPickerDialog.newInstance(mCurrentNumber, mMinNumber, mMaxNumber, mTitle);
        dialog.setTargetFragment(mFragment, mRequestTag);
        dialog.show(fm, null);
    }

    /**
     * Instantiates a new on param click listener.
     * 
     * @param activity reference to the parent activity hosting the fragment
     * @param fragment reference to fragment hosting the button
     * @param minNumber minimum number to show in number picker
     * @param maxNumber maximum number to show in number picker
     * @param viewId resource id of the button view
     * @param requestTag tag used to identify the response of the number picker
     * @param title title text of the number picker
     */
    public OnNumberButtonClickListener(FragmentActivity activity, Fragment fragment, int minNumber,
            int maxNumber, int viewId, int requestTag, String title) {
        mActivity = activity;
        mFragment = fragment;
        mMinNumber = minNumber;
        mMaxNumber = maxNumber;
        mViewId = viewId;
        mRequestTag = requestTag;
        mTitle = title;
    }
}
