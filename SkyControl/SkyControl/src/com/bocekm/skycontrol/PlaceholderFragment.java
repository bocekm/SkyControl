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
import android.widget.TextView;

/**
 * Placeholder fragment with no content except text depicting purpose of the fragment.
 */
public class PlaceholderFragment extends Fragment {

    /** Constant used to save any Fragment placeholder text into arguments Bundle. */
    public final static String PLACEHOLDER_TEXT = "placeholder_text";

    /**
     * Get new instance of the {@link PlaceholderFragment}.
     * 
     * @param placeholderTextResource resource id of the placeholder text to be displayed in the
     *        center of the Fragment
     * @return the placeholder fragment
     */
    public static PlaceholderFragment newInstance(int placeholderTextResource) {
        PlaceholderFragment newinstance = new PlaceholderFragment();

        // Set the passed text as an argument for the onCreateView method
        Bundle args = new Bundle();
        args.putInt(PLACEHOLDER_TEXT, placeholderTextResource);
        newinstance.setArguments(args);

        return newinstance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.placeholder_fragment, container, false);
        // Set the placeholder text to the center of the fragment
        TextView placeholderText = (TextView) rootView.findViewById(R.id.placeholder_text);
        placeholderText.setText(getArguments().getInt(PLACEHOLDER_TEXT, 0));
        return rootView;
    }
}
