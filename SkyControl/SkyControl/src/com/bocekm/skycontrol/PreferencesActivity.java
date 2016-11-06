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
import android.support.v4.app.FragmentActivity;

/**
 * The {@link PreferencesActivity} displays the application settings by instantiating new
 * {@link PreferencesFragment}.
 */
public class PreferencesActivity extends FragmentActivity {
    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the PreferencesFragment as the main Activity content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferencesFragment()).commit();
    }
}
