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

/**
 * Defines app-wide constants.
 */
public final class SkyControlConst {

    /** Tag of shared preferences repository that stores persistent data. */
    public static final String APP_PREFERENCES = "com.bocekm.skycontrol.APP_PREFERENCES";

    /** Tags for Android LogCat. */
    public static final String DEBUG_TAG = "SkyControl Debug";
    public static final String ERROR_TAG = "SkyControl Error";

    /** An empty string for string's initialization/comparison. */
    public static final String EMPTY_STRING = new String();

    /** Android device heartbeat message period in seconds. */
    public static final int HEARTBEAT_PERIOD_IN_SECONDS = 1;

    /** Name of the XML containing definition of obstacles. */
    public static final String OBSTACLES_XML = "obstacles.xml";

    /** Size of TIFF data types. */
    public static final int BYTES_IN_TIFF_SHORT = 2;
    public static final int BYTES_IN_TIFF_LONG = 4;
    public static final int BYTES_IN_TIFF_DOUBLE = 8;

    /** Name of the ASTER GDEM file in assets and also on external storage where it gets copied. */
    public static final String ELEVATION_FILE_ASSET = "digital_elevation_model.tif";
    /**
     * Name of an XML specifying the obstacles in assets and also on external storage where it gets
     * copied.
     */
    public static final String OBSTACLES_FILE_ASSET = "obstacles.xml";
}
