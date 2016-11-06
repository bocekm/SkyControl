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
package com.bocekm.skycontrol.rrt;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import android.util.Log;

import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.cas.CollisionAvoidance;
import com.bocekm.skycontrol.cas.TiffParser;
import com.google.android.gms.maps.model.LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Searches space around the vehicle for collision free path using modified RRT algorithm.
 */
public class RrtSearch {

    private float mVehicleAltitude;
    private float mVehicleHeading;

    private Random mRandom;
    private RrtTree mSearchTree;
    private boolean mRrtTargetReached = false;
    private GeometryFactory mGeometryFactory;
    private Point mRrtTarget;

    /**
     * Probability that target will be chosen instead of creating new random point in space. In
     * percent out of 100.
     */
    private static final int PROBABILITY_OF_TARGET = 30;

    /**
     * Important element. It determines how big is going to be the search space. It's multiplier of
     * the distance between initial and target position.
     */
    private static final float FRONT_DISTANCE_SCALE_FACTOR = 3;

    /**
     * Angle going to the left and right from the vehicle heading. In these directions lie the
     * search space corners.
     */
    private static final float VEHICLE_FRONT_ANGLE = 45;

    /**
     * The vehicle front angle is divided by this constant. In the end it means that the rear search
     * area is smaller then the front.
     */
    private static final float VEHICLE_REAR_ANGLE_DIVISOR = 2.0f;

    /**
     * Maximum line length connecting the randomly generated position with it's closest tree node.
     * When large it may decrease the resulting number of waypoints but it can also cause the
     * waypoints divert substantially from the target position. It's subject for tweaking and for
     * now it is set to the distance between initial and target position.
     */
    private double mBranchLengthInM;

    private double mSpaceDimensionX;
    private double mSpaceDimensionY;
    private LatLng mSpaceOrigin;

    /**
     * Instantiates a new RRT search.
     * 
     * @param initialPosition the initial position
     * @param vehicleHeading the vehicle heading
     * @param vehicleAltitude the vehicle altitude
     * @param targetPosition the target position
     */
    public RrtSearch(LatLng initialPosition, float vehicleHeading, float vehicleAltitude,
            LatLng targetPosition) {
        mRandom = new Random(System.currentTimeMillis());
        mVehicleAltitude = vehicleAltitude;
        mVehicleHeading = vehicleHeading;

        double vehicleToTargetDistance =
                mBranchLengthInM = SkyControlUtils.getDistance(initialPosition, targetPosition);
        computeRrtSpace(initialPosition, vehicleToTargetDistance);

        // Specifying the WGS-84 datum (EPSG:4326 Coordinate Reference System)
        mGeometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        // x .. longitude, y .. latitude
        Point initPoint =
                mGeometryFactory.createPoint(new Coordinate(initialPosition.longitude,
                        initialPosition.latitude));
        RrtNode initNode = new RrtNode(initPoint, null);

        mRrtTarget =
                mGeometryFactory.createPoint(new Coordinate(targetPosition.longitude,
                        targetPosition.latitude));

        mSearchTree = new RrtTree(initNode);
    }

    /**
     * Get collision free path using modified RRT algorithm.
     * 
     * @param iterations maximum number of RRT algorithm steps
     * @return the list
     */
    public List<LatLng> runSearch(int iterations) {
        if (CollisionAvoidance.checkForCollision(new LatLng(mRrtTarget.getY(), mRrtTarget.getX()),
                mVehicleAltitude))
            // Return null if the target position lies within obstacle/terrain so we wouldn't
            // certainly find a collision-free path
            return null;

        RrtNode next = null;
        long initTime = System.currentTimeMillis();
        for (int count = 0; count < iterations && !mRrtTargetReached; count++) {
            next = step();
            if (next != null)
                mSearchTree.add(next);
        }
        long elapsed = System.currentTimeMillis() - initTime;
        Log.d(SkyControlConst.DEBUG_TAG, "RRT search time: " + elapsed);

        if (!mRrtTargetReached) {
            Log.e(SkyControlConst.ERROR_TAG, "RRT path not found");
            SkyControlUtils.log("Collision-free path found\n", false);
            return null;
        }
        SkyControlUtils.log("Collision-free path found\n", true);

        // Get the tree path by going backwards from the target position to the root of the tree
        List<LatLng> waypoints = new ArrayList<LatLng>();
        RrtNode waypoint = mSearchTree.closestTo(mRrtTarget);
        while (!waypoint.isRoot()) {
            waypoints.add(0, new LatLng(waypoint.getPoint().getY(), waypoint.getPoint().getX()));
            waypoint = waypoint.getParent();
        }
        waypoints.add(0, new LatLng(waypoint.getPoint().getY(), waypoint.getPoint().getX()));

        // Remove redundant waypoints
        optimizeWaypoints(waypoints);
        
        // Discard the first waypoint as it is the initial vehicle position
        waypoints.remove(0);
        // Discard the last waypoint, which is the target actually
        if (waypoints.size() > 0)
            waypoints.remove(waypoints.size() - 1);
        
        Log.d(SkyControlConst.DEBUG_TAG, "RRT optimized waypoints: " + waypoints.size());

        logWaypoints(waypoints);
        return waypoints;
    }

    /**
     * Log found waypoints to log file.
     * 
     * @param waypoints the waypoints
     */
    private void logWaypoints(List<LatLng> waypoints) {
        int i = 0;
        StringBuilder sBldr = new StringBuilder("RRT waypoints:\n");
        for (LatLng wp : waypoints)
            sBldr.append("WP " + i++ + " (lat " + wp.latitude + ", lng " + wp.longitude + ", alt "
                    + mVehicleAltitude + ")\n");
        SkyControlUtils.log(sBldr.toString(), false);
    }

    /**
     * Remove such waypoints that do not provide collision avoidance function.
     * 
     * @param waypoints the waypoints
     * @return optimized list of waypoints
     */
    private List<LatLng> optimizeWaypoints(List<LatLng> waypoints) {
        ListIterator<LatLng> it = waypoints.listIterator();
        if (it.hasNext())
            // Do not use the first waypoint (init vehicle position) for optimization as the vehicle
            // may be a bit further then this waypoint says before the avoidance path is found and
            // loaded to the vehicle
            it.next();
        while (it.hasNext()) {
            // Load the first waypoint
            LatLng waypoint1 = (LatLng) it.next();
            if (it.hasNext())
                // Skip the second waypoint
                it.next();
            else
                return waypoints;
            LatLng waypoint3;
            if (it.hasNext())
                // Load the third waypoint
                waypoint3 = (LatLng) it.next();
            else
                return waypoints;
            // Check if line path between first and third waypoint collides with obstacle/terrain
            boolean collides =
                    CollisionAvoidance.checkForCollision(waypoint1, waypoint3, mVehicleAltitude);
            // Return to the second waypoint
            it.previous();
            it.previous();
            if (!collides) {
                // If there's no collision remove the middle (second) waypoint from the list
                it.remove();
                // Return to the first waypoint to have possibility to check collision between the
                // first and fourth waypoint in next loop iteration
                it.previous();
            }
        }
        return waypoints;
    }

    /**
     * Computes geographical boundaries of the space to be searched by RRT. These boundaries give us
     * space dimensions used for calculation of the random point position.
     * 
     * @param vehiclePosition the initial vehicle position
     * @param vehicleToTargetDistance distance between initial and target position in m
     * @return true, if the computed space lies within the elevation model
     */
    private boolean computeRrtSpace(LatLng vehiclePosition, double vehicleToTargetDistance) {
        double rearDistanceScaleFactor =
                (FRONT_DISTANCE_SCALE_FACTOR * Math.cos(SkyControlUtils
                        .degToRad(VEHICLE_FRONT_ANGLE)))
                        / Math.cos(SkyControlUtils.degToRad(VEHICLE_FRONT_ANGLE)
                                / VEHICLE_REAR_ANGLE_DIVISOR);

        TiffParser elevationModel = CollisionAvoidance.get().getElevationModel();

        // Compute position of the space front left corner. It will be used as an origin for
        // position calculation of the randomly generated point.
        float frontLeftHeading = mVehicleHeading - VEHICLE_FRONT_ANGLE;
        if (frontLeftHeading < 0.0f)
            frontLeftHeading += 360.0f;
        LatLng frontLeftPosition =
                mSpaceOrigin =
                        SkyControlUtils.getDestination(vehiclePosition, frontLeftHeading,
                                FRONT_DISTANCE_SCALE_FACTOR * vehicleToTargetDistance);
        if (!elevationModel.isPositionWithin(frontLeftPosition))
            return false;

        // Compute position of the space front right corner
        float frontRightHeading = mVehicleHeading + VEHICLE_FRONT_ANGLE;
        if (frontRightHeading > 360.0f)
            frontRightHeading -= 360.0f;
        LatLng frontRightPosition =
                SkyControlUtils.getDestination(vehiclePosition, frontRightHeading,
                        FRONT_DISTANCE_SCALE_FACTOR * vehicleToTargetDistance);
        if (!elevationModel.isPositionWithin(frontRightPosition))
            return false;

        // Compute position of the space rear left corner
        float rearLeftHeading =
                mVehicleHeading - 90.0f - VEHICLE_FRONT_ANGLE / VEHICLE_REAR_ANGLE_DIVISOR;
        if (rearLeftHeading < 0.0f)
            rearLeftHeading += 360.0f;
        LatLng rearLeftPosition =
                SkyControlUtils.getDestination(vehiclePosition, rearLeftHeading,
                        rearDistanceScaleFactor * vehicleToTargetDistance);
        if (!elevationModel.isPositionWithin(rearLeftPosition))
            return false;

        // Compute position of the space rear right corner
        float rearRightHeading =
                mVehicleHeading + 90.0f + VEHICLE_FRONT_ANGLE / VEHICLE_REAR_ANGLE_DIVISOR;
        if (rearRightHeading > 360.0f)
            rearRightHeading -= 360.0f;
        LatLng rearRightPosition =
                SkyControlUtils.getDestination(vehiclePosition, rearRightHeading,
                        rearDistanceScaleFactor * vehicleToTargetDistance);
        if (!elevationModel.isPositionWithin(rearRightPosition))
            return false;

        // Get the distance between the front and side corners of the space. These distances will be
        // used to calculate position of the randomly generated point in the RRT space. Due to
        // curvature of the Earth the distance between left and right side (and between front and
        // rear) corners may not be equal, but we don't count with that for the sake of simplicity.
        mSpaceDimensionX = SkyControlUtils.getDistance(frontLeftPosition, frontRightPosition);
        mSpaceDimensionY = SkyControlUtils.getDistance(frontLeftPosition, rearLeftPosition);

        return true;
    }

    private Point getRandomPoint() {
        double randDistFromOriginInX = mRandom.nextDouble() * mSpaceDimensionX;
        double randDistFromOriginInY = mRandom.nextDouble() * mSpaceDimensionY;
        double distFromOrigin =
                Math.sqrt(Math.pow(randDistFromOriginInX, 2.0)
                        + Math.pow(randDistFromOriginInY, 2.0));
        // Angle from origin: 0 - 90
        double angleFromOrigin =
                SkyControlUtils.radToDeg(Math.acos(randDistFromOriginInX / distFromOrigin));
        double bearingFromOrigin = mVehicleHeading + 90.0 + angleFromOrigin;
        if (bearingFromOrigin > 360.0)
            bearingFromOrigin -= 360.0;
        LatLng randPointPosition =
                SkyControlUtils.getDestination(mSpaceOrigin, bearingFromOrigin, distFromOrigin);

        return mGeometryFactory.createPoint(new Coordinate(randPointPosition.longitude,
                randPointPosition.latitude));
    }

    private RrtNode step() {
        Point target;
        RrtNode nodeClosestToTarget;
        boolean toRrtTarget = false;
        boolean reachingRrtTarget = false;

        // Determine the target point in space
        int p = mRandom.nextInt(100);
        if (p < PROBABILITY_OF_TARGET) {
            // PROBABILITY_OF_TARGET percent chance to choose the actual target
            target = mRrtTarget;
            toRrtTarget = true; //
        } else
            // (100 - PROBABILITY_OF_TARGET) percent chance to choose random point in space to have
            // possibility to grow the tree
            target = getRandomPoint();
        // find tree node closest to the target (random or real)
        nodeClosestToTarget = mSearchTree.closestTo(target);
        Point nodeClosestToTargetPoint = nodeClosestToTarget.getPoint();
        // x .. longitude, y .. latitude
        double dist =
                SkyControlUtils.getDistance(nodeClosestToTargetPoint.getY(),
                        nodeClosestToTargetPoint.getX(), target.getY(), target.getX());
        if (dist < mBranchLengthInM && toRrtTarget)
            // We can reach the target from the closest node and it is the RRT search target so we
            // might be done
            reachingRrtTarget = true;
        else
            // Update the target position to be branch length constant meters far from the closest
            // node.
            target = updateTargetDistance(nodeClosestToTargetPoint, target, mBranchLengthInM);
        if (CollisionAvoidance
                .checkForCollision(nodeClosestToTargetPoint, target, mVehicleAltitude))
            // Collision detected
            return null;
        else {
            RrtNode ret = createNode(target, nodeClosestToTarget);
            if (reachingRrtTarget)
                // Didn't collide, will reach the target
                mRrtTargetReached = true;
            return ret;
        }
    }

    /**
     * Creates new point in specific distance from the initial point in direction towards another
     * point.
     * 
     * @param origin the origin
     * @param towards the towards
     * @param distance distance of the new point from the origin
     * @return the new point
     */
    private Point updateTargetDistance(Point origin, Point towards, double distance) {
        // x .. longitude, y .. latitude
        double azimuth =
                SkyControlUtils.getAzimuth(origin.getY(), origin.getX(), towards.getY(),
                        towards.getX());
        LatLng newLatLng =
                SkyControlUtils.getDestination(new LatLng(origin.getY(), origin.getX()), azimuth,
                        distance);
        Point newPoint =
                mGeometryFactory
                        .createPoint(new Coordinate(newLatLng.longitude, newLatLng.latitude));
        return newPoint;
    }

    /**
     * Creates new K-D tree node.
     * 
     * @param point the point
     * @param parent the parent
     * @return the new node
     */
    private RrtNode createNode(Point point, RrtNode parent) {
        return new RrtNode(point, parent);
    }
}
