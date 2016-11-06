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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bocekm.skycontrol.file.FileStream;

/**
 * {@link LogFragment} class handles the Log tab.
 */
public class LogFragment extends Fragment implements
        OnSharedPreferenceChangeListener {

    /** This button clears all the text in log. */
    private Button mClearButton;

    /** Button to enable/disable autoscrolling of the log text window when new text is added. */
    private ToggleButton mAutoscrollToggle;

    /** Scrollable wrapper for the {@link LogFragment#mLogTextView}. */
    private ScrollView mLogScrollView;

    /** Window with log text. */
    private TextView mLogTextView = null;

    /** Log file stream. */
    private BufferedOutputStream mLogStream = null;

    /** Indicates whether the user enabled logging to file. */
    private boolean mLogToFileEnabled = false;

    /** Indicates whether user enabled autoscroll functionality. True by default. */
    private boolean mAutoscrollEnabled = true;

    /** Denotes the preference with text to be saved in log tab window. */
    public static final String APPEND_TO_LOG_WINDOW_PREF_KEY =
            "com.bocekm.com.append_to_log_window_pref";

    /** Denotes the preference with text to be saved in log file. */
    public static final String APPEND_TO_LOG_FILE_PREF_KEY =
            "com.bocekm.com.append_to_log_file_pref";

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Load the layout for this fragment
        View rootView = inflater.inflate(R.layout.log_fragment, container, false);
        // Instantiate references to UI elements
        mClearButton = (Button) rootView.findViewById(R.id.clear_button);
        mAutoscrollToggle = (ToggleButton) rootView.findViewById(R.id.autoscroll_button);
        mLogScrollView = (ScrollView) rootView.findViewById(R.id.log_scroll_view);
        mLogTextView = (TextView) rootView.findViewById(R.id.log_textview);

        // Set the on/off state of the toggle
        mAutoscrollToggle.setChecked(mAutoscrollEnabled);
        // When user clicks the toggle, update the mAutoscrollEnabled variable accordingly
        mAutoscrollToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAutoscrollEnabled = isChecked ? true : false;
            }
        });

        // Add listener to any log window text change so it can autoscroll
        mLogTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg) {
                if (mAutoscrollEnabled)
                    // Autoscroll to newly added text
                    mLogScrollView.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        });

        // Clear any text residing within Log window on Clear button click
        mClearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogTextView.setText(SkyControlConst.EMPTY_STRING);
            }
        });

        // Load saved log window text if there's any (text saved by onSaveInstanceState method)
        if (savedInstanceState != null)
            mLogTextView.setText(savedInstanceState.getCharSequence("logContent"));

        // Load any text to be logged saved in shared preferences (saved by other fragments)
        loadLogFromSharedPrefs();

        // Autoscroll down to the end of loaded text
        mLogScrollView.fullScroll(View.FOCUS_DOWN);

        return rootView;
    }

    /*
     * (non-Javadoc) Open log file stream if user has enabled logging to file.
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user preferences
        SharedPreferences userPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mLogToFileEnabled = userPref.getBoolean(PreferencesFragment.LOG_TO_FILE_PREF_KEY, false);
        enableFileLogging(mLogToFileEnabled);
    }

    /**
     * Creates or closes log file stream based on whether the file logging is enabled or disabled.
     * 
     * @param loggingEnabled is logging enabled
     */
    private void enableFileLogging(boolean loggingEnabled) {
        if (loggingEnabled) {
            if (mLogStream != null)
                return;
            try {
                mLogStream = FileStream.getLogFileStream();
            } catch (FileNotFoundException e) {
                SkyControlUtils.toast("Log file couldn't be created", Toast.LENGTH_SHORT);
                mLogStream = null;
                e.printStackTrace();
            }
        } else {
            if (mLogStream == null)
                return;
            try {
                mLogStream.close();
            } catch (IOException e) {
                Log.e(SkyControlConst.ERROR_TAG, "Log file stream cannot be closed");
                e.printStackTrace();
            }
            mLogStream = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister user preferences onchange listener
        SharedPreferences userPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        userPref.unregisterOnSharedPreferenceChangeListener(this);
        // Close log file stream if it has been opened
        enableFileLogging(false);
        // Clear any unsaved/undisplayed log text from shared preferences so it does not
        // unexpectedly pop up when opening the app later
        SharedPreferences prefs =
                getActivity().getSharedPreferences(SkyControlConst.APP_PREFERENCES,
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(APPEND_TO_LOG_WINDOW_PREF_KEY).remove(APPEND_TO_LOG_FILE_PREF_KEY).commit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current content of the log window to Fragment Bundle to preserve it in the event
        // of destroying this fragment
        if (mLogTextView != null)
            outState.putCharSequence("logContent", mLogTextView.getText());
    }

    /**
     * Load log text from {@link SharedPreferences}.
     */
    private void loadLogFromSharedPrefs() {
        // Open Shared Preferences
        SharedPreferences prefs =
                getActivity().getSharedPreferences(SkyControlConst.APP_PREFERENCES,
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Get any text to be logged that may be saved in shared preferences because
        // this Fragment was not instantiated at the time of need to log something
        String toBeAppended =
                prefs.getString(APPEND_TO_LOG_WINDOW_PREF_KEY, SkyControlConst.EMPTY_STRING);
        appendToLogWindow(toBeAppended);
        if (mLogToFileEnabled) {
            // Retreive the log text to be saved in file
            toBeAppended =
                    prefs.getString(APPEND_TO_LOG_FILE_PREF_KEY, SkyControlConst.EMPTY_STRING);
            appendToLogFile(toBeAppended);
        }
        // Once the text is logged, clear it from shared preferences
        editor.remove(APPEND_TO_LOG_WINDOW_PREF_KEY).remove(APPEND_TO_LOG_FILE_PREF_KEY);
        editor.commit();
    }

    /**
     * Adds text to an existing text within {@link LogFragment#mLogTextView}.
     * 
     * @param text text to be added to log
     */
    public void appendToLogWindow(String text) {
        mLogTextView.append(text);
    }

    /**
     * Adds text to a log file.
     * 
     * @param text text to be added to log
     */
    public void appendToLogFile(String text) {
        if (mLogStream == null) {
            Log.e(SkyControlConst.ERROR_TAG, "Log file stream is uninitialized");
            return;
        }
        try {
            mLogStream.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
        switch (key) {
            case PreferencesFragment.LOG_TO_FILE_PREF_KEY:
                // Indicates whether the user enabled logging to file
                mLogToFileEnabled =
                        sharedPrefs.getBoolean(PreferencesFragment.LOG_TO_FILE_PREF_KEY, true);
                enableFileLogging(mLogToFileEnabled);
                break;
            default:
                break;
        }
    }

    public boolean isLogToFileEnabled() {
        return mLogToFileEnabled;
    }
}
