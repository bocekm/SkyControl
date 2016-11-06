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
package com.bocekm.skycontrol.rrt;

import com.vividsolutions.jts.geom.Point;

/**
 * Class representing K-D tree node.
 */
public class RrtNode {
    protected RrtNode mParent;
    protected Point pt;
    
    public RrtNode(Point pt, RrtNode parent) {
        this.pt = pt;
        this.mParent = parent;
    }
    
    public RrtNode getParent() {
        return mParent;
    }
    
    public void setParent(RrtNode parent) {
        this.mParent = parent;
    }

    public Point getPoint() {
        return pt;
    }

    public boolean isRoot() {
        return mParent == null;
    }
}
