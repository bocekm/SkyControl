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

import com.bocekm.skycontrol.SkyControlUtils;
import com.savarese.spatial.Distance;
import com.savarese.spatial.Point;

/**
 * Defines a distance function for K-D tree. It's equivalent to
 * {@link SkyControlUtils#getDistance(double, double, double, double)}.
 * 
 * @param <Coord> the generic type
 * @param <P> the generic type
 */
public class OrthodromicDistance<Coord extends Number & Comparable<? super Coord>, P extends Point<Coord>> implements
        Distance<Coord, P> {
    
    // 0 .. x .. lng
    // 1 .. y .. lat
    private static final int LATITUDE = 1;
    private static final int LONGITUDE = 0;

    /**
     * Returns the orthodromic distance between two two-dimensional points.
     * 
     * @param from The first end point.
     * @param to The second end point.
     * @return The distance between from and to.
     */
    @Override
    public double distance(P from, P to) {
        return SkyControlUtils.getDistance(from.getCoord(LATITUDE).doubleValue(),
                from.getCoord(LONGITUDE).doubleValue(), to.getCoord(LATITUDE).doubleValue(), to
                        .getCoord(LONGITUDE).doubleValue());
    }

    /**
     * Returns the square of the orthodromic distance between two two-dimensional points.
     * 
     * @param from The first end point.
     * @param to The second end point.
     * @return The square of the orthodromic distance between from and to.
     */
    @Override
    public double distance2(P from, P to) {
        return Math.pow(distance(from, to), 2);
    }
}
