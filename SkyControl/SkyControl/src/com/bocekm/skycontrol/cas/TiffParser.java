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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.file.FileUtil;
import com.google.android.gms.maps.model.LatLng;

/**
 * Loads digital elevation model data from ASTER GDEM GeoTIFF.
 */
public class TiffParser {
    private ByteBuffer mBuffer = null;
    private RandomAccessFile mTiffFile = null;
    private ByteOrder mEndianness = ByteOrder.LITTLE_ENDIAN;
    private int[] mStripOffsets = null;
    private int mImageWidth = -1;
    private int mImageHeight = -1;
    private LatLng mMinPos = null;
    private LatLng mMaxPos = null;
    private double mScaleLat = -1;
    private double mScaleLng = -1;

    /** Position of the latitude in list of tiepoint values. */
    private static final int LAT_IN_TIEPOINT = 4;
    /** Position of the longitude in list of tiepoint values. */
    private static final int LNG_IN_TIEPOINT = 3;

    /**
     * Parses GeoTIFF and keeps the file opened for consecutive reading until the
     * {@link TiffParser#closeTiff()} is called. Parsing means loading metadata from header/IFD and
     * array of TIFF strip offsets. These offsets are needed for every value look-up, so it's
     * important from the performance standpoint to read them just once. Currently this parser works
     * possibly just with ASTER GDEM tiles downloaded from http://reverb.echo.nasa.gov/.
     * 
     * @param tiffFileName GeoTIFF filename
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void parseGeoTiff(String tiffFileName) throws IOException {

        String copiedFilePath = FileUtil.copyAssetToInternal(tiffFileName);
        if (copiedFilePath == null) {
            Log.e(SkyControlConst.ERROR_TAG, "Couldn't copy asset: " + tiffFileName + "\n");
            return;
        }

        try {
            mTiffFile = new RandomAccessFile(copiedFilePath, "r");
        } catch (FileNotFoundException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not open file: " + copiedFilePath + "\n");
            e.printStackTrace();
        }
        // Allocate 12 bytes in a buffer
        mBuffer = ByteBuffer.allocate(12);
        // Load 8 bytes from file to buffer
        FileUtil.readBytesFromFile(mTiffFile, mBuffer, 8, FileUtil.NO_POSITION_CHANGE);

        // TIFF header
        // First 2 bytes (Byte Order Field): 'MM' (big endian) or 'II' (little endian)
        byte endian = mBuffer.get();
        if (endian != mBuffer.get() || (endian != 'I' && endian != 'M')) {
            throw new IOException("Not a tiff file.");
        }
        // Set the endian to the buffer settings so read data are correctly interpreted
        mEndianness = endian == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        mBuffer.order(mEndianness);
        // Another 2 bytes (Version field): 42 indicates TIFF
        if (mBuffer.getShort() != 42) {
            throw new IOException("Not a tiff file.");
        }

        // Follows 4 bytes (First IFD Offset pointer). IFD = Image File Directory. Offset is meant
        // in bytes from start of the file. Usually there is just one IFD per file.
        int firstIfdPos = mBuffer.getInt();

        TiffField geoKeyDirectory = processFileDirectory(firstIfdPos);
        if (geoKeyDirectory != null)
            loadGeoKeys(geoKeyDirectory.getValueOrOffset());
    }

    private TiffField processFileDirectory(int firstIfdPos) throws IOException {
        // The first 2 bytes of the Image File Directory indicate number of fields (entries) stored
        // in this particular IFD.
        FileUtil.readBytesFromFile(mTiffFile, mBuffer, 2, firstIfdPos);
        TiffField geoKeyDirectory = null;
        ArrayList<Double> scale = null;
        ArrayList<Double> origin = null;

        // Number of IFD fields
        int fieldCount = mBuffer.getShort() & 0xFFFF;
        // Log.d(SkyControlConst.DEBUG_TAG, "Field count: " + fieldCount + "\n");

        // Go through all IFD fields
        for (; fieldCount > 0; --fieldCount) {
            // Each field is 12-bytes long. Meaning of each field can be found here
            // http://www.awaresystems.be/imaging/tiff/tifftags/baseline.html
            FileUtil.readBytesFromFile(mTiffFile, mBuffer, 12, FileUtil.NO_POSITION_CHANGE);
            // First 2 bytes of the field contain tag describing the content of the field.
            // Tags are defined as unsigned short but Java does not know unsigned short so we need
            // to use int to get the unsigned value.
            int tag = mBuffer.getShort() & 0xFFFF;
            // Log.d(SkyControlConst.DEBUG_TAG, "Tag: " + tag + " - 0x" + Integer.toHexString(tag)
            // + "\n");
            switch (tag) {
                case 256:
                    // ImageWidth
                    mImageWidth = (new TiffField(mBuffer)).getValueOrOffset();
                    Log.d(SkyControlConst.DEBUG_TAG, "ImageWidth: " + mImageWidth + "\n");
                    break;
                case 257:
                    // ImageHeight
                    mImageHeight = (new TiffField(mBuffer)).getValueOrOffset();
                    Log.d(SkyControlConst.DEBUG_TAG, "ImageHeight: " + mImageHeight + "\n");
                    break;
                case 258:
                    // BitsPerSample
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "BitsPerSample: " + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 259:
                    // Compression
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "Compression: " + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 262:
                    // PhotometricInterpretation
                    Log.d(SkyControlConst.DEBUG_TAG, "PhotometricInterpretation: "
                            + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 266:
                    // FillOrder
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "FillOrder: " + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 273:
                    // StripOffsets
                    TiffField stripOffsets = new TiffField(mBuffer);
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "StripOffsets of type " + stripOffsets.getType() + ": "
                                    + stripOffsets.getCount() + "\n");
                    mStripOffsets = loadInts(stripOffsets);
                    break;
                case 274:
                    // Orientation
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "Orientation: " + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 277:
                    // SamplesPerPixel
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "SamplesPerPixel: " + (new TiffField(mBuffer)).getValueOrOffset()
                                    + "\n");
                    break;
                case 278:
                    // RowsPerStrip
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "RowsPerStrip: " + (new TiffField(mBuffer)).getValueOrOffset() + "\n");
                    break;
                case 279:
                    // StripByteCounts
                    TiffField stripByteCounts = new TiffField(mBuffer);
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "StripByteCounts of type " + stripByteCounts.getType() + ": "
                                    + stripByteCounts.getCount() + "\n");
                    // Don't need to load if we know that:
                    // a)RowsPerStrip=1
                    // b)BitsPerSample=16
                    // c)SamplesPerPixel=1
                    // Then StripByteCounts = ImageHeight*2
                    // loadInts(stripByteCounts);
                    break;
                case 34735:
                    // GeoKeyDirectoryTag
                    // Log.d(SkyControlConst.DEBUG_TAG, "GeoKeyDirectoryTag found\n");
                    geoKeyDirectory = new TiffField(mBuffer);
                    break;
                case 33550:
                    // ModelPixelScaleTag
                    // Log.d(SkyControlConst.DEBUG_TAG, "ModelPixelScaleTag found\n");
                    TiffField modelPixelScale = new TiffField(mBuffer);
                    scale = loadDoubles(modelPixelScale);
                    Log.d(SkyControlConst.DEBUG_TAG,
                            "Pixel scale: (" + scale.get(0) + ", " + scale.get(1) + ")\n");
                    break;
                case 33922:
                    // ModelTiepointTag
                    // Log.d(SkyControlConst.DEBUG_TAG, "ModelTiepointTag found\n");
                    TiffField modelTiepointTag = new TiffField(mBuffer);
                    origin = loadDoubles(modelTiepointTag);
                    Log.d(SkyControlConst.DEBUG_TAG, "Origin: (" + origin.get(LAT_IN_TIEPOINT)
                            + ", " + origin.get(LNG_IN_TIEPOINT) + ")\n");
                    break;
                default:
                    break;
            }
        }
        // The terminating field follows after all the IFD fields. It is 4-bytes long -
        // either 4-bytes of zeros or offset to another IFD. Ignoring the possibility of the next
        // IFD for now.

        parseScaleAndOrigin(scale, origin);

        if (geoKeyDirectory == null) {
            Log.e(SkyControlConst.ERROR_TAG, "GeoKeyDirectoryTag not found in GeoTIFF\n");
            return null;
        }
        return geoKeyDirectory;
    }

    /**
     * Shall be closed on destroy of the instantiator.
     */
    public void closeTiff() {
        try {
            mTiffFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseScaleAndOrigin(ArrayList<Double> scale, ArrayList<Double> origin) {
        if (scale == null || origin == null) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not extract origin and scale from GeoTIFF\n");
            return;
        }
        // Scale is represented in (X,Y) of the image and in X we scale Longitude, in Y Latitude
        mScaleLng = scale.get(0);
        mScaleLat = scale.get(1);
        // Subtracting because the origin is in the upper left corner of the image and it is
        // expected that the image lies within northern hemisphere
        double minLat = origin.get(LAT_IN_TIEPOINT) - (mImageHeight * mScaleLat);
        double maxLat = origin.get(LAT_IN_TIEPOINT);
        Log.d(SkyControlConst.DEBUG_TAG, "Lat: " + minLat + ", " + maxLat + "\n");

        double minLng = origin.get(LNG_IN_TIEPOINT);
        double maxLng = origin.get(LNG_IN_TIEPOINT) + (mImageWidth * mScaleLng);
        Log.d(SkyControlConst.DEBUG_TAG, "Lng: " + minLng + ", " + maxLng + "\n");

        mMinPos = new LatLng(minLat, minLng);
        mMaxPos = new LatLng(maxLat, maxLng);
    }

    /**
     * Gets elevation of the specific global position from the terrain elevation model.
     *
     * @param position the position
     * @return the elevation when position is within elevation model or -1 otherwise
     */
    public int getElevation(LatLng position) {
        if (!isPositionWithin(position))
            return -1;
        // Choose in which strip the elevation lies (image height .. latitude)
        double latDiffScaled = (position.latitude - mMinPos.latitude) / mScaleLat;
        // Origin is in the top left corner, latitude number decreases going south .. lowest
        // latitude number lies at the bottom of the image.
        // The elevation on position (n,m) is not elevation of a point but it is an elevation of
        // square (n-scale/2,m-scale/2),(n+scale/2,m+scale/2), because raster type is
        // RasterPixelIsArea. And because latitude is reversed (higher latitude is at the beginning
        // of the image) we need to use ceiling method. That also means that elevation on image
        // pixel (pixelHeight, pixelWidth) covers area defined by (latitude origin -
        // pixelHeight*scale - scale/2, longitude origin + pixelWidth*scale - scale/2),(latitude
        // origin - pixelHeight*scale + scale/2, longitude origin + pixelWidth*scale + scale/2).
        int stripOffsetIndex = mImageHeight - ((Double) Math.ceil(latDiffScaled)).intValue();


        // Determine where in the chosen strip is the elevation positioned (image width ..
        // longitude)
        double lngDiffScaled = (position.longitude - mMinPos.longitude) / mScaleLng;
        // The same applies as for latitude except we need to use floor instead of ceil, as
        // longitude position in image is not reversed
        int offsetInStrip =
                ((Double) Math.floor(lngDiffScaled)).intValue()
                        * SkyControlConst.BYTES_IN_TIFF_SHORT;
        int stripOffset = mStripOffsets[stripOffsetIndex];
        try {
            FileUtil.readBytesFromFile(mTiffFile, mBuffer, SkyControlConst.BYTES_IN_TIFF_SHORT,
                    stripOffset + offsetInStrip);
        } catch (IOException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not get elevation\n");
            e.printStackTrace();
            return -1;
        }
        int elevation = mBuffer.getShort() & 0xFFFF;
        return elevation;
    }

    /**
     * Checks if is the position lies within the elevation model specified by GeoTIFF.
     * 
     * @param position the position
     * @return true, if is position within
     */
    public boolean isPositionWithin(LatLng position) {
        if (mMinPos.latitude < position.latitude && position.latitude < mMaxPos.latitude
                && mMinPos.longitude < position.longitude && position.longitude < mMaxPos.longitude)
            return true;
        else
            return false;
    }

    /**
     * Gets corners of the digital elevation model area.
     *
     * @return dem area corner positions 
     */
    public List<LatLng> getBoundaryPoints() {
        List<LatLng> boundary = new ArrayList<LatLng>();
        boundary.add(new LatLng(mMaxPos.latitude, mMinPos.longitude));
        boundary.add(mMaxPos);
        boundary.add(new LatLng(mMinPos.latitude, mMaxPos.longitude));
        boundary.add(mMinPos);
        boundary.add(new LatLng(mMaxPos.latitude, mMinPos.longitude));
        return boundary;
    }

    /**
     * Load values of type double in count and from offset specified in the TIFF field.
     * 
     * @param field the TIFF field
     * @return the array with read doubles
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private ArrayList<Double> loadDoubles(TiffField field) throws IOException {
        if (field.getType() != TiffField.TIFF_TYPE_DOUBLE) {
            Log.e(SkyControlConst.ERROR_TAG, "Field type is not DOUBLE\n");
            return null;
        }
        // Save current file pointer
        long currentPosition = mTiffFile.getFilePointer();
        int bytesToRead = field.getCount() * SkyControlConst.BYTES_IN_TIFF_DOUBLE;
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        buffer.order(mEndianness);
        FileUtil.readBytesFromFile(mTiffFile, buffer, bytesToRead, field.getValueOrOffset());
        ArrayList<Double> doubles = new ArrayList<Double>();
        for (int i = 0; i < field.getCount(); i++) {
            double value = buffer.getDouble();
            doubles.add(value);
            // Log.d(SkyControlConst.DEBUG_TAG, "Double " + i + ": " + value + "\n");
        }
        // Load saved file pointer
        mTiffFile.seek(currentPosition);
        return doubles;
    }

    /**
     * Load values of type integer in count and from offset specified in the TIFF field.
     * 
     * @param field the TIFF field
     * @return the array of read integers
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private int[] loadInts(TiffField field) throws IOException {
        if (field.getType() != TiffField.TIFF_TYPE_LONG) {
            Log.e(SkyControlConst.ERROR_TAG, "Field type is not LONG\n");
            return null;
        }
        // Save current file pointer
        long currentPosition = mTiffFile.getFilePointer();
        int numberOfInts = field.getCount();
        int bytesToRead = numberOfInts * SkyControlConst.BYTES_IN_TIFF_LONG;
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        buffer.order(mEndianness);
        FileUtil.readBytesFromFile(mTiffFile, buffer, bytesToRead, field.getValueOrOffset());
        int[] ints = new int[numberOfInts];
        for (int i = 0; i < field.getCount(); i++) {
            int value = buffer.getInt();
            // if (i<10)
            // Log.d(SkyControlConst.DEBUG_TAG, "Double " + i + ": " + value + "\n");
            ints[i] = value;
        }
        // Load saved file pointer
        mTiffFile.seek(currentPosition);
        return ints;
    }

    /**
     * Process fields special for GeoTIFF called GeoKeys specifying how to interpret the data in
     * TIFF. Currently just values used in ASTER GDEM GeoTIFF are supported.
     * 
     * @param headerPosition the header position
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void loadGeoKeys(int headerPosition) throws IOException {
        // First 4 shorts contain header, but first 3 shorts aren't interesting (GeoTiff version and
        // revision)
        FileUtil.readBytesFromFile(mTiffFile, mBuffer, 2, headerPosition + 6);
        int numberOfKeys = mBuffer.getShort() & 0xFFFF;
        // Log.d(SkyControlConst.DEBUG_TAG, "Number of keys: " + numberOfKeys + "\n");
        // Header is immediately followed by a collection of <numberOfKeys> KeyEntry sets, each of
        // which is 4-shorts long
        for (; numberOfKeys > 0; --numberOfKeys) {
            // Each KeyEntry is 8-bytes long
            FileUtil.readBytesFromFile(mTiffFile, mBuffer, 8, FileUtil.NO_POSITION_CHANGE);
            // First 2 bytes of the KeyEntry contain KeyID describing the content of the field.
            // KeyIDs the same as TIFF tags are defined as unsigned short.
            // List of KeyIDs: http://www.remotesensing.org/geotiff/spec/geotiff6.html#6.2
            int keyId = mBuffer.getShort() & 0xFFFF;
            GeoTiffKeyEntry key = new GeoTiffKeyEntry(mBuffer);
            switch (keyId) {
                case 1024:
                    // GTModelTypeGeoKey
                    if (key.getValueOrOffset() == 2)
                        Log.d(SkyControlConst.DEBUG_TAG, "Model Type: ModelTypeGeographic");
                    else
                        Log.e(SkyControlConst.ERROR_TAG, "Model Type not supported");
                    break;
                case 1025:
                    // GTRasterTypeGeoKey
                    if (key.getValueOrOffset() == 1)
                        Log.d(SkyControlConst.DEBUG_TAG, "Raster Type: RasterPixelIsArea");
                    else
                        Log.e(SkyControlConst.ERROR_TAG, "Raster Type not supported");
                    break;
                case 2048:
                    // GeographicTypeGeoKey
                    if (key.getValueOrOffset() == 4326)
                        Log.d(SkyControlConst.DEBUG_TAG, "Geographic CS Type: GCS_WGS_84");
                    else
                        Log.e(SkyControlConst.ERROR_TAG, "Geographic CS Type not supported");
                    break;
                case 2052:
                    // GeogLinearUnitsGeoKey
                    if (key.getValueOrOffset() == 9001)
                        Log.d(SkyControlConst.DEBUG_TAG, "Linear Units: Linear_Meter");
                    else
                        Log.e(SkyControlConst.ERROR_TAG, "Linear Units not supported");
                    break;
                case 2054:
                    // GeogAngularUnitsGeoKey
                    if (key.getValueOrOffset() == 9102)
                        Log.d(SkyControlConst.DEBUG_TAG, "Angular Units: Angular_Degree");
                    else
                        Log.e(SkyControlConst.ERROR_TAG, "Angular Units not supported");
                    break;
                default:
                    Log.e(SkyControlConst.ERROR_TAG, "Unsupported GeoTIFF KeyID: " + keyId + "\n");
                    break;
            }
        }
    }
}
