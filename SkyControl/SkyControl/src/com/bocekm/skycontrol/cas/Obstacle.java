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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Persist;

import com.bocekm.skycontrol.SkyControlUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * {@link Obstacles} class represents a child element in the XML specifying obstacles. It is used by
 * {@link Persist}.
 */
@ElementList
public class Obstacle {

    @Element(name = "coords")
    private String mCoords;

    @Element(name = "elevation")
    private int mElevation;

    /** Minimum number of the coordinates to form a polygon. Required by JTS library. */
    private static final int MIN_POLYGON_COORDS = 4;

    private Polygon mPolygon = null;

    public Polygon getPolygon() {
        return mPolygon;
    }

    public int getElevation() {
        return mElevation;
    }

    /**
     * Creates a polygon connecting the coords parsed from the "coords" XML element value. TODO: Can
     * be easily extended to allow polygon holes and circles, see
     * https://developers.google.com/maps/documentation/android/shapes
     * 
     * @return true, if successful
     */
    public boolean createPolygonFromCoords() {
        // Specifying the WGS-84 datum (EPSG:4326 Coordinate Reference System)
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        // Format of the coordinates: "[double,double\s+]+"
        String matchDouble = "(\\d+\\.\\d+)";
        Pattern pattern = Pattern.compile(matchDouble + "\\s*,\\s*" + matchDouble);
        Matcher matcher = pattern.matcher(mCoords);
        List<Coordinate> coordsList = new ArrayList<Coordinate>();
        // Load the coords from string to the list
        while (matcher.find()) {
            // x .. longitude, y .. latitude
            coordsList.add(new Coordinate(Double.valueOf(matcher.group(2)), Double.valueOf(matcher
                    .group(1))));
        }

        // The coordinates must form a ring. If the last coordinate doesn't match the first one,
        // append the first one to the end of the list.
        int lastCoordIndex = coordsList.size() - 1;
        if (lastCoordIndex >= 0 && !coordsList.get(0).equals2D(coordsList.get(lastCoordIndex)))
            coordsList.add(coordsList.get(0));
        if (coordsList.size() >= MIN_POLYGON_COORDS) {
            Coordinate[] coordsArray = coordsList.toArray(new Coordinate[coordsList.size()]);
            mPolygon = geometryFactory.createPolygon(coordsArray);
            return true;
        } else {
            SkyControlUtils.log("One of the obstacles has less than required " + MIN_POLYGON_COORDS
                    + " coordinates\n", true);
            return false;
        }
    }
}
