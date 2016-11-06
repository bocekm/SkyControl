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

import java.nio.ByteBuffer;

/**
 * Loads values from byte buffer which contains TIFF field data.
 */
public class TiffField {

    private int mType;
    private int mCount;
    private int mValueOrOffset;

    public static final int TIFF_TYPE_LONG = 4;
    public static final int TIFF_TYPE_DOUBLE = 12;

    public TiffField(ByteBuffer buffer) {
        readField(buffer);
    }

    private void readField(ByteBuffer buffer) {
        // Tag has been already read from the buffer. It is followed by 2 bytes describing
        // representation of the value (TIFF-Type)
        mType = buffer.getShort();
        // Log.d(SkyControlConst.DEBUG_TAG, "Type: " + mType + "\n");

        // Follows number of values of 'type' that are stored in this field (4 bytes).
        mCount = buffer.getInt();
        // Log.d(SkyControlConst.DEBUG_TAG, "Count: " + mCount + "\n");

        // Last 4 bytes store the value itself (if count*sizeof(type) <= 4B) or points to a value
        // storage
        mValueOrOffset = buffer.getInt();
        // Log.d(SkyControlConst.DEBUG_TAG, "Value or offset: " + mValueOrOffset + "\n");
    }

    public int getType() {
        return mType;
    }

    public int getCount() {
        return mCount;
    }

    public int getValueOrOffset() {
        return mValueOrOffset;
    }
}
