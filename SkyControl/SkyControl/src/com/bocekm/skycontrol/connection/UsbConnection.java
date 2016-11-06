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
package com.bocekm.skycontrol.connection;

import java.io.IOException;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.mavlink.MavLinkConnection;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

/**
 * {@link UsbConnection} handles the hardware aspect of communication between app and connected USB
 * device.
 * 
 * "usb-serial-for-android" library is used in this class. See
 * https://github.com/mik3y/usb-serial-for-android
 */
public class UsbConnection extends MavLinkConnection {

    /** Communication speed in Baud rate. */
    private static final int BAUD_RATE = 57600;

    /** Timeout for writing data using USB driver in milliseconds. */
    private static final int USB_WRITE_TIMEOUT = 500;

    /** Timeout for reading data using USB driver in milliseconds. */
    private static final int USB_READ_TIMEOUT = 200;

    /** USB Driver. */
    private UsbSerialDriver mUsbDriver = null;

    /** {@link Context} of the parent. */
    private Context mParent;

    /**
     * Instantiates new {@link UsbConnection} class object.
     * 
     * @param parentContext the parent context, implementing the MavLinkConnectionListener
     */
    public UsbConnection(Context parentContext) {
        super(parentContext);
        mParent = parentContext;
    }

    /*
     * (non-Javadoc) Connect to the attached USB device.
     * 
     * @see com.bocekm.skycontrol.mavlink.MavLinkConnection#openConnection()
     */
    @Override
    protected void openConnection() throws IOException {
        // Get UsbManager from Android.
        UsbManager manager = (UsbManager) mParent.getSystemService(Context.USB_SERVICE);

        // Find the first available driver. It's unlikely to have more than one USB device
        // connected.
        mUsbDriver = UsbSerialProber.findFirstDevice(manager);

        if (mUsbDriver == null) {
            Log.d(SkyControlConst.DEBUG_TAG, "No USB Devices found");
            throw new IOException("No Devices found");
        } else {
            Log.d(SkyControlConst.DEBUG_TAG, "Connecting to USB with Baud rate " + BAUD_RATE);
            try {
                // Try to connect to the USB device.
                mUsbDriver.open();
                // Set connection speed, parity and other parameters
                mUsbDriver.setParameters(BAUD_RATE, UsbSerialDriver.DATABITS_8,
                        UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(SkyControlConst.ERROR_TAG, "Error setting up device: " + e.getMessage(), e);
                mUsbDriver.close(); // throws IOException
                mUsbDriver = null;
                return;
            }
        }
    }

    /*
     * (non-Javadoc) Read data from driver. This call will return up to mReadData.length bytes. If
     * no data is received it will timeout after USB_READ_TIMEOUT ms.
     * 
     * @see com.bocekm.skycontrol.mavlink.MavLinkConnection#readDataBlock()
     */
    @Override
    protected int readDataBlock() throws IOException {
        return mUsbDriver.read(mReadData, USB_READ_TIMEOUT);
    }

    /*
     * (non-Javadoc) Send data to the attached USB device.
     * 
     * @see com.bocekm.skycontrol.mavlink.MavLinkConnection#sendBuffer(byte[])
     */
    @Override
    protected void sendBuffer(byte[] buffer) {
        // Write data to driver. This call writes buffer.length bytes
        // if data can't be sent, then it will timeout in USB_WRITE_TIMEOUT ms.
        if (mUsbDriver != null) {
            try {
                mUsbDriver.write(buffer, USB_WRITE_TIMEOUT);
            } catch (IOException e) {
                Log.e(SkyControlConst.ERROR_TAG, "USB write error: " + e.getMessage(), e);
            }
        }
    }

    /*
     * (non-Javadoc) Close connection to the USB device.
     * 
     * @see com.bocekm.skycontrol.mavlink.MavLinkConnection#closeConnection()
     */
    @Override
    protected void closeConnection() throws IOException {
        if (mUsbDriver != null) {
            mUsbDriver.close();
            mUsbDriver = null;
        }
    }
}
