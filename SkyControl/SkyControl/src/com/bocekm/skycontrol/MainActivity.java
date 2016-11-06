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

import java.io.File;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.connection.AndroidHeartbeat;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.MissionItemList;
import com.bocekm.skycontrol.mission.item.DoSetHome;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;
import com.bocekm.skycontrol.vehicle.VehicleHeartbeat.HeartbeatState;
import com.google.android.gms.maps.model.LatLng;

/**
 * The {@link MainActivity} class is the entry point of the application.
 */
public class MainActivity extends FragmentActivity implements
        ActionBar.TabListener,
        AlertDialogFragment.AlertDialogListener,
        ConnectionListener,
        OnSharedPreferenceChangeListener,
        VehicleListener,
        MissionListener {

    /** The {@link PagerAdapter} providing the fragments for each of the tabs. */
    private TabsPagerAdapter mPagerAdapter;

    /** The {@link ViewPager} hosting the content of the tabs. */
    private NonSwipeableViewPager mViewPager;

    /** Android version check result. */
    private boolean mIsDeviceSupported;

    /** Reference to a {@link LogHelper} class. */
    private LogHelper mLogHelper;

    /** Reference to class managing periodic heartbeat messages. */
    private AndroidHeartbeat mAndroidHeartbeat;

    /** USB manager handles actions related to USB device. */
    UsbManager mUsbManager;

    /** Intent to be broadcasted when user allows/denies usage of connected USB device. */
    PendingIntent mPermissionIntent;

    /** Connected USB device. */
    UsbDevice mUsbDevice;

    /** Broadcast intent tag for handling USB device usage permissions request. */
    private static final String ACTION_USB_PERMISSION = "com.bocekm.skycontrol.USB_PERMISSION";

    /** Preference indicating whether the app runs for the first time. */
    private static final String LOADED_FIRST_TIME_PREF_KEY =
            "com.bocekm.skycontrol.loaded_first_time_pref";

    /** Action bar view for managing the WP and FD modes by user. */
    private MissionActionProvider mMissionProvider = null;
    /** Action bar view for (dis)connecting the telemetry by user. */
    private ConnectActionProvider mConnProvider = null;

    /** Whether the user requested writing of the mission to the vehicle through settings. */
    private boolean mMissionWriteRequested = false;

    /**
     * Bundle in onCreate has some data on two occasions: a) the Activity is destroyed by system or
     * b) change of configuration (e.g. device orientation).
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure we're running on Ice Cream Sandwich or newer
        mIsDeviceSupported = isAndroidSupported();
        if (!mIsDeviceSupported) {
            // stop processing the onCreate method - that would lead to crash
            return;
        }

        // Load default preferences if this is the first app run
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set up application attributes according to user preferences
        resolvePreferences();

        // Set up action bar parameters
        final ActionBar actionBar = getActionBar();
        setUpActionBar(actionBar);

        // Add tabbing functionality by using ViewPager
        mViewPager = new NonSwipeableViewPager(this);
        mViewPager.setId(R.id.view_pager);
        // Set number of pages to keep in memory (to the left and to the right from the current
        // page/tab)
        // To provide better user experience, set the number higher than default 1, so the tabs do
        // not get recreated too much often
        mViewPager.setOffscreenPageLimit(3);
        // Set the ViewPager to manage the content of this Activity
        setContentView(mViewPager);

        // Initialize content of the tabs to be displayed on action bar
        initializeTabs(actionBar);

        // Keep the reference to this class, mainly for logging purpose. That's why it is positioned
        // after initializing the action bar tabs, because logging function relies on that.
        SkyControlApp.setMainActivity(this);

        // Instantiate the LogHelper class to be able to log even before the
        // LogFragment is instantiated
        mLogHelper = new LogHelper(this);

        // Specify period for sending heartbeat messages
        mAndroidHeartbeat = new AndroidHeartbeat(SkyControlConst.HEARTBEAT_PERIOD_IN_SECONDS);

        // Get USB manager to register device attach event
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // This permission intent serves as a callback called when user allows/denies USB device
        // usage
        mPermissionIntent =
                PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // To receive the broadcast message receiver needs to be registered
        IntentFilter permissionFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, permissionFilter);
        IntentFilter detachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, detachedFilter);

        // Initialize collision avoidance data, like obstacles and terrain
        CollisionAvoidance.get().onCreate();
    }

    /**
     * Sets application attributes according to user preferences. Register listener on changes to
     * preferences.
     */
    private void resolvePreferences() {
        // Get user preferences
        SharedPreferences userPref = PreferenceManager.getDefaultSharedPreferences(this);
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);

        // Get application preferences
        SharedPreferences appPrefs =
                getSharedPreferences(SkyControlConst.APP_PREFERENCES, Context.MODE_PRIVATE);
        // Determine whether the app runs for the first time
        if (appPrefs.getBoolean(LOADED_FIRST_TIME_PREF_KEY, true)) {
            // Set default directory for saving the files accessible to user, like logs
            File defaultDir =
                    new File(Environment.getExternalStorageDirectory(),
                            getString(getApplicationInfo().labelRes));
            userPref.edit()
                    .putString(PreferencesFragment.FILE_DIRECTORY_PREF_KEY, defaultDir.getPath())
                    .commit();
            Log.d(SkyControlConst.DEBUG_TAG, "Default dir set: " + defaultDir.getPath() + "\n");

            // Set the preference that the app has already been run
            appPrefs.edit().putBoolean(LOADED_FIRST_TIME_PREF_KEY, false).commit();
        }
    }

    /*
     * (non-Javadoc) The onNewIntent is called because this Activity has launchMode set to
     * singleTop. That means when USB device is connected and the application is running already,
     * the existing Activity instance is used and not created a new one. Then onNewIntent receives
     * intent saying USB device was connected (attached).
     * 
     * @see android.support.v4.app.FragmentActivity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            // As the USB device is connected, request user for permission to use it
            Log.d(SkyControlConst.DEBUG_TAG, "USB device connected");
            mUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast listeners to not leak them as the app finishes
        this.unregisterReceiver(mUsbReceiver);
        // Removes reference to this Activity in the application singleton
        SkyControlApp.setMainActivity(null);
        mLogHelper.destroyLogHelper();
        // Remove listeners from the action bar providers as they are to be destroyed with this
        // Activity but they don't have any ondestroy lifecycle method
        if (mMissionProvider != null)
            mMissionProvider.removeListeners();
        if (mConnProvider != null)
            mConnProvider.removeListener();

        // Destroys collision avoidance data, like obstacles and terrain
        CollisionAvoidance.get().onDestroy();
    }

    /** Receives broadcast intents related to USB device. */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        /*
         * (non-Javadoc)
         * 
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION:
                    // User allowed or denied usage of the connected USB device
                    synchronized (this) {
                        // Resolve whether user granted permission to use the device
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            // Get the connected device parameters
                            mUsbDevice =
                                    (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                            if (mUsbDevice != null) {
                                Log.d(SkyControlConst.DEBUG_TAG, "Permission granted for device: "
                                        + mUsbDevice);
                                // Start the MavLink service.
                                Connection.get().getMavLinkClient().bindMavLinkService();
                            }
                        } else {
                            Log.e(SkyControlConst.ERROR_TAG, "Permission denied for device: "
                                    + mUsbDevice);
                        }
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    // The telemetry has been disconnected. Stop the MavLink service if it is still
                    // running
                    Connection.get().getMavLinkClient().unbindMavLinkService();
                    break;
                default:
                    break;
            }

        }
    };

    /*
     * (non-Javadoc) Handles changes made upon the user settings.
     * 
     * @see
     * android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged
     * (android.content.SharedPreferences, java.lang.String)
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesFragment.SWIPE_ENABLED_PREF_KEY:
                // Enable or disable the functionality of tabs swiping
                Boolean swipeEnabled =
                        sharedPreferences.getBoolean(PreferencesFragment.SWIPE_ENABLED_PREF_KEY,
                                true);
                mViewPager.setSwipeEnabled(swipeEnabled);
                break;
            default:
                break;
        }
    }

    /**
     * Initializes content of all the tabs.
     * 
     * @param actionBar the action bar
     */
    private void initializeTabs(final ActionBar actionBar) {
        // Create the adapter that will return a fragment for each of the tabs of the activity
        mPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager(), this);

        // Set up the ViewPager with the tabs adapter
        mViewPager.setAdapter(mPagerAdapter);

        // When swiping between different tabs, select the corresponding tab on the action bar
        OnPageChangeListener onPageListener = new OnPageChangeListener(this);
        mViewPager.setOnPageChangeListener(onPageListener);

        // Add all the tabs we need to the action bar
        for (int position = 0; position < mPagerAdapter.getCount(); position++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab().setText(mPagerAdapter.getTabTitles().get(position))
                    .setTabListener(this));
        }
    }

    /**
     * Checks whether the version of Android is supported. Currently the app supports Android
     * version 4.0 (Ice Cream Sandwich) and later.
     * 
     * @return true, if the Android is supported
     */
    private boolean isAndroidSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Open dialog with "Not supported" and close the app on click on OK button
            AlertDialogFragment androidUnsupportedDialog =
                    AlertDialogFragment.newInstance("Error",
                            "This app supports just Android 4.0 and later");
            showDialogFragment(androidUnsupportedDialog);
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Listen to Connection events
        Connection.get().getEvents().addConnectionListener(this);
        Mission.get().getEvents().addMissionListener(this);
        Vehicle.get().getEvents().addVehicleListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onStop()
     */
    @Override
    public void onStop() {
        super.onStop();
        // Remove listener to the Connection events
        Connection.get().getEvents().removeConnectionListener(this);
        Mission.get().getEvents().removeMissionListener(this);
        Vehicle.get().getEvents().removeVehicleListener(this);
    }


    /*
     * (non-Javadoc) Listens to MavLink service connection events.
     * 
     * @see com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener#onConnectionEvent(
     * com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent)
     */
    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_BOUND:
                // MavLink service has been bound, start sending heartbeat
                mAndroidHeartbeat.setActive(true);
                break;
            case SERVICE_UNBOUND:
                // Hide the send/receive mission menu items
                invalidateOptionsMenu();
                // MavLink service has been unbound
                // Stop sending heartbeat
                mAndroidHeartbeat.setActive(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case VEHICLE_CONNECTED:
                SkyControlUtils.toast("Vehicle connected", Toast.LENGTH_SHORT);
                // Intentional fall through
            case HEARTBEAT_RESTORED:
                // Show the send/receive mission menu items
                invalidateOptionsMenu();
                break;
            case HEARTBEAT_TIMEOUT:
                invalidateOptionsMenu();
                SkyControlUtils.toast("Vehicle lost", Toast.LENGTH_SHORT);
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.mission.MissionEvents.MissionListener#onMissionEvent(com.bocekm.skycontrol
     * .mission.MissionEvents.MissionEvent)
     */
    @Override
    public void onMissionEvent(MissionEvent event) {
        switch (event) {
            case MISSION_WRITTEN:
                if (mMissionWriteRequested) {
                    // This app has more places where the mission write is requested so we don't
                    // want to toast about that every time
                    SkyControlUtils.toast(getString(R.string.mission_written), Toast.LENGTH_SHORT);
                    mMissionWriteRequested = false;
                } else
                    SkyControlUtils.log(getString(R.string.mission_written) + "\n", false);
                break;
            case MISSION_RECEIVED:
                SkyControlUtils.toast(getString(R.string.mission_received), Toast.LENGTH_SHORT);
                break;
            default:
                break;
        }
    }

    /**
     * Sets parameters of the action bar.
     * 
     * @param actionBar reference to the action bar
     */
    private void setUpActionBar(ActionBar actionBar) {
        // Allow tabs as a navigation tool
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    /*
     * (non-Javadoc) Creates content of the menu including items visible on action bar.
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add buttons for toggling waypoint following and flight director modes
        if (mMissionProvider != null)
            mMissionProvider.removeListeners();
        MenuItem missionItem = menu.add(null);
        missionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mMissionProvider = new MissionActionProvider(this);
        missionItem.setActionProvider(mMissionProvider);

        // Add connection toggle button
        if (mConnProvider != null)
            mConnProvider.removeListener();
        MenuItem connItem = menu.add(null);
        connItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mConnProvider = new ConnectActionProvider(this);
        connItem.setActionProvider(mConnProvider);

        // Add send/receive mission and set home menu items
        if (Vehicle.get().getHeartbeat().getHeartbeatState() == HeartbeatState.PERIODIC_HEARTBEAT)
            getMenuInflater().inflate(R.menu.vehicle_connected_menu, menu);

        // Add additional menu items specified in XML
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        switch (item.getItemId()) {
            case R.id.preferences_settings:
                // Open window with application preferences
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                return true;
            case R.id.send_mission:
                mMissionWriteRequested = true;
                Mission.get().sendMissionToVehicle();
                return true;
            case R.id.receive_mission:
                Mission.get().receiveMissionFromVehicle();
                return true;
            case R.id.set_home:
                setHome();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setHome() {
        // Mission with the only command to set home location
        MissionItemList homeMission = new MissionItemList();
        LatLng position = Vehicle.get().getPosition().getPosition();
        float altitude;
        if (CollisionAvoidance.get().isCasEnabled())
            // Add one meter to the terrain elevation to have the home above the ground
            altitude = CollisionAvoidance.get().getElevationModel().getElevation(position) + 1;
        else
            altitude = Vehicle.get().getAltitude().getAltitude();
        DoSetHome homeCmd = new DoSetHome(homeMission).setPosition(position).setAltitude(altitude);

        homeMission.addMissionItem(homeCmd);
        Mission.get().sendMissionToVehicle(homeMission);
        SkyControlUtils.log("Home set with alt " + altitude + " m\n", true);
    }

    /**
     * Shows the dialog passed as an argument to the user.
     * 
     * @param dialog the dialog to be showed
     */
    private void showDialogFragment(DialogFragment dialog) {
        dialog.show(getSupportFragmentManager(), null);
    }

    /*
     * (non-Javadoc) Closes the application when clicked on OK of presented Dialog.
     * 
     * @see
     * com.bocekm.skycontrol.AlertDialogFragment.AlertDialogListener#onDialogPositiveClick(android
     * .support.v4.app.DialogFragment)
     */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        this.finish();
    }

    /**
     * Gets the fragment displayed on the currently opened tab.
     * 
     * @return {@link Fragment} displayed on the currently opened tab.
     */
    public Fragment getCurrentPageFragment() {
        Fragment currentPageFragment =
                mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        return currentPageFragment;
    }


    /**
     * Gets the fragment set on tab having position numbered from 0. Be aware that the fragment may
     * not be instantiated yet so check the return value on null.
     * 
     * @param position the position of the tab on action bar, numbered from 0
     * @return {@link Fragment} assigned to reside under navigation tab denoted by position. May be
     *         null if the fragment is not instantiated yet.
     */
    public Fragment getPageFragmentOnPosition(int position) {
        if (mPagerAdapter != null)
            return mPagerAdapter.getRegisteredFragment(position);
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.ActionBar.TabListener#onTabSelected(android.app.ActionBar.Tab,
     * android.app.FragmentTransaction)
     */
    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction arg1) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.ActionBar.TabListener#onTabReselected(android.app.ActionBar.Tab,
     * android.app.FragmentTransaction)
     */
    @Override
    public void onTabReselected(Tab arg0, android.app.FragmentTransaction arg1) {}

    /*
     * (non-Javadoc)
     * 
     * @see android.app.ActionBar.TabListener#onTabUnselected(android.app.ActionBar.Tab,
     * android.app.FragmentTransaction)
     */
    @Override
    public void onTabUnselected(Tab arg0, android.app.FragmentTransaction arg1) {}
}
