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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.bocekm.skycontrol.SkyControlConst;
import com.bocekm.skycontrol.SkyControlUtils;
import com.bocekm.skycontrol.file.FileUtil;

/**
 * Parses XML containing definition of the {@link Obstacles}.
 */
public class XmlParser {

    /**
     * Parses the obstacles from XML.
     * 
     * @return the obstacles
     */
    public static Obstacles parseObstaclesFromXml() {
        // Persister is an implementation of a serializer which is capable of parsing the XML
        Serializer serializer = new Persister();
        InputStream xml;
        Obstacles obstacles = null;
        try {
            String copiedFilePath = FileUtil.copyAssetToInternal(SkyControlConst.OBSTACLES_XML);
            xml = new BufferedInputStream(new FileInputStream(copiedFilePath));
            // To parse the XML pass the specially formatted object representing the root element in
            // XML to the Persister serializer
            obstacles = serializer.read(Obstacles.class, xml);
        } catch (IOException e) {
            SkyControlUtils.log("Could not open " + SkyControlConst.OBSTACLES_XML + "\n", true);
            e.printStackTrace();
        } catch (Exception e) {
            SkyControlUtils.log("Error when parsing " + SkyControlConst.OBSTACLES_XML + "\n", true);
            e.printStackTrace();
        }
        // Create polygons from the parsed coordinates
        Iterator<Obstacle> it = obstacles.getObstacleList().iterator();
        while (it.hasNext()) {
            Obstacle obstacle = (Obstacle) it.next();
            if (!obstacle.createPolygonFromCoords())
                // Remove the obstacle from the list if creating the polygon was not successful
                it.remove();
        }

        return obstacles;
    }
}
