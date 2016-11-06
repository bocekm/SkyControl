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
package com.bocekm.skycontrol.cas;

import java.io.IOException;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.cas.CollisionEvents.CollisionEvent;
import com.bocekm.skycontrol.connection.Connection;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionEvent;
import com.bocekm.skycontrol.connection.ConnectionEvents.ConnectionListener;
import com.bocekm.skycontrol.mission.Mission;
import com.bocekm.skycontrol.vehicle.Vehicle;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleEvent;
import com.bocekm.skycontrol.vehicle.VehicleEvents.VehicleListener;
import com.google.android.gms.maps.model.LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * {@link CollisionAvoidance} class is a singleton loaded at start of the app. Holds reference to
 * the loaded obstacle and terrain data and contains methods checking whether a particular
 * position+altitude lies within terrain/obstacle. The class gets actually initialized twice. First
 * init is from {@link SkyControlApp} and then to load data from files,
 * {@link CollisionAvoidance#onCreate()} needs to be called. The caller (Activity) must not forget
 * to call also {@link CollisionAvoidance#onDestroy()} on it's own destruction to release listeners
 * and opened files.
 */
public class CollisionAvoidance implements
        VehicleListener,
        OnSharedPreferenceChangeListener,
        ConnectionListener {

    /** This {@link CollisionAvoidance} class instance. */
    private static CollisionAvoidance sCollisionAvoidance;
    private static CollisionEvents sCollisionEvents;

    private TiffParser mElevationModel = null;
    private List<Obstacle> mObstacles = null;
    private boolean mCasEnabled = true;

    /**
     * This constant defines in how many seconds the vehicle achieves position which is to be
     * checked for an obstacle.
     */
    private int mDistanceToCheckpointInS = 0;
    private LatLng mCheckpointPosition = null;
    private boolean mDangerOfCollision = false;

    /** Specifying the WGS-84 datum (EPSG:4326 Coordinate Reference System) */
    private static final GeometryFactory sGeometryFactory = new GeometryFactory(
            new PrecisionModel(), 4326);

    /**
     * Constructor of {@link CollisionAvoidance} class (private because it's a singleton).
     */
    private CollisionAvoidance() {
        sCollisionEvents = new CollisionEvents();
        Vehicle.get().getEvents().addVehicleListener(this);
        Connection.get().getEvents().addConnectionListener(this);
    }

    /**
     * Instantiates new {@link CollisionAvoidance} object only if it wasn't instantiated before,
     * because it's singleton so just one instance exists per running app.
     * 
     * @return instance of the {@link CollisionAvoidance} singleton
     */
    public static CollisionAvoidance init() {
        if (sCollisionAvoidance == null)
            sCollisionAvoidance = new CollisionAvoidance();
        return sCollisionAvoidance;
    }

    /**
     * Returns the only instance of {@link Mission} singleton.
     * 
     * @return reference to {@link Mission} singleton instance
     */
    public static CollisionAvoidance get() {
        return sCollisionAvoidance;
    }

    /**
     * Class data initialization. Shall be called at the beginning of the application so other
     * classes can make use of this class.
     */
    public void onCreate() {
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        // Register on change listener on user settings
        userPref.registerOnSharedPreferenceChangeListener(this);
        mDistanceToCheckpointInS = userPref.getInt(PreferencesFragment.CAS_DISTANCE_PREF_KEY, 1);
        mCasEnabled = userPref.getBoolean(PreferencesFragment.CAS_ENABLED_PREF_KEY, false);
        if (mCasEnabled)
            loadObstacles();
    }

    /**
     * Shall be called by the initiator of the instance of this class on before it's own
     * destruction.
     */
    public void onDestroy() {
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        userPref.unregisterOnSharedPreferenceChangeListener(this);
        sCollisionEvents.onCollisionEvent(CollisionEvent.CLEAR_OF_COLLISION);
        destroyObstacles();
    }

    /**
     * Loads obstacles from an XML and terrain elevation model from GeoTIFF.
     */
    private void loadObstacles() {
        Obstacles obstacles = XmlParser.parseObstaclesFromXml();
        mObstacles = obstacles.getObstacleList();

        mElevationModel = new TiffParser();
        try {
            mElevationModel.parseGeoTiff(SkyControlConst.ELEVATION_FILE_ASSET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Destroy loaded obstacle data, closes handle to the opened GeoTIFF file.
     */
    private void destroyObstacles() {
        if (mElevationModel != null) {
            mElevationModel.closeTiff();
            mElevationModel = null;
        }

        if (mObstacles != null) {
            mObstacles.clear();
            mObstacles = null;
        }
    }

    /**
     * Checks whether any obstacle is present in front of the vehicle.
     */
    private void checkForCollision() {
        float currentAltitude = Vehicle.get().getAltitude().getAltitude();
        LatLng currentPosition = Vehicle.get().getPosition().getPosition();
        float currentVertSpeed = Vehicle.get().getSpeed().getVerticalSpeed();
        float currentGroundspeed = Vehicle.get().getSpeed().getGroundspeed();
        float currentHeading = Vehicle.get().getAttitude().getYawInDegrees();

        float predictedCheckpointAltitude =
                currentAltitude + currentVertSpeed * mDistanceToCheckpointInS;

        // Distance which will the vehicle achieve in DISTANCE_TO_CHECKPOINT_IN_S seconds
        double distanceOfCheckpoint = mDistanceToCheckpointInS * currentGroundspeed;

        mCheckpointPosition =
                SkyControlUtils.getDestination(currentPosition, currentHeading,
                        distanceOfCheckpoint);
        sCollisionEvents.onCollisionEvent(CollisionEvent.CHECKPOINT_POSITION_UPDATED);

        boolean dangerOfCollision =
                checkForCollision(currentPosition, mCheckpointPosition, predictedCheckpointAltitude);

        if (dangerOfCollision && !mDangerOfCollision) {
            // Send notification to listeners in case danger of collision has been detected
            mDangerOfCollision = true;
            sCollisionEvents.onCollisionEvent(CollisionEvent.DANGER_OF_COLLISION);
            SkyControlUtils.log("Danger of collision with obstacle/terrain\n", true);
        } else if (!dangerOfCollision && mDangerOfCollision) {
            // Send notification to listeners in case danger is no longer imminent
            mDangerOfCollision = false;
            sCollisionEvents.onCollisionEvent(CollisionEvent.CLEAR_OF_COLLISION);
        }
    }

    /**
     * Check for collision with both terrain and obstacles. Also creates a line between
     * currentPosition and checkpointPosition, which is checked whether it's interrupted by
     * obstacle.
     * 
     * @param currentPosition the current position
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForCollision(LatLng currentPosition, LatLng checkpointPosition,
            float checkpointAltitude) {
        boolean obstacleCollision =
                checkForObstacleCollision(currentPosition, checkpointPosition, checkpointAltitude);
        boolean terrainCollision = checkForTerrainCollision(checkpointPosition, checkpointAltitude);
        return (obstacleCollision || terrainCollision);
    }

    /**
     * Check for collision with both terrain and obstacles using Point instead of LatLng. Also
     * creates a line between currentPosition and checkpointPosition, which is checked whether it's
     * interrupted by obstacle.
     * 
     * @param currentPosition the current position
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForCollision(Point currentPosition, Point checkpointPosition,
            float checkpointAltitude) {
        boolean obstacleCollision =
                checkForObstacleCollision(currentPosition, checkpointPosition, checkpointAltitude);
        LatLng checkpoint = new LatLng(checkpointPosition.getY(), checkpointPosition.getX());
        boolean terrainCollision = checkForTerrainCollision(checkpoint, checkpointAltitude);
        return (obstacleCollision || terrainCollision);
    }

    /**
     * Check for collision with both terrain and obstacles..
     * 
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForCollision(LatLng checkpointPosition, float checkpointAltitude) {
        boolean obstacleCollision =
                checkForObstacleCollision(checkpointPosition, checkpointAltitude);
        boolean terrainCollision = checkForTerrainCollision(checkpointPosition, checkpointAltitude);
        return (obstacleCollision || terrainCollision);
    }

    /**
     * Creates line between currentPosition and checkpointPosition and checks whether this line
     * intersects with/lies within obstacle.
     * 
     * @param currentPosition the current position
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForObstacleCollision(LatLng currentPosition,
            LatLng checkpointPosition, float checkpointAltitude) {
        // x .. longitude, y .. latitude
        LineString currentToCheckpointLine =
                sGeometryFactory.createLineString(new Coordinate[] {
                        new Coordinate(currentPosition.longitude, currentPosition.latitude),
                        new Coordinate(checkpointPosition.longitude, checkpointPosition.latitude)});
        return lineCollidesWithObstacle(checkpointAltitude, currentToCheckpointLine);
    }

    /**
     * Creates line between currentPosition and checkpointPosition and checks whether this line
     * intersects with/lies within obstacle. Uses Point instead of LatLng.
     * 
     * @param currentPosition the current position
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForObstacleCollision(Point currentPosition,
            Point checkpointPosition, float checkpointAltitude) {
        LineString currentToCheckpointLine =
                sGeometryFactory.createLineString(new Coordinate[] {
                        new Coordinate(currentPosition.getX(), currentPosition.getY()),
                        new Coordinate(checkpointPosition.getX(), checkpointPosition.getY())});
        return lineCollidesWithObstacle(checkpointAltitude, currentToCheckpointLine);
    }

    /**
     * Checks whether the line intersects with/lies within any of all known obstacles.
     * 
     * @param checkpointAltitude the checkpoint altitude
     * @param currentToCheckpointLine the current to checkpoint line
     * @return true, if successful
     */
    private static boolean lineCollidesWithObstacle(float checkpointAltitude,
            LineString currentToCheckpointLine) {
        List<Obstacle> obstacles = sCollisionAvoidance.getObstacles();
        if (obstacles == null)
            return false;
        for (Obstacle obstacle : obstacles) {
            if ((currentToCheckpointLine.intersects(obstacle.getPolygon()) || currentToCheckpointLine
                    .within(obstacle.getPolygon()))
                    && checkpointAltitude <= obstacle.getElevation()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the checkpointPosition lies within any of the obstacles.
     * 
     * @param checkpointPosition the checkpoint position
     * @param checkpointAltitude the checkpoint altitude
     * @return true in danger of collision
     */
    public static boolean checkForObstacleCollision(LatLng checkpointPosition,
            float checkpointAltitude) {
        // x .. longitude, y .. latitude
        Point checkpoint =
                sGeometryFactory.createPoint(new Coordinate(checkpointPosition.longitude,
                        checkpointPosition.latitude));
        List<Obstacle> obstacles = sCollisionAvoidance.getObstacles();
        if (obstacles == null)
            return false;
        for (Obstacle obstacle : obstacles) {
            if (checkpoint.within(obstacle.getPolygon())
                    && checkpointAltitude <= obstacle.getElevation()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads terrain elevation from GeoTIFF on the passed position and compares it with the passed
     * altitude.
     * 
     * @param position geographic position
     * @param altitude AMSL altitude
     * @return true, if the position+altitude lies below terrain
     */
    public static boolean checkForTerrainCollision(LatLng position, float altitude) {
        TiffParser elevationModel = sCollisionAvoidance.getElevationModel();
        if (elevationModel == null)
            return false;
        int terrainElevation = elevationModel.getElevation(position);
        if (altitude <= terrainElevation && terrainElevation > -1) {
            return true;
        }
        return false;
    }

    @Override
    public void onVehicleEvent(VehicleEvent event) {
        switch (event) {
            case POSITION:
                // Checks on new vehicle location update whether it doesn't head to
                // obstacle/terrain.
                // TODO: On vehicle location update the rest of the vehicle values may be outdated
                // (altitude/yaw/speed). Consider the impact on the collision prediction.
                if (mCasEnabled)
                    checkForCollision();
                break;
            default:
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesFragment.CAS_DISTANCE_PREF_KEY:
                mDistanceToCheckpointInS =
                        sharedPreferences.getInt(PreferencesFragment.CAS_DISTANCE_PREF_KEY, 1);
                break;
            case PreferencesFragment.CAS_ENABLED_PREF_KEY:
                mCasEnabled =
                        sharedPreferences.getBoolean(PreferencesFragment.CAS_ENABLED_PREF_KEY,
                                false);
                if (mCasEnabled) {
                    loadObstacles();
                } else {
                    destroyObstacles();
                }
                break;
            default:
                break;
        }
    }

    public TiffParser getElevationModel() {
        return mElevationModel;
    }

    public List<Obstacle> getObstacles() {
        return mObstacles;
    }

    public CollisionEvents getEvents() {
        return sCollisionEvents;
    }

    public LatLng getCheckpointPosition() {
        return mCheckpointPosition;
    }

    public boolean isCasEnabled() {
        return mCasEnabled;
    }

    public boolean inDangerOfCollision() {
        return mDangerOfCollision;
    }

    @Override
    public void onConnectionEvent(ConnectionEvent event) {
        switch (event) {
            case SERVICE_UNBOUND:
                // Clear any state related to connected vehicle
                mDangerOfCollision = false;
                sCollisionEvents.onCollisionEvent(CollisionEvent.CLEAR_OF_COLLISION);
                break;
            default:
                break;
        }
    }
}
