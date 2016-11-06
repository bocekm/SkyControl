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
package com.bocekm.skycontrol.file;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bocekm.skycontrol.PreferencesFragment;
import com.bocekm.skycontrol.SkyControlApp;
import com.bocekm.skycontrol.SkyControlConst;

/**
 * Provides utilities related to file handling.
 */
public class FileUtil {

    public static final long NO_POSITION_CHANGE = -1;

    /**
     * Copies file from assets to internal storage if it isn't present in internal storage already.
     * 
     * @param filename filename of the asset, not path
     * @return full path of the copied file
     */
    public static String copyAssetToInternal(String filename) {
        // TODO: Make this method asynchronous to not block main UI thread for large files
        Context context = SkyControlApp.getAppContext();
        String destinationFilePath =
                FileUtil.getUserSpecifiedDirectory() + File.separator + filename;

        if (!new File(destinationFilePath).exists()) {
            try {
                copyCompressedAsset(context, filename, destinationFilePath);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.d(SkyControlConst.DEBUG_TAG, "File has been already copied: " + filename + "\n");
        }
        return destinationFilePath;
    }

    /**
     * Copies compressed asset file to internal storage.
     *
     * @param context the context
     * @param sourceFileName the source file name
     * @param destinationFilePath the destination file path
     * @throws Exception the exception
     */
    private static void copyCompressedAsset(Context context, String sourceFileName,
            String destinationFilePath) throws Exception {

        InputStream inStream = null;
        try {
            inStream = context.getAssets().open(sourceFileName);
        } catch (IOException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not open file: " + sourceFileName + "\n");
            throw e;
        }

        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(destinationFilePath);
        } catch (FileNotFoundException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not be written to: " + destinationFilePath
                    + "\n");
            inStream.close();
            throw e;
        }

        byte[] buffer = new byte[1024];
        int byteCount;
        while ((byteCount = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, byteCount);
        }
        outStream.close();
        inStream.close();
    }

    /**
     * Faster method of copying file than used in
     * {@link FileUtil#copyCompressedAsset(Context, String, String)}, but cannot be used for assets,
     * because most of the files in assets gets compressed and then this method fails. Thus it is
     * intended for storage to storage file copying. Or uncompressed asset to storage copying. To
     * see which asset file formats don't get compressed, go to:
     * http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/
     * 
     * @param context the context
     * @param sourceFileDescriptor the source file descriptor
     * @param destinationFilePath the destination file path
     * @throws Exception the exception
     */
    public static void copyUncompressedFile(Context context, FileDescriptor sourceFileDescriptor,
            String destinationFilePath) throws Exception {
        FileInputStream inStream = new FileInputStream(sourceFileDescriptor);
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(destinationFilePath);
        } catch (FileNotFoundException e) {
            Log.e(SkyControlConst.ERROR_TAG, "Could not be written to: " + destinationFilePath
                    + "\n");
            inStream.close();
            throw e;
        }
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (Exception e) {
            Log.e(SkyControlConst.ERROR_TAG, "Channel error when copying to " + destinationFilePath
                    + "\n");
            throw e;
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
        inStream.close();
        outStream.close();
    }

    /**
     * Reads number of bytes from a file.
     * 
     * @param file reference to the file
     * @param buffer the byte buffer
     * @param n number of bytes to read
     * @param pos position of the file pointer from where to read the bytes; optional - pass
     *        {@link FileUtil#NO_POSITION_CHANGE} to do not change current file pointer
     * @throws IOException signals that an I/O exception has occurred
     */
    public static void readBytesFromFile(RandomAccessFile file, ByteBuffer buffer, int n, long pos)
            throws IOException {
        buffer.position(0);
        buffer.limit(n);

        if (pos > NO_POSITION_CHANGE)
            file.seek(pos);

        FileChannel channel = file.getChannel();
        channel.read(buffer);
        buffer.flip();
    }

    /**
     * Returns directory specified by user in preferences for saving user accessible files.
     * 
     * @return the user specified directory
     */
    static public File getUserSpecifiedDirectory() {
        // Get user shared preferences
        SharedPreferences userPref =
                PreferenceManager.getDefaultSharedPreferences(SkyControlApp.getAppContext());
        return new File(userPref.getString(PreferencesFragment.FILE_DIRECTORY_PREF_KEY, ""));
    }

    /**
     * Returns timestamp in yyMMdd-HHmmss format.
     * 
     * @return the time stamp
     */
    static public String getTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss", Locale.US);
        String timeStamp = sdf.format(new Date());
        return timeStamp;
    }
}
