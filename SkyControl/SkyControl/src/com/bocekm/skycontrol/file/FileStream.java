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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * {@link FileStream} class creates new files in the directory specified by user in preferences.
 */
public class FileStream {

    /**
     * Creates new log file stream.
     * 
     * @return the log file stream
     * @throws FileNotFoundException the file not found exception
     */
    public static BufferedOutputStream getLogFileStream() throws FileNotFoundException {
        // Get directory for saving the log files
        File logDir = new File(FileUtil.getUserSpecifiedDirectory(), "Logs");
        // Create the log directory if it doesn't exist, including missing parent directories
        logDir.mkdirs();
        // Specify new log file name having timestamp in it
        File logFile = new File(logDir, FileUtil.getTimeStamp() + ".log");
        // Create new log file, deleting the unlikely existing one
        // BufferOutputStream is used for better performance when saving lots of data
        BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(logFile));
        return buf;
    }
}
