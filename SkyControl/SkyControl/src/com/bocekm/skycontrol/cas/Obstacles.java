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

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persist;

/**
 * Class representing the root element of the obstacles XML. It is used by {@link Persist}. See <a
 * href="http://simple.sourceforge.net">http://simple.sourceforge.net</a> for usage of the special
 * tags for dealing with XML elements.
 */
@Root(name = "obstacles", strict = false)
public class Obstacles {

    @ElementList(name = "obstacle", inline = true)
    private List<Obstacle> mObstacleList;

    public List<Obstacle> getObstacleList() {
        return mObstacleList;
    }
}
