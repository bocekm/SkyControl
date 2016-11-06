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
 * Reads GeoTIFF KeyEntry values from byte buffer. Expects that KeyID has been already read.
 */
public class GeoTiffKeyEntry {
    int mTiffTagLocation;
    int mCount;
    int mValueOrOffset;

    public GeoTiffKeyEntry(ByteBuffer buffer) {
        readKeyEntry(buffer);
    }

    private void readKeyEntry(ByteBuffer buffer) {
        // The KeyID, already read from buffer, is followed by 2 bytes describing TIFFTagLocation.
        // Indicates which TIFF tag contains the value(s) of the Key.
        mTiffTagLocation = buffer.getShort() & 0xFFFF;
        // Log.d(SkyControlConst.DEBUG_TAG, "TIFFTagLocation: " + mTiffTagLocation + "\n");

        // Follows number of values of TIFF-Types/shorts that are stored in this key (2 bytes).
        mCount = buffer.getShort() & 0xFFFF;
        // Log.d(SkyControlConst.DEBUG_TAG, "Count: " + mCount + "\n");

        // If TIFFTagLocation is 0, then ValueOrOffset contains value of key and is short.
        // If TIFFTagLocation is equal to GeoKeyDirectoryTag (0x87AF) then array of shorts is
        // appended to this key, starting on the position of ValueOrOffset.
        // If TIFFTagLocation is TIFF-Type, then ValueOrOffset is an offset to the array of
        // TIFF-Types.
        mValueOrOffset = buffer.getShort() & 0xFFFF;
        // Log.d(SkyControlConst.DEBUG_TAG, "Value or offset: " + mValueOrOffset + "\n");
    }

    public int getTiffTagLocation() {
        return mTiffTagLocation;
    }

    public int getCount() {
        return mCount;
    }

    public int getValueOrOffset() {
        return mValueOrOffset;
    }
}
