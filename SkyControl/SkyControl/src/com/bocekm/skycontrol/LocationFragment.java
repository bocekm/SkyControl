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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bocekm.skycontrol.ListeningMapView.MapViewListener;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionListener;
import com.bocekm.skycontrol.cas.Obstacle;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.mission.MissionEvents.MissionEvent;
import com.bocekm.skycontrol.mission.MissionEvents.MissionListener;
import com.bocekm.skycontrol.mission.item.NavMissionItem;
import com.bocekm.skycontrol.mission.item.Waypoint;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.HashBiMap;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * The {@link LocationFragment} class implements the mapping functionality.
 */
public class LocationFragment extends Fragment implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        VehicleListener,
        MissionListener,
        ConnectionListener,
        MapViewListener,
        CollisionListener {

    /** The {@link ListeningMapView} containing the map. Calls onMapTouch method. */
    private ListeningMapView mMapView;
    /** Instance of {@link GoogleMap} created within {@link MapView}. */
    private GoogleMap mMap;
    /** Indicates that the resource variable has actually no resource assigned. */
    private static final int NO_RESOURCE_SET = -1;
    /** Vehicle lost map marker. */
    private Marker mVehicleLostMarker;
    /** Vehicle map marker. */
    private Marker mVehicleMarker;
    /** Device map marker resource. */
    private int mDeviceMarkerResource = NO_RESOURCE_SET;
    /** Device map marker. */
    private Marker mDeviceMarker;
    /** Current instantiation of the {@link LocationClient}. */
    private LocationClient mDeviceLocationClient;
    /** A request to connect to Location Services. */
    private LocationRequest mDeviceLocationRequest;
    /** Reference to the vehicle location button. */
    private ImageButton mVehicleLocationButton;
    /** Reference to the edit waypoints button. */
    private ImageButton mEditWaypointsButton;
    /** Heading of the vehicle in degrees used for rotation of the vehicle icon on the map. */
    private double mVehicleHeading = 0.0;
    /** Status text field on the bottom of the map. */
    private TextView mConnectionStatus;

    /** Marks whether the map should follow the vehicle's location. */
    private boolean mFollowVehicle = true;
    /** Keeps the latest acquired vehicle location. */
    private LatLng mLatestVehicleLocation = null;

    /** Request code to send to Google Play services. It's is returned in Activity.onActivityResult. */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    /** Location update parameter: Milliseconds per second. */
    private static final int MILLISECONDS_PER_SECOND = 1000;
    /** Location update parameter: A fast interval ceiling. */
    private static final int FAST_CEILING_IN_SECONDS = 1;
    /** Location update parameter: Update interval in milliseconds. */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    /** Location update parameter: A fast ceiling of update intervals, used when the app is visible. */
    private static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * FAST_CEILING_IN_SECONDS;
    /** Tag of shared preferences repository that stores persistent camera position data. */
    private static final String MAP_PREFERENCES = "com.bocekm.skycontrol.MAP_PREFERENCES";
    /** Shared preferences keys for saved map camera parameters. */
    private static final String PREF_LAT = "lat";
    private static final String PREF_LNG = "lng";
    private static final String PREF_BEA = "bea";
    private static final String PREF_TILT = "tilt";
    private static final String PREF_ZOOM = "zoom";

    /** Fragment showing the basic flight data of the connected vehicle. */
    private OverviewFragment mOverviewFragment;
    /** Fragment showing the list of mission items. */
    private MissionItemListFragment mMissionItemListFragment;
    /** Says whether the waypoint edit sidebar is visible. */
    private boolean mIsInWaypointEditMode = false;
    /** Markers of the mission items. */
    private final HashBiMap<NavMissionItem, Marker> mMarkers = HashBiMap.create();
    /** Line connecting the mission items to be drawn on map. */
    private Polyline mMissionPathLine = null;
    /** Color of the line connecting the mission items on map. */
    private static final int MISSION_PATH_DEFAULT_COLOR = Color.WHITE;
    /** Width of the line connecting the mission items on map in screen pixels. */
    private static final int MISSION_PATH_DEFAULT_WIDTH = 4;
    /** Indicates that no waypoint shall be highlighted. */
    private static final int NO_WAYPOINT = -1;
    /** Currently highlighted waypoint. */
    private int mSelectedWaypoint = NO_WAYPOINT;
    /** Index of waypoint added or removed last time. */
    private int mChangedWaypoint = NO_WAYPOINT;

    /** List of polygons representing obstacles on map. */
    private List<Polygon> mDisplayedObstacles = null;
    /** Path in the direction of the vehicle checked for collision. */
    private Polyline mPredictedPathLine = null;
    /** Line representing boundary of the ASTER GDEM terrain data. */
    private Polyline mDemBoundaryLine = null;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMissionItemListFragment = new MissionItemListFragment();
        mOverviewFragment = new OverviewFragment();

        // Create a new global location parameters object
        mDeviceLocationRequest = LocationRequest.create();
        // Set the update interval
        mDeviceLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Use high accuracy
        mDeviceLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the interval ceiling to one minute
        mDeviceLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Create a new location client, using the enclosing class to handle callbacks
        mDeviceLocationClient = new LocationClient(getActivity(), this, this);
    }

    /*
     * (non-Javadoc) Initialize LocationFragment's UI.
     * 
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.location_fragment, container, false);

        if (savedInstanceState != null) {
            // Load saved data
            mSelectedWaypoint = savedInstanceState.getInt("selectedWaypoint", NO_WAYPOINT);
            mChangedWaypoint = savedInstanceState.getInt("changedWaypoint", NO_WAYPOINT);
            mIsInWaypointEditMode = savedInstanceState.getBoolean("isInWaypointEditMode", false);
        }

        // Adds two fragments to the side panel: one with flight data overview and one with
        // mission item list.
        // Hide the mission item list by default so the overview fragment is visible.
        FragmentManager fm = getChildFragmentManager();
        fm.popBackStackImmediate();
        FragmentTransaction ft = fm.beginTransaction();
        if (fm.findFragmentByTag("overview") == null) {
            ft.add(R.id.side_map_fragment, mOverviewFragment, "overview");
        }
        if (fm.findFragmentByTag("mission") == null) {
            ft.add(R.id.side_map_fragment, mMissionItemListFragment, "mission");
            if (!mIsInWaypointEditMode)
                ft.hide(mMissionItemListFragment);
        }
        ft.addToBackStack(null);
        ft.commit();

        // Get reference to the UI elements
        mVehicleLocationButton = (ImageButton) rootView.findViewById(R.id.vehicle_location_button);
        mConnectionStatus = (TextView) rootView.findViewById(R.id.text_connection_status);
        mMapView = (ListeningMapView) rootView.findViewById(R.id.map_view);
        mEditWaypointsButton = (ImageButton) rootView.findViewById(R.id.edit_waypoints_button);
        if (mIsInWaypointEditMode)
            mEditWaypointsButton.setImageResource(R.drawable.ic_edit_waypoints_exit);

        mVehicleLocationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clicking on Vehicle Location button starts the vehicle following mode - camera
                // gets centered to vehicle location on each location update
                mFollowVehicle = true;
                // Button disappears - it gets visible again when user manually moves with the map
                mVehicleLocationButton.setVisibility(View.INVISIBLE);
                // If any location is known at this point, move camera to it. Wait for the location
                // otherwise.
                if (mLatestVehicleLocation != null) {
                    moveMapToLocation(mLatestVehicleLocation);
                } else {
                    SkyControlUtils.toast(getString(R.string.location_not_acquired),
                            Toast.LENGTH_LONG);
                }
            }
        });

        // Show or hide the waypoint list sidebar on button click
        mEditWaypointsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clicking on edit waypoints button changes the side map fragment to show or hide
                // list of mission waypoints
                showWaypointList();
            }
        });

        // Initialize a map within MapView by calling onCreate
        mMapView.onCreate(savedInstanceState);
        mMapView.registerMapViewListener(this);

        // Get the map within MapView
        mMap = mMapView.getMap();
        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(getActivity());
        // Load map camera position from last app run
        loadCameraPosition();
        // Add listeners to map events
        addMapListeners();

        // Add listeners to receive updates from the other classes
        Vehicle.get().getEvents().addVehicleListener(this);
        Connection.get().getEvents().addConnectionListener(this);
        Mission.get().getEvents().addMissionListener(this);
        CollisionAvoidance.get().getEvents().addCollisionListener(this);

        return rootView;
    }

    /*
     * (non-Javadoc) Called when the Fragment is reinstated, even before it becomes visible. Need to
     * connect to the Location Client to be able to receive location updates.
     * 
     * @see android.support.v4.app.Fragment#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();
        // Once the Location Client is connected, location updates get requested.
        if (mDeviceLocationClient != null)
            mDeviceLocationClient.connect();
    }

    /*
     * (non-Javadoc) Call MapView onResume method.
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        renderWaypoints();
        renderDemBoundary();
        renderObstaclesOnMap();

        if (mMapView != null)
            mMapView.onResume();
    }


    /*
     * (non-Javadoc) Call MapView onPause method.
     * 
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        mDemBoundaryLine = removeMapObject(mDemBoundaryLine);
        mMissionPathLine = removeMapObject(mMissionPathLine);
        mPredictedPathLine = removeMapObject(mPredictedPathLine);
        removeObstaclesFromMap();

        super.onPause();
        if (mMapView != null)
            mMapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("selectedWaypoint", mSelectedWaypoint);
        outState.putInt("changedWaypoint", mChangedWaypoint);
        outState.putBoolean("isInWaypointEditMode", mIsInWaypointEditMode);
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc) Called when the Activity is no longer visible and is stopped. Stop location
     * updates and disconnect from Location Client.
     * 
     * @see android.support.v4.app.Fragment#onStop()
     */
    @Override
    public void onStop() {
        // Location Client may not be instantiated when Google Play service is not installed
        if (mDeviceLocationClient != null) {
            if (mDeviceLocationClient.isConnected()) {
                stopPeriodicUpdates();
            }
            // After disconnect() is called, the client is considered "dead".
            mDeviceLocationClient.disconnect();
        }
        // Remove any device bitmap from map
        removeDeviceBitmap();

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        // This is the last stage where the fragment view exists and because the events do changes
        // to view this is the place to remove their listeners
        Connection.get().getEvents().removeConnectionListener(this);
        Mission.get().getEvents().removeMissionListener(this);
        Vehicle.get().getEvents().removeVehicleListener(this);
        CollisionAvoidance.get().getEvents().addCollisionListener(this);

        super.onDestroyView();
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

    /*
     * (non-Javadoc) Call MapView onDestroy method.
     * 
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        // Save map camera position to be able to load it on next app start
        saveCameraPosition();

        mMapView.unregisterMapViewListener();

        if (mMapView != null)
            mMapView.onDestroy();

        mVehicleMarker = removeMapObject(mVehicleMarker);
        super.onDestroy();
    }

    /*
     * (non-Javadoc) Call MapView onLowMemory method.
     * 
     * @see android.support.v4.app.Fragment#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null)
            mMapView.onLowMemory();
    }

    /**
     * Renders current waypoint markers and highlights the selected waypoint. Gets called when
     * sidebar waypoint list ( {@link MissionItemListFragment}) is instantiated.
     */
    protected void renderWaypoints() {
        // Render markers on map and connect them with polyline
        updateMissionItemMarkers(Mission.get().getNavMissionItems());
        // Highlight previously selected waypoint
        highlightWaypoint(mSelectedWaypoint, false);
    }

    /**
     * Shows or hides waypoint list in side map fragment based on its current visibility.
     */
    private void showWaypointList() {
        if (mIsInWaypointEditMode) {
            showWaypointList(false);
        } else {
            showWaypointList(true);
        }
    }

    /**
     * Shows or hides waypoint list in side map fragment based on the passed boolean.
     * 
     * @param show true to show the list, false to hide it
     */
    private void showWaypointList(boolean show) {
        // Clicking on edit waypoints button changes the side map fragment to show list of
        // mission waypoints
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        if (show) {
            if (mIsInWaypointEditMode)
                // Do not try to show already shown fragment
                return;
            ft.show(mMissionItemListFragment);
            mIsInWaypointEditMode = true;
            mEditWaypointsButton.setImageResource(R.drawable.ic_edit_waypoints_exit);
        } else {
            if (!mIsInWaypointEditMode)
                // Do not try to hide already hidden fragment
                return;
            ft.hide(mMissionItemListFragment);
            mIsInWaypointEditMode = false;
            mEditWaypointsButton.setImageResource(R.drawable.ic_edit_waypoints);
        }
        // Start the animated transition.
        ft.commit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bocekm.skycontrol.ListeningMapView.MapViewListener#onMapTouch()
     */
    @Override
    public void onMapTouch() {
        // Once the user manually moves the map, stop the vehicle following mode and show
        // the Vehicle Location button.
        mFollowVehicle = false;
        mVehicleLocationButton.setVisibility(View.VISIBLE);
    }

    /**
     * Saves current map camera position to shared preferences.
     */
    private void saveCameraPosition() {
        CameraPosition camera = mMap.getCameraPosition();
        SharedPreferences settings =
                getActivity().getSharedPreferences(MAP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(PREF_LAT, (float) camera.target.latitude);
        editor.putFloat(PREF_LNG, (float) camera.target.longitude);
        editor.putFloat(PREF_BEA, camera.bearing);
        editor.putFloat(PREF_TILT, camera.tilt);
        editor.putFloat(PREF_ZOOM, camera.zoom);
        editor.apply();
    }

    /**
     * Updates marker corresponding to the mission item in hash or create new marker if it doesn't
     * exist yet.
     * 
     * @param item waypoint for which the marker should be updated/created
     */
    private void updateMarker(NavMissionItem item) {
        final LatLng position = item.getPosition();
        Marker marker = mMarkers.get(item);
        if (marker == null) {
            // Create new marker
            marker = mMap.addMarker(new MarkerOptions().position(position));
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker.setDraggable(true);
            mMarkers.put(item, marker);
        }
        marker.setPosition(position);
    }

    /**
     * Shows correct mission item markers on the map.
     * 
     * @param list list with the updated mission items for which the markers will be shown on map
     */
    private void updateMissionItemMarkers(List<NavMissionItem> list) {
        if (mChangedWaypoint >= list.size())
            mSelectedWaypoint = list.size() - 1;

        removeOldMissionItemMarkers(list);
        for (NavMissionItem item : list) {
            updateMarker(item);
        }
        highlightWaypoint(mSelectedWaypoint, false);
        updateMissionPath();
    }

    /**
     * Removes from hash such mission items that exist no more in the updated list and also removes
     * their corresponding markers from the map.
     * 
     * @param list list with the updated mission items
     */
    private void removeOldMissionItemMarkers(List<NavMissionItem> list) {
        Iterator<Map.Entry<NavMissionItem, Marker>> it = mMarkers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<NavMissionItem, Marker> entry = (Map.Entry<NavMissionItem, Marker>) it.next();
            NavMissionItem missionItem = entry.getKey();
            if (!list.contains(missionItem)) {
                Marker marker = entry.getValue();
                marker.remove();
                it.remove();
            }
        }
    }

    /**
     * Loads map camera position from shared preferences.
     */
    private void loadCameraPosition() {
        CameraPosition.Builder camera = new CameraPosition.Builder();
        SharedPreferences settings =
                getActivity().getSharedPreferences(MAP_PREFERENCES, Context.MODE_PRIVATE);
        camera.bearing(settings.getFloat(PREF_BEA, 0));
        camera.tilt(settings.getFloat(PREF_TILT, 0));
        camera.zoom(settings.getFloat(PREF_ZOOM, 0));
        camera.target(new LatLng(settings.getFloat(PREF_LAT, 0), settings.getFloat(PREF_LNG, 0)));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()));
    }

    /**
     * Adds listeners to the map events.
     */
    private void addMapListeners() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            /*
             * (non-Javadoc) Create new waypoint on map long-click.
             * 
             * @see
             * com.google.android.gms.maps.GoogleMap.OnMapLongClickListener#onMapLongClick(com.google
             * .android.gms.maps.model.LatLng)
             */
            @Override
            public void onMapLongClick(LatLng latLng) {
                Waypoint wp = new Waypoint(Mission.get().getMissionItemList());
                wp.setPosition(latLng);
                wp.setDefaultAltitude();
                wp.toastOnCollision();
                Mission.get().addMissionItem(wp);
                mChangedWaypoint = Mission.get().getNavMissionItemIndex(wp);
            }
        });

        // Update the waypoint global position when its corresponding marker is dragged
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                updateMissionItemPosition(marker);
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                updateMissionItemPosition(marker);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updateMissionItemPosition(marker);
            }
        });

        // Highlight the waypoint corresponding to the clicked marker in the waypoint list
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                NavMissionItem item = (NavMissionItem) getMissionItem(marker);
                if (item instanceof NavMissionItem) {
                    highlightWaypoint(item, false);
                }
                // Return true to indicate the marker click has been handled and no default onclick
                // behavior should occur
                return true;
            }
        });
    }

    /**
     * Highlights the waypoint based on index of the waypoint in mission.
     * 
     * @param waypointIndex index of the waypoint in the list of waypoints
     * @param focusMapOnMarker whether to focus map on marker
     */
    protected void highlightWaypoint(int waypointIndex, boolean focusMapOnMarker) {
        if (waypointIndex < 0)
            return;
        NavMissionItem item = Mission.get().getNavMissionItemOnIndex(waypointIndex);
        highlightWaypoint(item, focusMapOnMarker);
    }

    /**
     * Highlights the waypoint by selecting it in the list of waypoints and by changing the marker
     * color to red.
     * 
     * @param waypoint the waypoint to be highlighted
     * @param focusMapOnMarker whether to focus map on marker
     */
    protected void highlightWaypoint(NavMissionItem waypoint, boolean focusMapOnMarker) {
        // Show the waypoint list in map sidebar if not shown already
        showWaypointList(true);
        // Get waypoint zero-based index within mission
        int waypointIndex = waypoint.getIndexInNavMission();

        ListView lv = mMissionItemListFragment.getListView();
        // The list with waypoints may not be instantiated yet (loads in separate thread); if so,
        // skip operations upon it, this method will be run again once the list gets instantiated
        if (lv != null) {
            // Highlight the waypoint in the list
            lv.setItemChecked(waypointIndex, true);
            // Scroll to the waypoint if not in visible area
            lv.setSelection(waypointIndex);
        }
        // Unhighlight previously highlighted marker
        Marker marker;
        if (mSelectedWaypoint >= 0) {
            marker = getMarkerOnIndex(mSelectedWaypoint);
            if (marker != null)
                marker.setIcon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }
        // Get marker of the waypoint to highlight
        mSelectedWaypoint = waypoint.getIndexInNavMission();
        marker = getMarkerOnIndex(mSelectedWaypoint);
        if (marker != null)
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        if (focusMapOnMarker)
            moveMapToLocation(waypoint.getPosition());
    }

    /**
     * Gets mission item corresponding to the marker.
     * 
     * @param marker the marker
     * @return the mission item
     */
    private NavMissionItem getMissionItem(Marker marker) {
        return mMarkers.inverse().get(marker);
    }

    /**
     * Gets marker corresponding to the mission item.
     * 
     * @param index index of the mission item
     * @return the corresponding marker
     */
    private Marker getMarkerOnIndex(int index) {
        List<NavMissionItem> list = Mission.get().getNavMissionItems();
        if (index >= list.size())
            return null;
        NavMissionItem item = list.get(index);
        return mMarkers.get(item);
    }

    /**
     * Updates global position of the mission item based on the position of the marker on the map.
     * 
     * @param marker the marker
     */
    private void updateMissionItemPosition(Marker marker) {
        NavMissionItem item = (NavMissionItem) getMissionItem(marker);
        if (item instanceof NavMissionItem) {
            item.setPosition(marker.getPosition());
            item.toastOnCollision();
        }
        updateMissionPath();
    }

    /**
     * Draws a line connecting the mission item markers.
     */
    private void updateMissionPath() {
        final List<LatLng> pathPoints = new ArrayList<LatLng>();
        for (NavMissionItem missionItem : Mission.get().getNavMissionItems()) {
            pathPoints.add(missionItem.getPosition());
        }

        if (mMissionPathLine == null) {
            PolylineOptions pathOptions = new PolylineOptions();
            pathOptions.color(MISSION_PATH_DEFAULT_COLOR).width(MISSION_PATH_DEFAULT_WIDTH);
            mMissionPathLine = mMap.addPolyline(pathOptions);
        }
        mMissionPathLine.setPoints(pathPoints);
    }

    /**
     * Draws a polygon on a map to show boundaries of the loaded digital elevation model.
     */
    private void renderDemBoundary() {
        if (CollisionAvoidance.get().getElevationModel() == null)
            return;
        if (mDemBoundaryLine == null) {
            PolylineOptions pathOptions = new PolylineOptions();
            pathOptions.color(MISSION_PATH_DEFAULT_COLOR).width(MISSION_PATH_DEFAULT_WIDTH);
            mDemBoundaryLine = mMap.addPolyline(pathOptions);
        }
        mDemBoundaryLine
                .setPoints(CollisionAvoidance.get().getElevationModel().getBoundaryPoints());
    }

    /**
     * Verifies that Google Play Services are available before making a request.
     * 
     * @param c Context
     * @return true if Google Play Services are available, otherwise false
     */
    public static boolean isLocationServiceAvailable(Context c) {
        // Check Google Play service availability
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(c);
        if (resultCode != ConnectionResult.SUCCESS) {
            // Display an error dialog with possibilities to recover
            showErrorDialog(resultCode, 0, (FragmentActivity) c);
            return false;
        }
        return true;
    }

    /**
     * Sends a request to Location Services to start sending location of the Android device
     * periodically.
     */
    private void startPeriodicUpdates() {
        mDeviceLocationClient.requestLocationUpdates(mDeviceLocationRequest, this);
    }

    /**
     * Sends a request to Location Services to stop location updates.
     */
    private void stopPeriodicUpdates() {
        mDeviceLocationClient.removeLocationUpdates(this);
    }

    /**
     * Shows a dialog returned by Google Play services for the connection error code.
     * 
     * @param errorCode an error code returned from onConnectionFailed
     * @param requestCode the request code
     * @param c context of the Activity
     */
    private static void showErrorDialog(int errorCode, int requestCode, FragmentActivity c) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, c, requestCode);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(c.getSupportFragmentManager(), SkyControlConst.DEBUG_TAG);
        }
    }

    /**
     * Handle last known location of the Android device. It may be outdated though.
     */
    private void handleLastKnownLocation() {
        // If Google Play Services is available
        if (isLocationServiceAvailable(getActivity())) {
            // Get the current location
            Location lastKnown = mDeviceLocationClient.getLastLocation();
            if (lastKnown != null)
                handleDeviceLocation(lastKnown, true);
        }
    }

    /*
     * (non-Javadoc) Handles results returned to this Fragment from other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * onConnectionFailed may call startResolutionForResult() to start an Activity that handles
     * Google Play services problems. The result of this call returns here, to onActivityResult.
     * 
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
        // If the request code matches the code sent in onConnectionFailed
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode) {
                // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        // Log the result
                        Log.d(SkyControlConst.DEBUG_TAG, getString(R.string.resolved));
                        // Hide the status text
                        mConnectionStatus.setVisibility(View.INVISIBLE);
                        break;
                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(SkyControlConst.DEBUG_TAG, getString(R.string.no_resolution));
                        // Display the result
                        mConnectionStatus.setText(R.string.no_resolution);
                        mConnectionStatus.setVisibility(View.VISIBLE);
                        break;
                }
                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(SkyControlConst.DEBUG_TAG, "Unknown Activity request code");
                break;
        }
    }

    /*
     * (non-Javadoc) Called by Location Services if the attempt to Location Services fails.
     * 
     * @see com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener#
     * onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects. If the error has a resolution,
         * try sending an Intent to start a Google Play services activity that can resolve error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getActivity(),
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                Log.e(SkyControlConst.ERROR_TAG, e.getLocalizedMessage());
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode(), CONNECTION_FAILURE_RESOLUTION_REQUEST,
                    getActivity());
        }
    }

    /*
     * (non-Javadoc) Called by Location Services when the request to connect the client finishes
     * successfully. At this point, we request the last known location of the Android device and
     * start periodic updates.
     * 
     * @see
     * com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks#onConnected(android
     * .os.Bundle)
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(SkyControlConst.DEBUG_TAG, getString(R.string.location_connected));
        handleLastKnownLocation();
        startPeriodicUpdates();
    }

    /*
     * (non-Javadoc) Called by Location Services if the connection to the location client drops
     * because of an error.
     * 
     * @see
     * com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks#onDisconnected()
     */
    @Override
    public void onDisconnected() {
        Log.e(SkyControlConst.ERROR_TAG, getString(R.string.location_disconnected));
    }

    /*
     * (non-Javadoc) Reports Android device location updates to the UI.
     * 
     * @see
     * com.google.android.gms.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            handleDeviceLocation(location, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener#onVehicleEvent(com.bocekm.skycontrol
     * .vehicle.VehicleEvents.VehicleEvent)
     */
    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case ATTITUDE:
                // Get the vehicle heading for rotation of the vehicle marker on the map
                // Needs to be converted to degrees
                mVehicleHeading = Vehicle.get().getAttitude().getYaw() * 180.0 / Math.PI;
                break;
            case POSITION:
                // Pass the new vehicle location to be handled appropriately by this fragment
                handleVehicleLocation(Vehicle.get().getPosition().getPosition());
                break;
            case HEARTBEAT_TIMEOUT:
                if (mLatestVehicleLocation != null) {
                    mConnectionStatus.setText(R.string.vehicle_lost_map_status);
                    mConnectionStatus.setVisibility(View.VISIBLE);
                    // Show the question mark on last known vehicle position
                    mVehicleLostMarker = removeMapObject(mVehicleLostMarker);
                    mVehicleLostMarker =
                            mMap.addMarker(new MarkerOptions().position(mLatestVehicleLocation)
                                    .anchor(0.5f, 0.5f).flat(true));
                    // Imprint the image of question mark to the map
                    mVehicleLostMarker.setIcon(BitmapDescriptorFactory
                            .fromResource(R.drawable.vehicle_lost));
                }
                break;
            case VEHICLE_CONNECTED:
            case HEARTBEAT_RESTORED:
                // Show text saying the vehicle has connected
                mConnectionStatus.setText(R.string.vehicle_connected_map_status);
                mConnectionStatus.setVisibility(View.VISIBLE);
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
            case MISSION_RECEIVED:
                // New mission has arrived - invalidate any selection to previous waypoints
                mChangedWaypoint = NO_WAYPOINT;
                mSelectedWaypoint = NO_WAYPOINT;
                // Show the new waypoints on map
                renderWaypoints();
                break;
            case MISSION_UPDATE:
                // Re-render the waypoint markers on the map
                renderWaypoints();
                break;
            case WAYPOINT_ADDED:
                // Get last waypoint of the mission and highlight it assuming that each new waypoint
                // is added at the end of the list
                List<NavMissionItem> list = Mission.get().getNavMissionItems();
                highlightWaypoint(list.size() - 1, false);
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener#onConnectionEvent(com
     * .bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent)
     */
    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                // Remove markers representing the vehicle position
                mVehicleLostMarker = removeMapObject(mVehicleLostMarker);
                mVehicleMarker = removeMapObject(mVehicleMarker);
                mPredictedPathLine = removeMapObject(mPredictedPathLine);
                mLatestVehicleLocation = null;
                mConnectionStatus.setText(R.string.waiting_telemetry_map_status);
                mConnectionStatus.setVisibility(View.VISIBLE);
                break;
            case SERVICE_BOUND:
                mConnectionStatus.setText(R.string.waiting_vehicle_map_status);
                mConnectionStatus.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCollisionEvent(CollisionEvent event) {
        switch (event) {
            case OBSTACLES_LOADED:
                renderObstaclesOnMap();
                renderDemBoundary();
                break;
            case CHECKPOINT_POSITION_UPDATED:
                renderPredictedPathOnMap(CollisionAvoidance.get().getCheckpointPosition());
                break;
            case OBSTACLES_DESTROYED:
                removeObstaclesFromMap();
                mDemBoundaryLine = removeMapObject(mDemBoundaryLine);
                mPredictedPathLine = removeMapObject(mPredictedPathLine);
                break;
            default:
                break;
        }
    }

    /**
     * Handles new Android device location.
     * 
     * @param location location to be handled
     * @param lastKnown says whether the location is last known or accurate
     */
    private void handleDeviceLocation(Location location, boolean lastKnown) {
        // Convert Android location to Google Play services format
        LatLng locationGoogle = new LatLng(location.getLatitude(), location.getLongitude());
        // Set initial fake vehicle position
        if (Vehicle.get().getPosition().getFakePosition() == null
                && Vehicle.get().getPosition().isFakeGpsEnabled())
            Vehicle.get().getPosition().setFakePosition(locationGoogle);
        // Log the location in the Log
        SkyControlUtils.log("Device position: " + SkyControlUtils.locationToString(locationGoogle)
                + "\n", false);

        // If the device marker has not been rendered yet
        if (mDeviceMarkerResource == NO_RESOURCE_SET) {
            // Then add the device marker on the map, anchor the marker in the center of the
            // image, flat = rotate the marker together with the map rotation
            mDeviceMarker =
                    mMap.addMarker(new MarkerOptions().position(locationGoogle).anchor(0.5f, 0.5f)
                            .flat(true));
            // Choose the image of the marker
            setDeviceBitmap(lastKnown);
        } else {
            // Change the vehicle image if needed
            setDeviceBitmap(lastKnown);
            // Move the marker to the updated location
            mDeviceMarker.setPosition(locationGoogle);
        }
    }

    /**
     * Handles new vehicle location.
     * 
     * @param location vehicle location to handle
     */
    private void handleVehicleLocation(LatLng location) {
        // Save the latest location for later use
        mLatestVehicleLocation = location;
        // Move the map position to the new acquired location (if user didn't manually moved the map
        // disallowing the vehicle location following)
        if (mFollowVehicle)
            moveMapToLocation(location);

        // Remove the question mark if it was set due to lost vehicle heartbeat
        mVehicleLostMarker = removeMapObject(mVehicleLostMarker);
        // Remove text from status windows saying we are waiting for position
        mConnectionStatus.setVisibility(View.INVISIBLE);

        if (mVehicleMarker == null) {
            // Tell user vehicle position was acquired
            SkyControlUtils
                    .toast(getString(R.string.vehicle_position_acquired), Toast.LENGTH_SHORT);
            // If the airplane marker has not been rendered yet
            // Add the Airplane marker on the map
            mVehicleMarker =
                    mMap.addMarker(new MarkerOptions().position(location).anchor(0.5f, 0.5f)
                            .flat(true));
            mVehicleMarker.setRotation((float) mVehicleHeading);
            // Imprint the image of vehicle to the map
            mVehicleMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.vehicle));
        } else {
            // Move the marker to the updated location
            mVehicleMarker.setPosition(location);
        }
        // Rotate the marker based on current vehicle heading
        mVehicleMarker.setRotation((float) mVehicleHeading);
    }

    /**
     * Chooses from two images based on accuracy of the location. Last known location may be quite
     * inaccurate.
     * 
     * @param lastKnown says whether the location is last known or accurate
     */
    private void setDeviceBitmap(boolean lastKnown) {
        if (lastKnown && mDeviceMarkerResource != R.drawable.inaccurate_home) {
            mDeviceMarkerResource = R.drawable.inaccurate_home;
            mDeviceMarker.setIcon(BitmapDescriptorFactory.fromResource(mDeviceMarkerResource));
        } else if (!lastKnown && mDeviceMarkerResource != R.drawable.accurate_home) {
            mDeviceMarkerResource = R.drawable.accurate_home;
            mDeviceMarker.setIcon(BitmapDescriptorFactory.fromResource(mDeviceMarkerResource));
        }
    }

    /**
     * Removes the map icon representing an Android device.
     */
    private void removeDeviceBitmap() {
        mDeviceMarker = removeMapObject(mDeviceMarker);
        mDeviceMarkerResource = NO_RESOURCE_SET;
    }

    /**
     * Removes the object from map (may it be marker or polyline). See the return value description.
     * 
     * @param object instance of the object
     * @return null; make sure you set the object you are removing to this return value. Behavior of
     *         the methods on removed map objects is undefined.
     */
    private <T> T removeMapObject(T object) {
        if (object == null)
            return null;
        if (object instanceof Marker)
            ((Marker) object).remove();
        if (object instanceof Polyline)
            ((Polyline) object).remove();
        return null;
    }

    /**
     * Changes map camera focus to the location passed as a argument.
     * 
     * @param location the location
     */
    private void moveMapToLocation(LatLng location) {
        // Get the parameters of the current camera to retain what the user set (zoom, bearing)
        CameraPosition oldPosition = mMap.getCameraPosition();
        // Create new camera settings based on the previous camera settings
        CameraPosition.Builder builder = CameraPosition.builder(oldPosition);
        // Update the location of the new camera settings
        builder.target(location);
        // Zoom in if the previous camera was too much zoomed out
        if (oldPosition.zoom <= 13)
            builder.zoom(15);
        CameraPosition newPosition = builder.build();
        // move the camera to the acquired location
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition));
        }
    }

    /**
     * Keeps index of the last changed (removed/added) waypoint.
     * 
     * @param changedWaypoint index of the changed waypoint
     */
    protected void keepWaypointIndex(int changedWaypoint) {
        mChangedWaypoint = changedWaypoint;
        if (changedWaypoint < mSelectedWaypoint)
            mSelectedWaypoint--;
    }

    /**
     * Render line on map representing path in front of the vehicle which is being checked for
     * collision.
     * 
     * @param checkpointPosition position of the most distant point in front of vehicle checked for
     *        collision
     */
    private void renderPredictedPathOnMap(LatLng checkpointPosition) {
        mPredictedPathLine = removeMapObject(mPredictedPathLine);
        PolylineOptions pathOptions =
                new PolylineOptions().color(MISSION_PATH_DEFAULT_COLOR)
                        .width(MISSION_PATH_DEFAULT_WIDTH)
                        .add(Vehicle.get().getPosition().getPosition()).add(checkpointPosition);
        mPredictedPathLine = mMap.addPolyline(pathOptions);
    }

    /**
     * Render polygons on map representing obstacles loaded from an XML.
     */
    private void renderObstaclesOnMap() {
        if (CollisionAvoidance.get().getObstacles() == null)
            return;
        mDisplayedObstacles = new ArrayList<Polygon>();
        // Go through all obstacles parsed from an XML
        for (Obstacle obstacle : CollisionAvoidance.get().getObstacles()) {
            // Instantiates a new Polygon object and adds points to define a rectangle
            PolygonOptions polygonOptions = new PolygonOptions();
            com.vividsolutions.jts.geom.Polygon obstaclePolygon = obstacle.getPolygon();
            Coordinate[] polygonCoords = obstaclePolygon.getCoordinates();
            for (Coordinate coord : polygonCoords) {
                // x .. longitude, y .. latitude
                polygonOptions.add(new LatLng(coord.y, coord.x));
            }
            polygonOptions.strokeColor(Color.parseColor("#88E60000"))
                    .fillColor(Color.parseColor("#88A37A00"))
                    .strokeWidth(MISSION_PATH_DEFAULT_WIDTH).geodesic(true);

            Polygon polygon = mMap.addPolygon(polygonOptions);
            // Keep the reference to the polygon so we may remove it from map later
            mDisplayedObstacles.add(polygon);
        }
    }

    /**
     * Removes polygons from map representing obstacles.
     */
    private void removeObstaclesFromMap() {
        if (mDisplayedObstacles == null)
            return;
        for (Polygon obstacle : mDisplayedObstacles) {
            obstacle.remove();
        }
        mDisplayedObstacles.clear();
        mDisplayedObstacles = null;
    }
}
