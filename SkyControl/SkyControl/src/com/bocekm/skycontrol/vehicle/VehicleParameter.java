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
package com.bocekm.skycontrol.vehicle;

import com.MAVLink.Messages.enums.MAV_PARAM_TYPE;

/**
 * Stores specification of one autopilot parameter.
 */
public class VehicleParameter {

    /** Name of the parameter working as its identification, max 16 characters long. */
    private String mName;

    /** Value of the parameter. */
    private float mValue;

    /** Data type of the value, one of {@link MAV_PARAM_TYPE}. */
    private int mType;

    public String getName() {
        return mName;
    }

    /**
     * Sets the name of the parameter.
     * 
     * @param name name of the parameter working as its identification, max 16 characters long
     * @return the parameter object itself to have possibility to chain methods
     */
    public VehicleParameter setName(String name) {
        mName = name;
        return this;
    }

    public float getValue() {
        return mValue;
    }

    /**
     * Sets value of the parameter.
     * 
     * @param value value of the parameter
     * @return the parameter object itself to have possibility to chain methods
     */
    public VehicleParameter setValue(float value) {
        mValue = value;
        return this;
    }

    public int getType() {
        return mType;
    }

    /**
     * Sets data type of the value.
     * 
     * @param type data type of the value, one of {@link MAV_PARAM_TYPE}
     * @return the parameter object itself to have possibility to chain methods
     */
    public VehicleParameter setType(int type) {
        mType = type;
        return this;
    }

}
